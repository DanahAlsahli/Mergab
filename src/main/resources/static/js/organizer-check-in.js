
/* ==========================================
   MERGAB | ORGANIZER QR CHECK-IN
   Real Camera Scanner + Spring Boot API
   ========================================== */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const session = MergabAuth.requireRole(["ORGANIZER"]);
    if (!session) return;

    const $ = id => document.getElementById(id);

    const API = "/api/v1";
    const video = $("qrVideo");
    const viewport = $("scannerViewport");
    const zoneSelect = $("checkInZone");
    const tokenInput = $("qrTokenInput");
    const startButton = $("startCameraButton");
    const stopButton = $("stopCameraButton");
    const submitButton = $("submitVisitButton");

    let mediaStream = null;
    let scanning = false;
    let starting = false;
    let submitting = false;
    let frameId = null;
    let detector = null;
    let lastScanTime = 0;

    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d", {
        willReadFrequently: true
    });

    // =====================================
    // ACCOUNT
    // =====================================

    $("organizerAccountLink").textContent =
        session.fullName || "حسابي";

    $("organizerLogoutButton").addEventListener(
        "click",
        () => MergabAuth.logout()
    );

    // =====================================
    // UI HELPERS
    // =====================================

    function setCameraStatus(message) {
        $("cameraStatus").textContent = message;
    }

    function showResult(message, type = "error") {
        const result = $("checkInResult");

        result.textContent = message;
        result.className = `checkin-result ${type}`;
        result.hidden = false;
    }

    function clearResult() {
        const result = $("checkInResult");
        result.hidden = true;
        result.textContent = "";
    }

    function friendlyError(message) {
        const error = String(message || "");

        if (/Invalid visitor QR code/i.test(error)) {
            return "رمز QR غير صالح أو غير مسجل في النظام.";
        }

        if (/must check out from the current zone first/i.test(error)) {
            return "الزائر داخل منطقة أخرى بالفعل. يجب تسجيل خروجه منها أولًا.";
        }

        if (/Zone is at full capacity/i.test(error)) {
            return "المنطقة وصلت إلى سعتها القصوى، ولا يمكن تسجيل دخول زائر جديد.";
        }

        if (/Visitor has no active zone visit/i.test(error)) {
            return "الزائر ليس لديه زيارة نشطة لتسجيل الخروج.";
        }

        if (/Visitor is not inside this zone/i.test(error)) {
            return "الزائر ليس مسجلًا داخل المنطقة المختارة.";
        }

        if (/Zone not found/i.test(error)) {
            return "المنطقة المختارة غير موجودة.";
        }

        if (/Failed to fetch|NetworkError/i.test(error)) {
            return "تعذر الاتصال بالخادم. تأكدي من تشغيل Spring Boot.";
        }

        return error || "تعذرت العملية. حاولي مرة أخرى.";
    }

    // =====================================
    // API
    // =====================================

    async function request(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            cache: "no-store",
            headers: {
                Authorization:
                    MergabAuth.getAuthorizationHeader(),

                Accept: "application/json",

                "X-Requested-With": "XMLHttpRequest",

                ...(options.body
                    ? { "Content-Type": "application/json" }
                    : {}),

                ...options.headers
            }
        });

        if (response.status === 401) {
            MergabAuth.logout();
            throw new Error("انتهت صلاحية تسجيل الدخول.");
        }

        if (response.status === 403) {
            throw new Error(
                "لا تملكين صلاحية تسجيل دخول وخروج الزوار."
            );
        }

        const text = await response.text();

        let data = null;

        if (text) {
            try {
                data = JSON.parse(text);
            } catch (_) {
                data = null;
            }
        }

        if (!response.ok) {
            throw new Error(
                data?.message ||
                `فشلت العملية (HTTP ${response.status}).`
            );
        }

        return data;
    }

    // =====================================
    // LOAD REAL ZONES
    // =====================================

    async function loadZones() {
        zoneSelect.replaceChildren();

        const loadingOption = document.createElement("option");
        loadingOption.value = "";
        loadingOption.textContent = "جارٍ تحميل المناطق...";
        zoneSelect.appendChild(loadingOption);

        zoneSelect.disabled = true;

        try {
            const zones = await request(`${API}/zone/get`);

            if (!Array.isArray(zones)) {
                throw new Error("استجابة المناطق غير صحيحة.");
            }

            zoneSelect.replaceChildren();

            const placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = "اختاري المنطقة";
            zoneSelect.appendChild(placeholder);

            zones.forEach(zone => {
                const option = document.createElement("option");

                option.value = String(zone.id);
                option.textContent = zone.name;

                zoneSelect.appendChild(option);
            });

            if (!zones.length) {
                placeholder.textContent =
                    "لا توجد مناطق مسجلة بعد";

                showResult(
                    "أضيفي منطقة من صفحة إدارة المناطق قبل تسجيل زيارات الزوار."
                );
            } else {
                zoneSelect.disabled = false;
            }

        } catch (error) {
            zoneSelect.replaceChildren();

            const option = document.createElement("option");
            option.value = "";
            option.textContent = "تعذر تحميل المناطق";

            zoneSelect.appendChild(option);

            showResult(friendlyError(error.message));
        }
    }

    // =====================================
    // STOP CAMERA
    // =====================================

    function stopCamera() {
        scanning = false;

        if (frameId !== null) {
            cancelAnimationFrame(frameId);
            frameId = null;
        }

        if (mediaStream) {
            mediaStream.getTracks().forEach(track => {
                track.stop();
            });

            mediaStream = null;
        }

        video.pause();
        video.srcObject = null;

        viewport.classList.remove("camera-active");

        startButton.disabled = starting;
        stopButton.disabled = true;

        setCameraStatus("الكاميرا متوقفة");
    }

    // =====================================
    // WHEN QR IS DETECTED
    // =====================================

    function handleScannedToken(rawValue) {
        const token = String(rawValue || "").trim();

        if (!token || !scanning) return;

        // Stop immediately to avoid scanning the same QR again
        stopCamera();

        // Only fill the input.
        // Never automatically register a visit.
        tokenInput.value = token;

        $("qrInputHint").textContent =
            "تمت قراءة الرمز. اختاري المنطقة ونوع العملية ثم اضغطي تأكيد.";

        setCameraStatus("تمت قراءة QR بنجاح");

        clearResult();

        showResult(
            "تمت قراءة رمز الزائر بنجاح. لم تُسجل الزيارة بعد؛ اضغطي «تأكيد العملية» لإتمامها.",
            "success"
        );
    }

    // =====================================
    // DETECT QR IN VIDEO FRAMES
    // =====================================

    async function scanFrame(timestamp) {
        if (!scanning) return;

        // Limit scanning frequency to reduce CPU usage
        if (timestamp - lastScanTime < 180) {
            frameId = requestAnimationFrame(scanFrame);
            return;
        }

        lastScanTime = timestamp;

        if (
            video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA &&
            video.videoWidth > 0 &&
            video.videoHeight > 0
        ) {
            let detectedToken = "";

            // Native browser scanner, if supported
            if (detector) {
                try {
                    const results = await detector.detect(video);

                    if (results.length > 0) {
                        detectedToken = results[0].rawValue || "";
                    }
                } catch (_) {
                    // Fall back to jsQR if native detection fails
                    detector = null;
                }
            }

            // jsQR fallback
            if (
                !detectedToken &&
                typeof window.jsQR === "function" &&
                context
            ) {
                try {
                    const maxWidth = 640;

                    const scale = Math.min(
                        1,
                        maxWidth / video.videoWidth
                    );

                    const width = Math.max(
                        1,
                        Math.round(video.videoWidth * scale)
                    );

                    const height = Math.max(
                        1,
                        Math.round(video.videoHeight * scale)
                    );

                    canvas.width = width;
                    canvas.height = height;

                    context.drawImage(
                        video,
                        0,
                        0,
                        width,
                        height
                    );

                    const image = context.getImageData(
                        0,
                        0,
                        width,
                        height
                    );

                    const result = window.jsQR(
                        image.data,
                        width,
                        height,
                        {
                            inversionAttempts: "attemptBoth"
                        }
                    );

                    if (result?.data) {
                        detectedToken = result.data;
                    }

                } catch (error) {
                    console.error(
                        "QR frame decoding failed:",
                        error
                    );
                }
            }

            if (!scanning) return;

            if (detectedToken) {
                handleScannedToken(detectedToken);
                return;
            }
        }

        if (scanning) {
            frameId = requestAnimationFrame(scanFrame);
        }
    }

    // =====================================
    // START CAMERA
    // =====================================

    async function startCamera() {
        if (starting || scanning) return;

        clearResult();

        if (
            !navigator.mediaDevices ||
            !navigator.mediaDevices.getUserMedia
        ) {
            showResult(
                "الكاميرا غير متاحة. افتحي الصفحة عبر localhost أو HTTPS، أو استخدمي الإدخال اليدوي."
            );
            return;
        }

        detector = null;

        if ("BarcodeDetector" in window) {
            try {
                detector = new BarcodeDetector({
                    formats: ["qr_code"]
                });
            } catch (_) {
                detector = null;
            }
        }

        if (
            !detector &&
            typeof window.jsQR !== "function"
        ) {
            showResult(
                "مكتبة قراءة QR غير متاحة. تأكدي من تحميل jsQR أو استخدمي الإدخال اليدوي."
            );
            return;
        }

        starting = true;

        startButton.disabled = true;

        setCameraStatus("جارٍ تشغيل الكاميرا...");

        try {
            mediaStream =
                await navigator.mediaDevices.getUserMedia({
                    audio: false,
                    video: {
                        facingMode: {
                            ideal: "environment"
                        }
                    }
                });

            video.srcObject = mediaStream;

            await video.play();

            scanning = true;
            lastScanTime = 0;

            viewport.classList.add("camera-active");

            setCameraStatus("جارٍ البحث عن QR...");

            stopButton.disabled = false;

            frameId = requestAnimationFrame(scanFrame);

        } catch (error) {
            stopCamera();

            let message =
                "تعذر تشغيل الكاميرا. تأكدي من توصيلها والسماح للمتصفح باستخدامها.";

            if (
                error.name === "NotAllowedError" ||
                error.name === "PermissionDeniedError"
            ) {
                message =
                    "المتصفح رفض صلاحية الكاميرا. اسمحي للموقع باستخدام الكاميرا من إعدادات المتصفح.";
            }

            if (
                error.name === "NotFoundError"
            ) {
                message =
                    "لم يتم العثور على كاميرا متصلة بالجهاز.";
            }

            showResult(message);

        } finally {
            starting = false;
            startButton.disabled = scanning;
        }
    }

    startButton.addEventListener(
        "click",
        startCamera
    );

    stopButton.addEventListener(
        "click",
        stopCamera
    );

    // =====================================
    // MANUAL INPUT
    // =====================================

    tokenInput.addEventListener("input", () => {
        clearResult();

        $("qrInputHint").textContent =
            "سيتم التحقق من صحة الرمز عند تأكيد العملية.";
    });

    // =====================================
    // CHECK-IN / CHECK-OUT
    // =====================================

    $("checkInForm").addEventListener(
        "submit",
        async event => {
            event.preventDefault();

            if (submitting) return;

            clearResult();

            const zoneId = zoneSelect.value;

            const qrToken =
                tokenInput.value.trim();

            const action =
                document.querySelector(
                    'input[name="visitAction"]:checked'
                )?.value;

            if (!zoneId) {
                showResult("اختاري المنطقة أولًا.");
                return;
            }

            if (!qrToken) {
                showResult(
                    "امسحي QR الزائر أو أدخلي الرمز يدويًا."
                );
                return;
            }

            if (
                action !== "check-in" &&
                action !== "check-out"
            ) {
                showResult("اختاري نوع العملية.");
                return;
            }

            if (scanning) {
                stopCamera();
            }

            const isCheckIn = action === "check-in";

            const endpoint = isCheckIn
                ? `${API}/zone-visit/check-in/${zoneId}`
                : `${API}/zone-visit/check-out/${zoneId}`;

            const method = isCheckIn
                ? "POST"
                : "PUT";

            submitting = true;
            submitButton.disabled = true;

            submitButton.textContent =
                "جارٍ تنفيذ العملية...";

            try {
                await request(endpoint, {
                    method,
                    body: JSON.stringify({
                        qrToken
                    })
                });

                // Success only after the backend confirms it
                showResult(
                    isCheckIn
                        ? "تم تسجيل دخول الزائر إلى المنطقة بنجاح."
                        : "تم تسجيل خروج الزائر من المنطقة بنجاح.",
                    "success"
                );

                tokenInput.value = "";

                $("qrInputHint").textContent =
                    "جاهز لمسح رمز الزائر التالي.";

            } catch (error) {
                showResult(
                    friendlyError(error.message)
                );

            } finally {
                submitting = false;
                submitButton.disabled = false;

                submitButton.replaceChildren();

                const icon =
                    document.createElement("span");

                icon.className =
                    "material-symbols-rounded";

                icon.textContent = "check_circle";

                submitButton.append(
                    icon,
                    " تأكيد العملية"
                );
            }
        }
    );

    // =====================================
    // CAMERA CLEANUP
    // =====================================

    document.addEventListener(
        "visibilitychange",
        () => {
            if (document.hidden && scanning) {
                stopCamera();
            }
        }
    );

    window.addEventListener(
        "pagehide",
        stopCamera
    );

    // =====================================
    // INITIALIZE
    // =====================================

    loadZones();
});
