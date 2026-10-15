
/* ==========================================
   MERGAB | ORGANIZER ZONES
   Polygon Boundaries + Interactive Map
   ========================================== */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const session = MergabAuth.requireRole(["ORGANIZER"]);
    if (!session) return;

    const $ = id => document.getElementById(id);

    const API = "/api/v1/zone";
    const MAP_IMAGE = "/img/map/expo-map-concept.jpg";
    const SVG_NS = "http://www.w3.org/2000/svg";

    const mapElement = $("zonePlacementMap");
    const form = $("zoneForm");
    const editor = $("zoneEditor");
    const tableBody = $("zonesTableBody");
    const drawingTools = $("zoneDrawingTools");

    let zones = [];
    let map = null;

    let editingId = null;
    let drawing = false;
    let boundaryPoints = [];
    let boundaryFinished = false;

    let pointerStart = null;
    let pointerMoved = false;

    // =====================================
    // ACCOUNT
    // =====================================

    $("organizerAccountLink").textContent =
        session.fullName || "حسابي";

    $("organizerLogoutButton").addEventListener(
        "click",
        () => MergabAuth.logout()
    );

    function setMapStatus(text) {
        $("zonesMapStatus").textContent = text;
    }

    function showMessage(text, type = "error") {
        const box = $("zoneFormMessage");

        box.textContent = text;
        box.className = `zones-form-message ${type}`;
        box.hidden = false;
    }

    function clearMessage() {
        const box = $("zoneFormMessage");
        box.hidden = true;
        box.textContent = "";
    }

    // =====================================
    // BACKEND API
    // =====================================

    async function api(path, options = {}) {
        const response = await fetch(API + path, {
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
                "لا تملكين صلاحية تنفيذ هذه العملية."
            );
        }

        if (!response.ok) {
            let errorMessage =
                `تعذرت العملية (${response.status}).`;

            try {
                const data = await response.json();

                errorMessage =
                    data.message || errorMessage;
            } catch (_) {}

            throw new Error(errorMessage);
        }

        const text = await response.text();

        if (!text) return null;

        try {
            return JSON.parse(text);
        } catch (_) {
            return null;
        }
    }

    // =====================================
    // BOUNDARY HELPERS
    // =====================================

    function parseBoundary(value) {
        try {
            const parsed =
                typeof value === "string"
                    ? JSON.parse(value)
                    : value;

            if (
                !Array.isArray(parsed) ||
                parsed.length < 3
            ) {
                return [];
            }

            const points = parsed.map(point => {
                if (
                    !Array.isArray(point) ||
                    point.length !== 2
                ) {
                    return null;
                }

                return point.map(Number);
            });

            const valid = points.every(point =>
                point !== null &&
                point.every(number =>
                    Number.isFinite(number) &&
                    number >= 0 &&
                    number <= 100
                )
            );

            return valid ? points : [];

        } catch (_) {
            return [];
        }
    }

    function hasPosition(zone) {
        return (
            zone.mapX != null &&
            zone.mapY != null &&
            Number.isFinite(Number(zone.mapX)) &&
            Number.isFinite(Number(zone.mapY))
        );
    }

    // Calculate polygon area
    function polygonArea(points) {
        if (points.length < 3) return 0;

        let sum = 0;

        points.forEach((point, index) => {
            const next =
                points[(index + 1) % points.length];

            sum +=
                point[0] * next[1] -
                next[0] * point[1];
        });

        return Math.abs(sum / 2);
    }

    // Calculate polygon center for the zone label
    function polygonCenter(points) {
        let twiceArea = 0;
        let centerX = 0;
        let centerY = 0;

        points.forEach((point, index) => {
            const next =
                points[(index + 1) % points.length];

            const cross =
                point[0] * next[1] -
                next[0] * point[1];

            twiceArea += cross;

            centerX +=
                (point[0] + next[0]) * cross;

            centerY +=
                (point[1] + next[1]) * cross;
        });

        if (Math.abs(twiceArea) < 0.000001) {
            return points[0];
        }

        return [
            centerX / (3 * twiceArea),
            centerY / (3 * twiceArea)
        ];
    }

    function createSVGElement(tag, attributes, parent) {
        const element =
            document.createElementNS(SVG_NS, tag);

        Object.entries(attributes).forEach(
            ([key, value]) => {
                element.setAttribute(
                    key,
                    String(value)
                );
            }
        );

        parent.appendChild(element);

        return element;
    }

    // =====================================
    // DRAW BOUNDARIES ON MAP
    // =====================================

    function drawBoundaries() {
        const svg =
            mapElement.querySelector(".mqm-isvg");

        if (!svg) return;

        svg.querySelector("#zoneBoundaryLayer")
            ?.remove();

        const layer =
            document.createElementNS(SVG_NS, "g");

        layer.id = "zoneBoundaryLayer";

        layer.setAttribute(
            "pointer-events",
            "none"
        );

        svg.insertBefore(layer, svg.firstChild);

        function drawPolygon(
            points,
            color,
            opacity
        ) {
            createSVGElement(
                "polygon",
                {
                    points: points
                        .map(point => point.join(","))
                        .join(" "),

                    fill: color,
                    "fill-opacity": opacity,

                    stroke: color,
                    "stroke-width": 1.2,
                    "stroke-linejoin": "round",

                    "vector-effect":
                        "non-scaling-stroke",

                    "pointer-events": "none"
                },
                layer
            );
        }

        // Existing saved boundaries
        zones.forEach(zone => {
            if (
                drawing &&
                String(zone.id) === String(editingId)
            ) {
                return;
            }

            const points =
                parseBoundary(zone.boundaryPoints);

            if (points.length >= 3) {
                drawPolygon(
                    points,
                    "#247C59",
                    0.40
                );
            }
        });

        // Currently edited boundary
        if (drawing && boundaryPoints.length > 0) {

            if (boundaryPoints.length >= 3) {
                drawPolygon(
                    boundaryPoints,
                    "#C7A76B",
                    boundaryFinished ? 0.27 : 0.12
                );
            }

            if (
                !boundaryFinished &&
                boundaryPoints.length >= 2
            ) {
                createSVGElement(
                    "polyline",
                    {
                        points: boundaryPoints
                            .map(point => point.join(","))
                            .join(" "),

                        fill: "none",

                        stroke: "#0E3D32",
                        "stroke-width": 0.6,

                        "vector-effect":
                            "non-scaling-stroke",

                        "pointer-events": "none"
                    },
                    layer
                );
            }

            // Boundary corners
            boundaryPoints.forEach(([x, y]) => {
                createSVGElement(
                    "circle",
                    {
                        cx: x,
                        cy: y,
                        r: 0.62,

                        fill: "#0E3D32",
                        stroke: "#FFFFFF",
                        "stroke-width": 0.25,

                        "pointer-events": "none"
                    },
                    layer
                );
            });
        }

        if (drawing) {
            $("boundaryDrawingStatus").textContent =
                boundaryFinished
                    ? `تم تحديد الحدود (${boundaryPoints.length} زوايا). يمكنك حفظ المنطقة.`
                    : `عدد الزوايا: ${boundaryPoints.length} — اختاري ثلاث زوايا على الأقل.`;
        }
    }

    // =====================================
    // INTERACTIVE MAP
    // =====================================

    function renderMap() {
        if (map) {
            map.destroy();
            map = null;
        }

        const mapZones = zones
            .filter(hasPosition)
            .map(zone => ({
                key: `zone-${zone.id}`,

                id: zone.id,

                name: {
                    ar: zone.name,
                    en: zone.name
                },

                x: Number(zone.mapX),
                y: Number(zone.mapY)
            }));

        map = MirqabMap.create(
            "#zonePlacementMap",
            {
                mode: "image",
                lang: "ar",

                demo: false,
                controls: true,
                heat: false,

                start: null,
                matchBy: "id",

                imageFit: "contain",

                imageLayout: {
                    src: MAP_IMAGE,
                    zones: mapZones,
                    edges: [],
                    hub: null,
                    metersPerUnit: 8
                },

                onSelect(info) {
                    if (drawing) return;

                    const zone = zones.find(item =>
                        String(item.id) ===
                        String(info.id)
                    );

                    if (zone) {
                        openEditor(zone);
                    }
                }
            }
        );

        // Hide visitor location
        mapElement.querySelectorAll(
            ".mqm-ime, .mqm-ime-label"
        ).forEach(element => {
            element.style.display = "none";
        });

        drawBoundaries();

        // Redraw after map image loads
        mapElement.querySelector(".mqm-img")
            ?.addEventListener(
                "load",
                drawBoundaries,
                { once: true }
            );
    }

    // =====================================
    // ADD / EDIT ZONE
    // =====================================

    function openEditor(zone = null) {
        editingId = zone?.id ?? null;

        drawing = true;

        boundaryPoints =
            parseBoundary(zone?.boundaryPoints);

        boundaryFinished =
            boundaryPoints.length >= 3;

        form.reset();

        $("zoneId").value = editingId ?? "";

        $("zoneName").value =
            zone?.name ?? "";

        $("zoneCapacity").value =
            zone?.capacity ?? "";

        $("zoneInitialVisitors").value =
            zone?.initialVisitors ?? 0;

        $("zoneIndoor").checked =
            zone?.indoor === true;

        $("zonePrayerArea").checked =
            zone?.hasPrayerArea === true;

        $("zoneMapX").value =
            zone?.mapX ?? "";

        $("zoneMapY").value =
            zone?.mapY ?? "";

        $("zoneBoundaryPoints").value =
            boundaryFinished
                ? JSON.stringify(boundaryPoints)
                : "";

        $("zoneEditorTitle").textContent =
            zone
                ? "تعديل المنطقة"
                : "إضافة منطقة جديدة";

        $("zoneLocationHint").textContent =
            "ارسمي حدود المنطقة ثم اضغطي إنهاء تحديد الحدود.";

        clearMessage();

        editor.hidden = false;
        drawingTools.hidden = false;

        mapElement.classList.add("is-placing");

        setMapStatus(
            "حددي حدود المنطقة على الخريطة"
        );

        drawBoundaries();

        mapElement.scrollIntoView({
            behavior: "smooth",
            block: "center"
        });
    }

    function closeEditor() {
        drawing = false;
        editingId = null;

        boundaryPoints = [];
        boundaryFinished = false;

        editor.hidden = true;
        drawingTools.hidden = true;

        form.reset();

        mapElement.classList.remove(
            "is-placing"
        );

        setMapStatus("خريطة تفاعلية");

        drawBoundaries();
    }

    $("addZoneButton").addEventListener(
        "click",
        () => openEditor()
    );

    $("cancelZoneButton").addEventListener(
        "click",
        closeEditor
    );

    $("closeZoneEditor").addEventListener(
        "click",
        closeEditor
    );

    // =====================================
    // DISTINGUISH CLICK FROM DRAG
    // =====================================

    mapElement.addEventListener(
        "pointerdown",
        event => {
            pointerStart = {
                x: event.clientX,
                y: event.clientY
            };

            pointerMoved = false;
        },
        true
    );

    mapElement.addEventListener(
        "pointermove",
        event => {
            if (!pointerStart) return;

            const distance = Math.hypot(
                event.clientX - pointerStart.x,
                event.clientY - pointerStart.y
            );

            if (distance > 6) {
                pointerMoved = true;
            }
        },
        true
    );

    // =====================================
    // ADD CORNERS BY CLICKING MAP
    // =====================================

    mapElement.addEventListener(
        "click",
        event => {
            const wasDragging = pointerMoved;

            pointerStart = null;
            pointerMoved = false;

            if (
                !drawing ||
                boundaryFinished ||
                wasDragging
            ) {
                return;
            }

            // Ignore map controls and existing markers
            if (event.target.closest(
                ".mqm-controls, .mqm-heat, " +
                ".mqm-legend, .mqm-marker"
            )) {
                return;
            }

            // Only allow clicking actual map image
            const image =
                event.target.closest(".mqm-img");

            if (!image) return;

            const stage =
                mapElement.querySelector(".mqm-istage");

            if (!stage) return;

            const rect =
                stage.getBoundingClientRect();

            const x =
                (event.clientX - rect.left) /
                rect.width * 100;

            const y =
                (event.clientY - rect.top) /
                rect.height * 100;

            if (
                !Number.isFinite(x) ||
                !Number.isFinite(y) ||
                x < 0 || x > 100 ||
                y < 0 || y > 100
            ) {
                return;
            }

            boundaryPoints.push([
                Number(x.toFixed(2)),
                Number(y.toFixed(2))
            ]);

            drawBoundaries();

            setMapStatus(
                `جارٍ رسم الحدود — ${boundaryPoints.length} زوايا`
            );
        }
    );

    // =====================================
    // FINISH BOUNDARY
    // =====================================

    $("finishBoundary").addEventListener(
        "click",
        () => {
            if (
                boundaryPoints.length < 3 ||
                polygonArea(boundaryPoints) < 0.1
            ) {
                showMessage(
                    "حددي ثلاث زوايا مختلفة على الأقل لتكوين منطقة."
                );
                return;
            }

            boundaryFinished = true;

            $("zoneBoundaryPoints").value =
                JSON.stringify(boundaryPoints);

            // Zone label position
            const [centerX, centerY] =
                polygonCenter(boundaryPoints);

            $("zoneMapX").value =
                Number(centerX.toFixed(2));

            $("zoneMapY").value =
                Number(centerY.toFixed(2));

            $("zoneLocationHint").textContent =
                "تم تحديد حدود المنطقة وموقع اسمها.";

            clearMessage();

            setMapStatus(
                "تم إنهاء تحديد الحدود"
            );

            drawBoundaries();
        }
    );

    // =====================================
    // UNDO LAST POINT
    // =====================================

    $("undoBoundaryPoint").addEventListener(
        "click",
        () => {
            if (!boundaryPoints.length) return;

            boundaryPoints.pop();

            boundaryFinished = false;

            $("zoneBoundaryPoints").value = "";
            $("zoneMapX").value = "";
            $("zoneMapY").value = "";

            drawBoundaries();

            setMapStatus(
                "تم التراجع عن آخر زاوية"
            );
        }
    );

    // =====================================
    // CLEAR BOUNDARY
    // =====================================

    $("clearBoundary").addEventListener(
        "click",
        () => {
            boundaryPoints = [];
            boundaryFinished = false;

            $("zoneBoundaryPoints").value = "";
            $("zoneMapX").value = "";
            $("zoneMapY").value = "";

            clearMessage();
            drawBoundaries();

            setMapStatus(
                "تم مسح الحدود. ابدئي بتحديد الزوايا."
            );
        }
    );

    // =====================================
    // SAVE ZONE
    // =====================================

    form.addEventListener(
        "submit",
        async event => {
            event.preventDefault();

            clearMessage();

            const name =
                $("zoneName").value.trim();

            const capacity =
                Number($("zoneCapacity").value);

            const initialVisitors =
                Number($("zoneInitialVisitors").value);

            if (
                !name ||
                name.length > 50
            ) {
                showMessage(
                    "اسم المنطقة مطلوب وبحد أقصى 50 حرفًا."
                );
                return;
            }

            if (
                !Number.isInteger(capacity) ||
                capacity <= 0
            ) {
                showMessage(
                    "السعة يجب أن تكون عددًا صحيحًا أكبر من صفر."
                );
                return;
            }

            if (
                !Number.isInteger(initialVisitors) ||
                initialVisitors < 0 ||
                initialVisitors > capacity
            ) {
                showMessage(
                    "عدد الزوار الأولي يجب ألا يتجاوز سعة المنطقة."
                );
                return;
            }

            if (
                !boundaryFinished ||
                boundaryPoints.length < 3 ||
                polygonArea(boundaryPoints) < 0.1
            ) {
                showMessage(
                    "ارسمي حدود المنطقة واضغطي إنهاء تحديد الحدود قبل الحفظ."
                );
                return;
            }

            const payload = {
                name,
                capacity,
                initialVisitors,

                indoor:
                $("zoneIndoor").checked,

                hasPrayerArea:
                $("zonePrayerArea").checked,

                mapX:
                    Number($("zoneMapX").value),

                mapY:
                    Number($("zoneMapY").value),

                boundaryPoints:
                    JSON.stringify(boundaryPoints)
            };

            const button =
                $("saveZoneButton");

            button.disabled = true;

            try {
                const path =
                    editingId == null
                        ? "/add"
                        : `/update/${editingId}`;

                const method =
                    editingId == null
                        ? "POST"
                        : "PUT";

                await api(path, {
                    method,
                    body: JSON.stringify(payload)
                });

                closeEditor();

                await loadZones();

                setMapStatus(
                    "تم حفظ المنطقة وحدودها بنجاح"
                );

            } catch (error) {
                showMessage(
                    error.message ||
                    "تعذر حفظ المنطقة."
                );

                console.error(
                    "Save zone failed:",
                    error
                );

            } finally {
                button.disabled = false;
            }
        }
    );

    // =====================================
    // TABLE BUTTON
    // =====================================

    function createTableButton(
        icon,
        title,
        onClick,
        danger = false
    ) {
        const button =
            document.createElement("button");

        button.type = "button";
        button.title = title;
        button.setAttribute(
            "aria-label",
            title
        );

        if (danger) {
            button.className = "delete-zone";
        }

        const symbol =
            document.createElement("span");

        symbol.className =
            "material-symbols-rounded";

        symbol.textContent = icon;

        button.appendChild(symbol);

        button.addEventListener(
            "click",
            onClick
        );

        return button;
    }

    // =====================================
    // RENDER ZONES TABLE
    // =====================================

    function renderTable() {
        tableBody.replaceChildren();

        $("zonesCount").textContent =
            `${zones.length} منطقة`;

        if (!zones.length) {
            const row =
                document.createElement("tr");

            const cell =
                document.createElement("td");

            cell.colSpan = 5;

            cell.textContent =
                "لا توجد مناطق بعد. ابدئي بإضافة أول منطقة.";

            cell.style.textAlign = "center";

            row.appendChild(cell);
            tableBody.appendChild(row);

            return;
        }

        zones.forEach(zone => {
            const row =
                document.createElement("tr");

            const boundary =
                parseBoundary(zone.boundaryPoints);

            const locationText =
                boundary.length >= 3
                    ? "حدود محددة"
                    : hasPosition(zone)
                        ? "نقطة فقط"
                        : "غير محدد";

            const values = [
                zone.name,

                Number(zone.capacity)
                    .toLocaleString("ar-SA"),

                Number(zone.initialVisitors)
                    .toLocaleString("ar-SA"),

                locationText
            ];

            values.forEach(value => {
                const cell =
                    document.createElement("td");

                cell.textContent = value;

                row.appendChild(cell);
            });

            const actions =
                document.createElement("td");

            actions.appendChild(
                createTableButton(
                    "edit",
                    "تعديل المنطقة",
                    () => openEditor(zone)
                )
            );

            actions.appendChild(
                createTableButton(
                    "delete",
                    "حذف المنطقة",
                    () => deleteZone(zone),
                    true
                )
            );

            row.appendChild(actions);
            tableBody.appendChild(row);
        });
    }

    // =====================================
    // DELETE ZONE
    // =====================================

    async function deleteZone(zone) {
        const confirmed = window.confirm(
            `هل تريدين حذف منطقة "${zone.name}"؟`
        );

        if (!confirmed) return;

        try {
            await api(`/delete/${zone.id}`, {
                method: "DELETE"
            });

            closeEditor();
            await loadZones();

            setMapStatus(
                "تم حذف المنطقة بنجاح"
            );

        } catch (error) {
            window.alert(
                "تعذر حذف المنطقة. " +
                "قد تكون مرتبطة بمسارات أو زيارات.\n\n" +
                error.message
            );
        }
    }

    // =====================================
    // LOAD ZONES
    // =====================================

    async function loadZones() {
        try {
            const data = await api("/get");

            if (!Array.isArray(data)) {
                throw new Error(
                    "بيانات المناطق غير صحيحة."
                );
            }

            zones = data;

            renderTable();
            renderMap();

        } catch (error) {
            console.error(
                "Load zones failed:",
                error
            );

            setMapStatus(
                "تعذر تحميل المناطق"
            );

            tableBody.replaceChildren();

            const row =
                document.createElement("tr");

            const cell =
                document.createElement("td");

            cell.colSpan = 5;

            cell.textContent =
                "تعذر تحميل المناطق. يرجى تحديث الصفحة.";

            row.appendChild(cell);
            tableBody.appendChild(row);
        }
    }

    // =====================================
    // START
    // =====================================

// Show the map immediately, even when no zones exist
    renderMap();

// Load saved zones separately
    loadZones();
});