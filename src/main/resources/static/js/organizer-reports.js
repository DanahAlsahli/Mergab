
/* ==========================================
   MERGAB | ORGANIZER REPORTS
   Real Spring Boot API Integration
   ========================================== */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const session = MergabAuth.requireRole(["ORGANIZER"]);
    if (!session) return;

    const $ = id => document.getElementById(id);

    const API = "/api/v1";

    const formatNumber = value =>
        Number(value || 0).toLocaleString("ar-SA");

    const escapeHTML = value =>
        String(value ?? "").replace(/[&<>"']/g, character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]);

    let simulations = [];
    let attendance = [];
    let selectedSimulation = null;
    let currentReport = null;
    let editingAttendanceId = null;
    let requestSequence = 0;

    // ======================================
    // AUTHENTICATION
    // ======================================

    $("organizerAccountLink").textContent =
        session.fullName || "حسابي";

    $("organizerLogoutButton").addEventListener(
        "click",
        () => MergabAuth.logout()
    );

    // ======================================
    // MESSAGES
    // ======================================

    function showMessage(message, type = "error",
                         targetId = "reportsMessage") {

        const element = $(targetId);

        element.textContent = message;
        element.className = `zones-form-message ${type}`;
        element.hidden = false;
    }

    function clearMessage(targetId = "reportsMessage") {
        const element = $(targetId);
        element.hidden = true;
        element.textContent = "";
    }

    // ======================================
    // API REQUEST
    // ======================================

    async function apiRequest(path, options = {}) {

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
            throw new Error("ما عندك صلاحية لهذه العملية.");
        }

        const raw = await response.text();

        let result = null;

        if (raw) {
            try {
                result = JSON.parse(raw);
            } catch {
                result = null;
            }
        }

        if (!response.ok) {
            throw new Error(
                result?.message ||
                result?.detail ||
                `فشل الطلب (HTTP ${response.status})`
            );
        }

        return result;
    }

    // ======================================
    // RESET SIMULATION REPORT
    // ======================================

    function resetSimulationReport(
        message = "اختاري محاكاة مكتملة لعرض نتائجها."
    ) {

        [
            "reportPeakVisitors",
            "reportAverageOccupancy",
            "reportPeakQueue",
            "reportZoneCount"
        ].forEach(id => {
            $(id).textContent = "—";
        });

        $("reportPeakTime").textContent = "—";
        $("reportPeakQueueZone").textContent = "—";

        $("reportOccupancyChart").innerHTML =
            `<p class="organizer-empty">
                ${escapeHTML(message)}
             </p>`;

        $("reportBusiestZones").innerHTML =
            `<p class="organizer-empty">
                ${escapeHTML(message)}
             </p>`;

        $("reportZonesTableBody").innerHTML =
            `<tr>
                <td colspan="6" class="reports-empty-cell">
                    ${escapeHTML(message)}
                </td>
             </tr>`;

        $("exportReportButton").disabled = true;

        currentReport = null;
    }

    // ======================================
    // PROCESS REAL SIMULATION SNAPSHOTS
    // ======================================

    function calculateSimulationReport(snapshots) {

        const occupancyByMinute = new Map();
        const zoneResults = new Map();

        for (const snapshot of snapshots) {

            const minute = Number(snapshot.minute);
            const occupancy = Number(snapshot.occupancy);
            const queueLength = Number(snapshot.queueLength);

            const zoneId = Number(snapshot.zone?.id);

            if (![
                minute,
                occupancy,
                queueLength,
                zoneId
            ].every(Number.isFinite)) {
                continue;
            }

            // Total occupancy for each minute

            occupancyByMinute.set(
                minute,
                (occupancyByMinute.get(minute) || 0)
                + occupancy
            );

            // Statistics for each zone

            if (!zoneResults.has(zoneId)) {

                zoneResults.set(zoneId, {
                    id: zoneId,

                    name: snapshot.zone?.name ||
                        `منطقة #${zoneId}`,

                    capacity:
                        Number(snapshot.zone?.capacity) || 0,

                    peakOccupancy: occupancy,

                    peakMinute: minute,

                    peakQueue: queueLength
                });
            }

            const zone = zoneResults.get(zoneId);

            if (occupancy > zone.peakOccupancy) {

                zone.peakOccupancy = occupancy;
                zone.peakMinute = minute;
            }

            zone.peakQueue = Math.max(
                zone.peakQueue,
                queueLength
            );
        }

        const timeline = Array.from(
            occupancyByMinute.entries()
        )
            .sort((a, b) => a[0] - b[0])
            .map(([minute, occupancy]) => ({
                minute,
                occupancy
            }));

        const zones = Array.from(zoneResults.values())
            .map(zone => ({
                ...zone,

                peakPercent: zone.capacity > 0
                    ? (zone.peakOccupancy / zone.capacity) * 100
                    : null
            }))
            .sort((a, b) =>
                (b.peakPercent ?? -1) -
                (a.peakPercent ?? -1)
            );

        const peak = timeline.reduce(
            (highest, point) =>
                !highest ||
                point.occupancy > highest.occupancy
                    ? point
                    : highest,
            null
        );

        const averageOccupancy = timeline.length
            ? timeline.reduce(
            (total, point) =>
                total + point.occupancy,
            0
        ) / timeline.length
            : 0;

        const longestQueueZone = zones.reduce(
            (highest, zone) =>
                !highest ||
                zone.peakQueue > highest.peakQueue
                    ? zone
                    : highest,
            null
        );

        return {
            timeline,
            zones,
            peak,
            averageOccupancy,
            longestQueueZone
        };
    }

    // ======================================
    // OCCUPANCY SVG CHART
    // ======================================

    function drawOccupancyChart(timeline) {

        const container = $("reportOccupancyChart");

        if (!timeline.length) {
            container.innerHTML =
                `<p class="organizer-empty">
                    ما فيه بيانات لعرض الرسم البياني.
                 </p>`;
            return;
        }

        const width = 800;
        const height = 300;

        const left = 60;
        const right = 18;
        const top = 18;
        const bottom = 42;

        const plotWidth = width - left - right;
        const plotHeight = height - top - bottom;

        const maxOccupancy = Math.max(
            1,
            ...timeline.map(p => p.occupancy)
        );

        const firstMinute = timeline[0].minute;

        const lastMinute =
            timeline[timeline.length - 1].minute;

        const minuteRange = Math.max(
            1,
            lastMinute - firstMinute
        );

        function x(point) {
            return left +
                ((point.minute - firstMinute) /
                    minuteRange) * plotWidth;
        }

        function y(point) {
            return top + plotHeight -
                (point.occupancy / maxOccupancy)
                * plotHeight;
        }

        // Reduce SVG points for long simulations,
        // without changing calculated statistics.

        const step = Math.max(
            1,
            Math.ceil(timeline.length / 250)
        );

        const points = timeline.filter(
            (_, index) => index % step === 0
        );

        const lastPoint = timeline[timeline.length - 1];

        if (points[points.length - 1] !== lastPoint) {
            points.push(lastPoint);
        }

        const linePath = points.map(
            (point, index) =>
                `${index === 0 ? "M" : "L"} 
                 ${x(point).toFixed(2)} 
                 ${y(point).toFixed(2)}`
        ).join(" ");

        const areaPath = `
            ${linePath}
            L ${x(points[points.length - 1])}
              ${top + plotHeight}
            L ${x(points[0])}
              ${top + plotHeight}
            Z
        `;

        let gridLines = "";
        let xLabels = "";

        for (let i = 0; i <= 4; i++) {

            const value = maxOccupancy * i / 4;

            const gridY =
                top + plotHeight -
                (i * plotHeight / 4);

            gridLines += `
                <line
                    x1="${left}"
                    y1="${gridY}"
                    x2="${width - right}"
                    y2="${gridY}"
                    stroke="#e9eee7"
                />

                <text
                    x="${left - 10}"
                    y="${gridY + 4}"
                    text-anchor="end"
                    fill="#6b7d72"
                    font-size="11"
                >
                    ${Math.round(value).toLocaleString("en-US")}
                </text>
            `;

            const minute =
                firstMinute + minuteRange * i / 4;

            const labelX =
                left + i * plotWidth / 4;

            xLabels += `
                <text
                    x="${labelX}"
                    y="${height - 12}"
                    text-anchor="middle"
                    fill="#6b7d72"
                    font-size="11"
                >
                    ${Math.round(minute)} د
                </text>
            `;
        }

        container.innerHTML = `
            <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 ${width} ${height}"
                role="img"
                aria-label="إشغال مناطق المحاكاة"
            >

                <defs>
                    <linearGradient
                        id="reportsChartFill"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                    >
                        <stop
                            offset="0"
                            stop-color="#427e60"
                            stop-opacity=".26"
                        />
                        <stop
                            offset="1"
                            stop-color="#427e60"
                            stop-opacity=".02"
                        />
                    </linearGradient>
                </defs>

                ${gridLines}

                <path
                    d="${areaPath}"
                    fill="url(#reportsChartFill)"
                />

                <path
                    d="${linePath}"
                    fill="none"
                    stroke="#347756"
                    stroke-width="3"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                />

                ${xLabels}
            </svg>
        `;
    }

    // ======================================
    // RENDER SIMULATION STATISTICS
    // ======================================

    function renderSimulationReport() {

        if (!currentReport?.timeline.length) {

            resetSimulationReport(
                "المحاكاة ما فيها نتائج محفوظة."
            );

            return;
        }

        const {
            peak,
            averageOccupancy,
            longestQueueZone,
            zones,
            timeline
        } = currentReport;

        $("reportPeakVisitors").textContent =
            formatNumber(peak.occupancy);

        $("reportPeakTime").textContent =
            `عند الدقيقة ${formatNumber(peak.minute)}`;

        $("reportAverageOccupancy").textContent =
            formatNumber(Math.round(averageOccupancy));

        $("reportPeakQueue").textContent =
            formatNumber(longestQueueZone?.peakQueue || 0);

        $("reportPeakQueueZone").textContent =
            longestQueueZone?.name || "—";

        $("reportZoneCount").textContent =
            formatNumber(zones.length);

        drawOccupancyChart(timeline);

        // Busiest zones

        $("reportBusiestZones").innerHTML =
            zones.map(zone => {

                const percent = zone.peakPercent;

                const level =
                    percent == null
                        ? ""
                        : percent >= 90
                            ? "danger"
                            : percent >= 70
                                ? "warning"
                                : "";

                const width = Math.min(
                    100,
                    Math.max(0, percent ?? 0)
                );

                return `
                    <div class="reports-zone-item">

                        <div class="reports-zone-item-head">

                            <span class="reports-zone-name">
                                ${escapeHTML(zone.name)}
                            </span>

                            <span class="reports-zone-percent">
                                ${percent == null
                    ? "—"
                    : percent.toFixed(1) + "%"}
                            </span>

                        </div>

                        <div class="reports-progress-track">
                            <div
                                class="reports-progress-fill ${level}"
                                style="width:${width}%"
                            ></div>
                        </div>

                        <span class="reports-zone-meta">
                            ذروة ${formatNumber(zone.peakOccupancy)}
                            زائر من سعة ${formatNumber(zone.capacity)}
                        </span>

                    </div>
                `;

            }).join("") ||
            `<p class="organizer-empty">
                لا توجد مناطق.
             </p>`;

        // Zone details table

        $("reportZonesTableBody").innerHTML =
            zones.map(zone => `
                <tr>
                    <td>
                        <strong>${escapeHTML(zone.name)}</strong>
                    </td>

                    <td>
                        ${formatNumber(zone.capacity)}
                    </td>

                    <td>
                        ${formatNumber(zone.peakOccupancy)}
                    </td>

                    <td>
                        ${zone.peakPercent == null
                ? "—"
                : zone.peakPercent.toFixed(1) + "%"}
                    </td>

                    <td>
                        ${formatNumber(zone.peakQueue)}
                    </td>

                    <td>
                        ${formatNumber(zone.peakMinute)}
                    </td>
                </tr>
            `).join("");

        $("exportReportButton").disabled = false;
    }

    // ======================================
    // LOAD SIMULATIONS
    // ======================================

    function renderSimulationOptions(preferredId = "") {

        const select = $("reportSimulationSelect");

        const completed = simulations.filter(
            simulation =>
                simulation.status === "COMPLETED"
        );

        select.replaceChildren();

        select.add(new Option(
            completed.length
                ? "اختاري محاكاة مكتملة"
                : "لا توجد محاكاة مكتملة",
            ""
        ));

        completed.forEach(simulation => {

            select.add(new Option(
                `محاكاة #${simulation.id} — 
                 ${simulation.durationMinutes} دقيقة`,

                String(simulation.id)
            ));
        });

        select.disabled = completed.length === 0;

        if (
            preferredId &&
            completed.some(
                simulation =>
                    String(simulation.id) === String(preferredId)
            )
        ) {
            select.value = String(preferredId);
        }
    }

    async function loadSelectedSimulation() {

        const sequence = ++requestSequence;

        const id = $("reportSimulationSelect").value;

        selectedSimulation = simulations.find(
            simulation => String(simulation.id) === id
        ) || null;

        resetSimulationReport(
            id
                ? "جاري تحميل نتائج المحاكاة..."
                : "اختاري محاكاة مكتملة لعرض نتائجها."
        );

        if (!id) return;

        try {

            const result = await apiRequest(
                `/simulation-snapshot/get-by-simulation/${encodeURIComponent(id)}`
            );

            if (sequence !== requestSequence) return;

            const snapshots = Array.isArray(result)
                ? result
                : [];

            currentReport =
                calculateSimulationReport(snapshots);

            renderSimulationReport();

        } catch (error) {

            if (sequence !== requestSequence) return;

            resetSimulationReport(
                "تعذر تحميل نتائج المحاكاة."
            );

            showMessage(error.message);
        }
    }

    async function loadSimulations(preferredId = "") {

        try {

            const result = await apiRequest(
                "/simulation/get-all"
            );

            simulations = Array.isArray(result)
                ? result
                : [];

            renderSimulationOptions(preferredId);

            await loadSelectedSimulation();

        } catch (error) {

            $("reportSimulationSelect").replaceChildren(
                new Option("تعذر تحميل المحاكاة", "")
            );

            resetSimulationReport(
                "تعذر تحميل المحاكاة."
            );

            showMessage(error.message);
        }
    }

    // ======================================
    // HISTORICAL ATTENDANCE SUMMARY
    // ======================================

    function updateAttendanceSummary() {

        const totals = attendance.map(
            record => Number(record.totalAttendance) || 0
        );

        const totalVisitors = totals.reduce(
            (sum, value) => sum + value,
            0
        );

        $("reportHistoryCount").textContent =
            formatNumber(attendance.length);

        $("reportHistoryTotal").textContent =
            formatNumber(totalVisitors);

        $("reportHistoryPeak").textContent =
            attendance.length
                ? formatNumber(Math.max(...totals))
                : "—";
    }

    // ======================================
    // RENDER ATTENDANCE TABLE
    // ======================================

    function renderAttendance() {

        const search = $("attendanceSearch")
            .value.trim().toLowerCase();

        const filtered = attendance.filter(record =>
            `${record.expoName} ${record.attendanceDate}`
                .toLowerCase()
                .includes(search)
        );

        $("attendanceTableBody").innerHTML =
            filtered.length
                ? filtered.map(record => {

                    const sourceUrl =
                        String(record.sourceUrl || "");

                    const validUrl =
                        /^https?:\/\//i.test(sourceUrl);

                    return `
                        <tr>

                            <td>
                                <strong>
                                    ${escapeHTML(record.expoName)}
                                </strong>
                            </td>

                            <td>
                                ${escapeHTML(record.attendanceDate)}
                            </td>

                            <td>
                                ${formatNumber(record.totalAttendance)}
                            </td>

                            <td>
                                ${validUrl
                        ? `<a
                                        class="reports-source-link"
                                        href="${escapeHTML(sourceUrl)}"
                                        target="_blank"
                                        rel="noopener noreferrer"
                                       >
                                          عرض المصدر
                                       </a>`
                        : "—"}
                            </td>

                            <td>
                                <div class="reports-table-actions">

                                    <button
                                        type="button"
                                        class="reports-action-btn"
                                        data-edit="${record.id}"
                                        aria-label="تعديل"
                                    >
                                        <span class="material-symbols-rounded">
                                            edit
                                        </span>
                                    </button>

                                    <button
                                        type="button"
                                        class="reports-action-btn danger"
                                        data-delete="${record.id}"
                                        aria-label="حذف"
                                    >
                                        <span class="material-symbols-rounded">
                                            delete
                                        </span>
                                    </button>

                                </div>
                            </td>

                        </tr>
                    `;

                }).join("")
                : `<tr>
                    <td colspan="5" class="reports-empty-cell">
                        لا توجد سجلات مطابقة.
                    </td>
                   </tr>`;
    }

    // ======================================
    // LOAD HISTORICAL ATTENDANCE
    // ======================================

    async function loadAttendance() {

        try {

            const result = await apiRequest(
                "/attendance/get-all"
            );

            attendance = (
                Array.isArray(result) ? result : []
            ).sort(
                (a, b) =>
                    String(b.attendanceDate)
                        .localeCompare(String(a.attendanceDate))
            );

            updateAttendanceSummary();
            renderAttendance();

        } catch (error) {

            $("attendanceTableBody").innerHTML =
                `<tr>
                    <td colspan="5" class="reports-empty-cell">
                        تعذر تحميل سجلات الحضور.
                    </td>
                 </tr>`;

            showMessage(error.message);
        }
    }

    // ======================================
    // ATTENDANCE EDITOR
    // ======================================

    function closeAttendanceEditor() {

        $("attendanceEditor").hidden = true;

        $("attendanceForm").reset();

        editingAttendanceId = null;

        clearMessage("attendanceFormMessage");
    }

    function openAttendanceEditor(record = null) {

        editingAttendanceId = record?.id ?? null;

        $("attendanceEditorTitle").textContent =
            record
                ? "تعديل سجل حضور"
                : "إضافة سجل حضور";

        $("attendanceForm").reset();

        $("attendanceExpoName").value =
            record?.expoName || "";

        $("attendanceDate").value =
            record?.attendanceDate || "";

        $("attendanceTotal").value =
            record?.totalAttendance ?? "";

        $("attendanceSource").value =
            record?.sourceUrl || "";

        $("attendanceEditor").hidden = false;

        clearMessage("attendanceFormMessage");

        $("attendanceEditor").scrollIntoView({
            behavior: "smooth",
            block: "nearest"
        });
    }

    // ======================================
    // ADD / UPDATE HISTORICAL ATTENDANCE
    // ======================================

    async function saveAttendance(event) {

        event.preventDefault();

        clearMessage("attendanceFormMessage");

        const expoName =
            $("attendanceExpoName").value.trim();

        const date =
            $("attendanceDate").value;

        const total =
            Number($("attendanceTotal").value);

        const sourceUrl =
            $("attendanceSource").value.trim();

        const today = new Date();

        const localToday = [
            today.getFullYear(),
            String(today.getMonth() + 1).padStart(2, "0"),
            String(today.getDate()).padStart(2, "0")
        ].join("-");

        if (
            !expoName ||
            !date ||
            !sourceUrl ||
            !Number.isInteger(total) ||
            total < 0 ||
            date > localToday
        ) {

            showMessage(
                "تأكدي من الاسم والتاريخ وعدد الزوار. التاريخ ما يكون بالمستقبل.",
                "error",
                "attendanceFormMessage"
            );

            return;
        }

        try {

            const parsedUrl = new URL(sourceUrl);

            if (!["http:", "https:"].includes(
                parsedUrl.protocol
            )) {
                throw new Error();
            }

        } catch {

            showMessage(
                "رابط المصدر لازم يبدأ بـ https:// أو http://",
                "error",
                "attendanceFormMessage"
            );

            return;
        }

        const saveButton = $("saveAttendanceButton");

        saveButton.disabled = true;

        try {

            const editing =
                editingAttendanceId !== null;

            const endpoint = editing
                ? `/attendance/update/${editingAttendanceId}`
                : "/attendance/add";

            await apiRequest(endpoint, {

                method: editing ? "PUT" : "POST",

                body: JSON.stringify({
                    expoName,
                    attendanceDate: date,
                    totalAttendance: total,
                    sourceUrl
                })
            });

            closeAttendanceEditor();

            showMessage(
                editing
                    ? "تم تحديث سجل الحضور بنجاح."
                    : "تمت إضافة سجل الحضور بنجاح.",
                "success"
            );

            await loadAttendance();

        } catch (error) {

            showMessage(
                error.message,
                "error",
                "attendanceFormMessage"
            );

        } finally {

            saveButton.disabled = false;
        }
    }

    // ======================================
    // DELETE HISTORICAL ATTENDANCE
    // ======================================

    async function deleteAttendance(id) {

        const record = attendance.find(
            item => Number(item.id) === Number(id)
        );

        if (!record) return;

        const confirmed = confirm(
            `متأكدة تبغين تحذفين سجل ${record.expoName} بتاريخ ${record.attendanceDate}؟`
        );

        if (!confirmed) return;

        try {

            await apiRequest(
                `/attendance/delete/${encodeURIComponent(id)}`,
                { method: "DELETE" }
            );

            showMessage(
                "تم حذف سجل الحضور بنجاح.",
                "success"
            );

            await loadAttendance();

        } catch (error) {

            // Backend prevents deleting attendance
            // when it is referenced by simulations.

            showMessage(error.message);
        }
    }

    // ======================================
    // EXPORT REAL REPORT AS CSV
    // ======================================

    function csvCell(value) {

        const text = String(value ?? "");

        // Prevent formula injection in Excel.

        const protectedText = /^[=+@\-\t\r]/.test(text)
            ? "'" + text
            : text;

        return `"${protectedText.replace(/"/g, '""')}"`;
    }

    function exportReportCSV() {

        if (
            !currentReport?.timeline.length ||
            !selectedSimulation
        ) {
            return;
        }

        const rows = [

            [
                "تقرير محاكاة مِرقاب",
                selectedSimulation.id
            ],

            [
                "مدة المحاكاة بالدقائق",
                selectedSimulation.durationMinutes
            ],

            [],

            [
                "المنطقة",
                "السعة",
                "ذروة الإشغال",
                "نسبة الذروة %",
                "أعلى طابور",
                "دقيقة الذروة"
            ],

            ...currentReport.zones.map(zone => [
                zone.name,
                zone.capacity,
                zone.peakOccupancy,

                zone.peakPercent == null
                    ? ""
                    : zone.peakPercent.toFixed(1),

                zone.peakQueue,
                zone.peakMinute
            ]),

            [],

            [
                "الدقيقة",
                "إجمالي الزوار في المناطق"
            ],

            ...currentReport.timeline.map(point => [
                point.minute,
                point.occupancy
            ])
        ];

        const csvContent =
            "\ufeff" +
            rows.map(
                row => row.map(csvCell).join(",")
            ).join("\r\n");

        const blob = new Blob(
            [csvContent],
            { type: "text/csv;charset=utf-8" }
        );

        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");

        link.href = url;

        link.download =
            `mergab-simulation-${selectedSimulation.id}-report.csv`;

        document.body.appendChild(link);

        link.click();

        link.remove();

        URL.revokeObjectURL(url);
    }

    // ======================================
    // EVENTS
    // ======================================

    $("reportSimulationSelect").addEventListener(
        "change",
        () => {
            clearMessage();
            loadSelectedSimulation();
        }
    );

    $("refreshReportsButton").addEventListener(
        "click",
        async () => {

            clearMessage();

            await Promise.all([
                loadSimulations(
                    $("reportSimulationSelect").value
                ),
                loadAttendance()
            ]);
        }
    );

    $("exportReportButton").addEventListener(
        "click",
        exportReportCSV
    );

    $("attendanceSearch").addEventListener(
        "input",
        renderAttendance
    );

    $("addAttendanceButton").addEventListener(
        "click",
        () => openAttendanceEditor()
    );

    $("closeAttendanceEditorButton").addEventListener(
        "click",
        closeAttendanceEditor
    );

    $("cancelAttendanceButton").addEventListener(
        "click",
        closeAttendanceEditor
    );

    $("attendanceForm").addEventListener(
        "submit",
        saveAttendance
    );

    $("attendanceTableBody").addEventListener(
        "click",
        event => {

            const editButton =
                event.target.closest("[data-edit]");

            const deleteButton =
                event.target.closest("[data-delete]");

            if (editButton) {

                const id = Number(
                    editButton.dataset.edit
                );

                const record = attendance.find(
                    item => Number(item.id) === id
                );

                if (record) {
                    openAttendanceEditor(record);
                }
            }

            if (deleteButton) {

                const id = Number(
                    deleteButton.dataset.delete
                );

                deleteAttendance(id);
            }
        }
    );

    // ======================================
    // INITIAL LOAD
    // ======================================

    resetSimulationReport();

    Promise.all([
        loadSimulations(),
        loadAttendance()
    ]);
});
