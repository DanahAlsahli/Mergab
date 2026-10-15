
/* ==========================================
   MERGAB | ORGANIZER EVENTS
   Real Spring Boot API Integration
   ========================================== */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const session = MergabAuth.requireRole(["ORGANIZER"]);
    if (!session) return;

    const $ = id => document.getElementById(id);

    const EVENT_API = "/api/v1/expo-event";
    const ZONE_API = "/api/v1/zone";

    const form = $("eventForm");
    const editor = $("eventEditor");
    const tableBody = $("eventsTableBody");
    const zoneSelect = $("eventZone");
    const saveButton = $("saveEventButton");

    let zones = [];
    let eventLists = {
        all: null,
        today: null,
        upcoming: null
    };

    let editingId = null;
    let originalZoneId = null;
    let activeFilter = "all";
    let saving = false;

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
    // MESSAGES
    // =====================================

    const pageNotice = document.createElement("div");
    pageNotice.className = "zones-form-message";
    pageNotice.hidden = true;

    document.querySelector(".zones-page-heading")
        .insertAdjacentElement("afterend", pageNotice);

    function showPageNotice(message, type = "success") {
        pageNotice.textContent = message;
        pageNotice.className = `zones-form-message ${type}`;
        pageNotice.hidden = false;
    }

    function showFormMessage(message, type = "error") {
        const element = $("eventFormMessage");
        element.textContent = message;
        element.className = `zones-form-message ${type}`;
        element.hidden = false;
    }

    function clearMessages() {
        $("eventFormMessage").hidden = true;
        $("eventFormMessage").textContent = "";
        pageNotice.hidden = true;
    }

    // =====================================
    // BACKEND REQUEST
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
                "لا تملكين صلاحية تنفيذ هذه العملية."
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
                `تعذرت العملية (HTTP ${response.status}).`
            );
        }

        return data;
    }

    // =====================================
    // DATE HELPERS
    // =====================================

    function parseDate(value) {
        if (!value) return null;

        const date = new Date(
            String(value).replace(" ", "T")
        );

        return Number.isNaN(date.getTime())
            ? null
            : date;
    }

    function formatDate(value) {
        const date = parseDate(value);

        if (!date) return "موعد غير متاح";

        return date.toLocaleString("ar-SA", {
            dateStyle: "medium",
            timeStyle: "short"
        });
    }

    function toDateTimeInput(value) {
        if (!value) return "";

        return String(value)
            .replace(" ", "T")
            .slice(0, 16);
    }

    function eventStatus(event) {
        const start = parseDate(event.startDateTime);
        const end = parseDate(event.endDateTime);

        if (!start || !end) {
            return {
                label: "غير محدد",
                className: "ended"
            };
        }

        const now = new Date();

        if (end <= now) {
            return {
                label: "منتهية",
                className: "ended"
            };
        }

        if (start > now) {
            return {
                label: "قادمة",
                className: "upcoming"
            };
        }

        return {
            label: "جارية الآن",
            className: "ongoing"
        };
    }

    function formatNumber(value) {
        const number = Number(value);

        return Number.isFinite(number)
            ? number.toLocaleString("ar-SA")
            : "—";
    }

    // =====================================
    // ZONE SELECTION
    // =====================================

    function populateZones() {
        zoneSelect.replaceChildren();

        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = zones.length
            ? "اختاري المنطقة"
            : "لا توجد مناطق متاحة";

        zoneSelect.appendChild(placeholder);

        zones.forEach(zone => {
            const option = document.createElement("option");
            option.value = String(zone.id);
            option.textContent = zone.name;
            zoneSelect.appendChild(option);
        });

        zoneSelect.disabled = zones.length === 0;
    }

    // =====================================
    // OPEN / CLOSE EDITOR
    // =====================================

    function openEditor(event = null) {
        if (!zones.length) {
            showPageNotice(
                "أضيفي منطقة من صفحة إدارة المناطق أولًا، أو تأكدي من تحميل المناطق.",
                "error"
            );
            return;
        }

        form.reset();
        clearMessages();

        editingId = event?.id ?? null;
        originalZoneId = event?.zone?.id ?? null;

        $("eventId").value = editingId ?? "";

        $("eventEditorTitle").textContent = event
            ? "تعديل الفعالية"
            : "إضافة فعالية جديدة";

        $("eventName").value = event?.name ?? "";

        $("eventStartDateTime").value =
            toDateTimeInput(event?.startDateTime);

        $("eventEndDateTime").value =
            toDateTimeInput(event?.endDateTime);

        $("eventExpectedDemand").value =
            event?.expectedDemand ?? "";

        zoneSelect.value = originalZoneId != null
            ? String(originalZoneId)
            : "";

        editor.hidden = false;

        editor.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }

    function closeEditor() {
        if (saving) return;

        editingId = null;
        originalZoneId = null;

        form.reset();
        editor.hidden = true;

        $("eventFormMessage").hidden = true;
    }

    $("addEventButton").addEventListener(
        "click",
        () => openEditor()
    );

    $("closeEventEditor").addEventListener(
        "click",
        closeEditor
    );

    $("cancelEventButton").addEventListener(
        "click",
        closeEditor
    );

    // =====================================
    // SAVE EVENT
    // =====================================

    form.addEventListener("submit", async event => {
        event.preventDefault();

        if (saving) return;

        $("eventFormMessage").hidden = true;

        const name = $("eventName").value.trim();

        const zoneId = zoneSelect.value;

        const startInput =
            $("eventStartDateTime").value;

        const endInput =
            $("eventEndDateTime").value;

        const expectedDemand = Number(
            $("eventExpectedDemand").value
        );

        if (!name || name.length > 100) {
            showFormMessage(
                "اسم الفعالية مطلوب وبحد أقصى 100 حرف."
            );
            return;
        }

        if (!zoneId) {
            showFormMessage("اختاري منطقة الفعالية.");
            return;
        }

        if (!startInput || !endInput) {
            showFormMessage(
                "حددي وقت بداية الفعالية ونهايتها."
            );
            return;
        }

        const startDate = parseDate(startInput);
        const endDate = parseDate(endInput);

        if (
            !startDate ||
            !endDate ||
            endDate <= startDate
        ) {
            showFormMessage(
                "وقت نهاية الفعالية لازم يكون بعد البداية."
            );
            return;
        }

        if (
            $("eventExpectedDemand").value === "" ||
            !Number.isInteger(expectedDemand) ||
            expectedDemand < 0
        ) {
            showFormMessage(
                "عدد الزوار المتوقع يجب أن يكون عددًا صحيحًا لا يقل عن صفر."
            );
            return;
        }

        // LocalDateTime format expected by Spring Boot
        const payload = {
            name,
            startDateTime: `${startInput}:00`,
            endDateTime: `${endInput}:00`,
            expectedDemand
        };

        saving = true;
        saveButton.disabled = true;

        let eventUpdated = false;

        try {
            if (editingId == null) {
                // Add event and assign the selected zone
                await request(
                    `${EVENT_API}/add/${zoneId}`,
                    {
                        method: "POST",
                        body: JSON.stringify(payload)
                    }
                );

            } else {
                // Update event details
                await request(
                    `${EVENT_API}/update/${editingId}`,
                    {
                        method: "PUT",
                        body: JSON.stringify(payload)
                    }
                );

                eventUpdated = true;

                // Changing a zone is a separate backend operation
                if (
                    String(zoneId) !==
                    String(originalZoneId)
                ) {
                    await request(
                        `${EVENT_API}/change-zone/${editingId}/${zoneId}`,
                        {
                            method: "PUT"
                        }
                    );
                }
            }

            editingId = null;
            originalZoneId = null;

            editor.hidden = true;
            form.reset();

            await loadData();

            showPageNotice(
                "تم حفظ الفعالية بنجاح.",
                "success"
            );

        } catch (error) {
            console.error(
                "Event save failed:",
                error
            );

            if (eventUpdated) {
                showFormMessage(
                    "تم تحديث بيانات الفعالية، لكن قد يكون تغيير المنطقة فشل. " +
                    error.message
                );

                // Refresh the table to reflect what was saved
                await loadData();
            } else {
                showFormMessage(error.message);
            }

        } finally {
            saving = false;
            saveButton.disabled = false;
        }
    });

    // =====================================
    // TABLE HELPERS
    // =====================================

    function createActionButton(
        icon,
        title,
        callback,
        danger = false
    ) {
        const button = document.createElement("button");
        button.type = "button";
        button.title = title;
        button.setAttribute("aria-label", title);

        if (danger) {
            button.className = "delete-zone";
        }

        const symbol = document.createElement("span");
        symbol.className = "material-symbols-rounded";
        symbol.textContent = icon;

        button.appendChild(symbol);
        button.addEventListener("click", callback);

        return button;
    }

    function createCell(row, content) {
        const cell = document.createElement("td");
        cell.textContent = String(content ?? "—");
        row.appendChild(cell);
        return cell;
    }

    function showTableMessage(message) {
        tableBody.replaceChildren();

        const row = document.createElement("tr");
        const cell = document.createElement("td");

        cell.colSpan = 7;
        cell.textContent = message;
        cell.style.textAlign = "center";

        row.appendChild(cell);
        tableBody.appendChild(row);
    }

    // =====================================
    // RENDER EVENTS
    // =====================================

    function renderTable() {
        const source = eventLists[activeFilter];

        if (!Array.isArray(source)) {
            $("eventsCount").textContent = "—";

            showTableMessage(
                "تعذر تحميل الفعاليات. حدّثي الصفحة للمحاولة مجددًا."
            );
            return;
        }

        const search = $("eventSearchInput")
            .value.trim()
            .toLowerCase();

        const filtered = source
            .filter(event => {
                if (!search) return true;

                const name = String(
                    event.name || ""
                ).toLowerCase();

                const zoneName = String(
                    event.zone?.name || ""
                ).toLowerCase();

                return name.includes(search) ||
                    zoneName.includes(search);
            })
            .sort((a, b) => {
                const aTime =
                    parseDate(a.startDateTime)?.getTime() ?? 0;

                const bTime =
                    parseDate(b.startDateTime)?.getTime() ?? 0;

                return aTime - bTime;
            });

        $("eventsCount").textContent =
            `${filtered.length} فعالية`;

        if (!filtered.length) {
            showTableMessage(
                search
                    ? "لا توجد فعاليات تطابق البحث."
                    : "لا توجد فعاليات ضمن هذا التصنيف."
            );
            return;
        }

        tableBody.replaceChildren();

        filtered.forEach(event => {
            const row = document.createElement("tr");

            createCell(row, event.name);

            createCell(
                row,
                event.zone?.name || "بدون منطقة"
            );

            createCell(
                row,
                formatDate(event.startDateTime)
            );

            createCell(
                row,
                formatDate(event.endDateTime)
            );

            createCell(
                row,
                formatNumber(event.expectedDemand)
            );

            const status = eventStatus(event);

            const statusCell = document.createElement("td");
            const badge = document.createElement("span");

            badge.className =
                `event-status ${status.className}`;

            badge.textContent = status.label;

            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            const actions = document.createElement("td");

            actions.appendChild(
                createActionButton(
                    "edit",
                    "تعديل الفعالية",
                    () => openEditor(event)
                )
            );

            actions.appendChild(
                createActionButton(
                    "delete",
                    "حذف الفعالية",
                    () => deleteEvent(event),
                    true
                )
            );

            row.appendChild(actions);
            tableBody.appendChild(row);
        });
    }

    // =====================================
    // DELETE EVENT
    // =====================================

    async function deleteEvent(event) {
        const confirmed = window.confirm(
            `هل تريدين حذف فعالية "${event.name}"؟`
        );

        if (!confirmed) return;

        try {
            await request(
                `${EVENT_API}/delete/${event.id}`,
                { method: "DELETE" }
            );

            closeEditor();

            await loadData();

            showPageNotice(
                "تم حذف الفعالية بنجاح.",
                "success"
            );

        } catch (error) {
            showPageNotice(
                "تعذر حذف الفعالية: " + error.message,
                "error"
            );
        }
    }

    // =====================================
    // FILTERS AND SEARCH
    // =====================================

    document.querySelectorAll(".events-filter")
        .forEach(button => {
            button.addEventListener("click", () => {
                activeFilter = button.dataset.filter;

                document.querySelectorAll(
                    ".events-filter"
                ).forEach(item => {
                    item.classList.toggle(
                        "active",
                        item === button
                    );
                });

                renderTable();
            });
        });

    $("eventSearchInput").addEventListener(
        "input",
        renderTable
    );

    // =====================================
    // LOAD EVENTS FROM BACKEND
    // =====================================

    async function loadData() {
        const results = await Promise.allSettled([
            request(`${ZONE_API}/get`),
            request(`${EVENT_API}/get`),
            request(`${EVENT_API}/get-today`),
            request(`${EVENT_API}/get-upcoming`)
        ]);

        const [zoneResult, allResult, todayResult, upcomingResult] =
            results;

        if (
            zoneResult.status === "fulfilled" &&
            Array.isArray(zoneResult.value)
        ) {
            zones = zoneResult.value;
        } else {
            zones = [];
            console.error(
                "Failed loading event zones:",
                zoneResult.reason
            );
        }

        populateZones();

        function getList(result) {
            return result.status === "fulfilled" &&
            Array.isArray(result.value)
                ? result.value
                : null;
        }

        eventLists = {
            all: getList(allResult),
            today: getList(todayResult),
            upcoming: getList(upcomingResult)
        };

        $("totalEventsCount").textContent =
            eventLists.all
                ? formatNumber(eventLists.all.length)
                : "—";

        $("todayEventsCount").textContent =
            eventLists.today
                ? formatNumber(eventLists.today.length)
                : "—";

        $("upcomingEventsCount").textContent =
            eventLists.upcoming
                ? formatNumber(eventLists.upcoming.length)
                : "—";

        if (allResult.status === "rejected") {
            console.error(
                "Failed loading all events:",
                allResult.reason
            );
        }

        if (todayResult.status === "rejected") {
            console.error(
                "Failed loading today's events:",
                todayResult.reason
            );
        }

        if (upcomingResult.status === "rejected") {
            console.error(
                "Failed loading upcoming events:",
                upcomingResult.reason
            );
        }

        renderTable();
    }

    // =====================================
    // INITIALIZE
    // =====================================

    loadData().catch(error => {
        console.error("Events initialization failed:", error);

        showPageNotice(
            "تعذر تحميل بيانات الفعاليات.",
            "error"
        );
    });
});
