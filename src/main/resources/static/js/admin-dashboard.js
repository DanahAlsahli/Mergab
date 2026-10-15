
/* ==========================================
   MERGAB — ADMIN DASHBOARD
   ========================================== */

(() => {
    "use strict";

    const $ = id => document.getElementById(id);
    const auth = window.MergabAuth;

    if (!auth) {
        window.location.replace("/login");
        return;
    }

    // Allow only logged-in administrators.
    const session = auth.requireRole(["ADMIN"]);
    if (!session) return;

    const API = "/api/v1";

    let USERS = [];
    let isLoading = false;

    const translations = {
        ar: {
            eyebrow: "إدارة مِرقاب",
            welcome: "أهلًا بك في لوحة الإدارة",
            welcomeName: "أهلًا بك، ",
            welcomeDesc: "تابع حسابات المستخدمين وأدر المنظّمين المسؤولين عن مناطق إكسبو.",
            refresh: "تحديث البيانات",
            addOrganizer: "إضافة منظّم",
            totalUsers: "إجمالي المستخدمين",
            totalOrganizers: "المنظّمون",
            totalVisitors: "الزوار",
            totalAdmins: "المسؤولون",
            recentUsers: "أحدث الحسابات",
            recentDesc: "آخر الحسابات المسجلة في مِرقاب.",
            viewAll: "عرض الكل",
            expoOverview: "نظرة عامة على إكسبو",
            expoDesc: "معلومات عن المناطق والفعاليات المسجلة.",
            totalZones: "المناطق المسجلة",
            totalEvents: "الفعاليات المسجلة",
            managementTitle: "إدارة الحسابات والصلاحيات",
            managementDesc: "أنشئ حسابات المنظّمين والمسؤولين وتابع المستخدمين.",
            manageUsers: "إدارة جميع المستخدمين",
            createAccount: "إنشاء حساب جديد",
            loading: "جاري تحميل البيانات...",
            loaded: "تم تحديث البيانات.",
            partialError: "بعض البيانات غير متاحة حاليًا. يمكنك المحاولة مرة أخرى.",
            loginRequired: "يرجى تسجيل الدخول للمتابعة.",
            accessDenied: "ليس لديك صلاحية للوصول إلى هذه الصفحة.",
            noAccounts: "لا توجد حسابات مسجلة حاليًا.",
            usersError: "تعذر تحميل بيانات المستخدمين.",
            updated: "آخر تحديث: ",
            admin: "مسؤول",
            organizer: "منظّم",
            visitor: "زائر"
        },

        en: {
            eyebrow: "Mergab Administration",
            welcome: "Welcome to the Admin Dashboard",
            welcomeName: "Welcome, ",
            welcomeDesc: "Manage user accounts and organizers responsible for Expo zones.",
            refresh: "Refresh Data",
            addOrganizer: "Add Organizer",
            totalUsers: "Total Users",
            totalOrganizers: "Organizers",
            totalVisitors: "Visitors",
            totalAdmins: "Administrators",
            recentUsers: "Recent Accounts",
            recentDesc: "The latest accounts registered in Mergab.",
            viewAll: "View All",
            expoOverview: "Expo Overview",
            expoDesc: "Registered zones and events.",
            totalZones: "Registered Zones",
            totalEvents: "Registered Events",
            managementTitle: "Accounts & Permissions",
            managementDesc: "Create organizer and administrator accounts and manage users.",
            manageUsers: "Manage All Users",
            createAccount: "Create New Account",
            loading: "Loading data...",
            loaded: "Data updated successfully.",
            partialError: "Some data is temporarily unavailable. Please try again.",
            loginRequired: "Please log in to continue.",
            accessDenied: "You do not have permission to access this page.",
            noAccounts: "No accounts registered yet.",
            usersError: "Unable to load user accounts.",
            updated: "Last updated: ",
            admin: "Administrator",
            organizer: "Organizer",
            visitor: "Visitor"
        }
    };

    function getLanguage() {
        return window.Mergab?.getLanguage?.() === "en"
            ? "en"
            : "ar";
    }

    function t(key) {
        return translations[getLanguage()][key] || key;
    }

    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>"']/g, char => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[char]);
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString(
            getLanguage() === "ar" ? "ar-SA" : "en-US"
        );
    }

    function getRoleName(role) {
        const roles = {
            ADMIN: t("admin"),
            ORGANIZER: t("organizer"),
            VISITOR: t("visitor")
        };

        return roles[role] || role || "—";
    }

    /* ==========================================
       API
       ========================================== */

    async function apiRequest(path, authenticated = true) {
        const headers = {
            Accept: "application/json"
        };

        if (authenticated) {
            const token = auth.getAuthorizationHeader();

            if (!token) {
                auth.clearSession();
                window.location.replace("/login");
                throw new Error("SESSION_EXPIRED");
            }

            headers.Authorization = token;
        }

        const response = await fetch(API + path, {
            method: "GET",
            headers,
            cache: "no-store"
        });

        if (authenticated && response.status === 401) {
            auth.clearSession();

            window.location.replace(
                "/login?next=" +
                encodeURIComponent(window.location.pathname)
            );

            throw new Error("SESSION_EXPIRED");
        }

        if (authenticated && response.status === 403) {
            window.location.replace("/forbidden");
            throw new Error("FORBIDDEN");
        }

        if (!response.ok) {
            throw new Error("HTTP_" + response.status);
        }

        return response.json();
    }

    /* ==========================================
       LANGUAGE & WELCOME
       ========================================== */

    let currentAdmin = null;

    function translatePage() {
        document.querySelectorAll("[data-a]").forEach(element => {
            element.textContent = t(element.dataset.a);
        });

        if (currentAdmin?.fullName) {
            $("adminWelcome").textContent =
                t("welcomeName") + currentAdmin.fullName;
        } else {
            $("adminWelcome").textContent = t("welcome");
        }

        if (USERS.length) {
            renderRecentUsers();
        }
    }

    /* ==========================================
       FEEDBACK
       ========================================== */

    function showMessage(text, type = "") {
        const element = $("dashboardMessage");

        element.textContent = text;
        element.className = "admin-message";

        if (type) element.classList.add(type);
    }

    /* ==========================================
       ACCOUNT STATISTICS
       ========================================== */

    function renderUserStats(users) {
        const count = role =>
            users.filter(user => user.role === role).length;

        $("dashTotalUsers").textContent =
            formatNumber(users.length);

        $("dashTotalOrganizers").textContent =
            formatNumber(count("ORGANIZER"));

        $("dashTotalVisitors").textContent =
            formatNumber(count("VISITOR"));

        $("dashTotalAdmins").textContent =
            formatNumber(count("ADMIN"));
    }

    /* ==========================================
       RECENT ACCOUNTS
       ========================================== */

    function renderRecentUsers() {
        const container = $("recentUsersList");

        if (!USERS.length) {
            container.innerHTML =
                `<p>${escapeHtml(t("noAccounts"))}</p>`;
            return;
        }

        // Newer records generally have higher generated IDs.
        const recent = USERS.slice()
            .sort((a, b) => Number(b.id) - Number(a.id))
            .slice(0, 5);

        container.innerHTML = recent.map(user => {
            const role = String(user.role || "");

            const badgeClass =
                ["ADMIN", "ORGANIZER", "VISITOR"].includes(role)
                    ? role.toLowerCase()
                    : "";

            return `
                <div class="admin-recent-item">

                    <div style="min-width:0">
                        <strong style="
                            display:block;
                            color:var(--admin-primary);
                            overflow-wrap:anywhere;
                        ">
                            ${escapeHtml(user.fullName)}
                        </strong>

                        <small style="
                            color:var(--admin-muted);
                            overflow-wrap:anywhere;
                        ">
                            ${escapeHtml(user.email)}
                        </small>
                    </div>

                    <span class="admin-role-badge ${badgeClass}">
                        ${escapeHtml(getRoleName(role))}
                    </span>

                </div>
            `;
        }).join("");
    }

    /* ==========================================
       DASHBOARD DATA
       ========================================== */

    function readArray(data) {
        if (Array.isArray(data)) return data;
        if (Array.isArray(data?.data)) return data.data;
        if (Array.isArray(data?.content)) return data.content;

        throw new Error("INVALID_RESPONSE");
    }

    async function loadDashboard() {
        if (isLoading) return;

        isLoading = true;
        $("refreshDashboard").disabled = true;

        showMessage(t("loading"));

        // Independent requests so one failed API does not
        // prevent the other dashboard sections from loading.
        const results = await Promise.allSettled([
            apiRequest("/user/get-all"),
            apiRequest("/zone/get", false),
            apiRequest("/expo-event/get", false)
        ]);

        const [usersResult, zonesResult, eventsResult] = results;

        let hasErrors = false;

        // Users
        if (usersResult.status === "fulfilled") {
            try {
                USERS = readArray(usersResult.value);

                renderUserStats(USERS);
                renderRecentUsers();

            } catch (error) {
                hasErrors = true;
                console.error("Admin users response:", error);
            }
        } else {
            hasErrors = true;
            console.error("Admin users API:", usersResult.reason);

            $("recentUsersList").textContent = t("usersError");
        }

        // Zones
        if (zonesResult.status === "fulfilled") {
            try {
                $("dashTotalZones").textContent =
                    formatNumber(readArray(zonesResult.value).length);
            } catch (error) {
                hasErrors = true;
                console.error("Admin zones response:", error);
            }
        } else {
            hasErrors = true;
            console.error("Admin zones API:", zonesResult.reason);
        }

        // Events
        if (eventsResult.status === "fulfilled") {
            try {
                $("dashTotalEvents").textContent =
                    formatNumber(readArray(eventsResult.value).length);
            } catch (error) {
                hasErrors = true;
                console.error("Admin events response:", error);
            }
        } else {
            hasErrors = true;
            console.error("Admin events API:", eventsResult.reason);
        }

        if (hasErrors) {
            showMessage(t("partialError"), "error");
        } else {
            showMessage("");
        }

        const now = new Date();

        $("dashboardUpdated").textContent =
            t("updated") +
            now.toLocaleString(
                getLanguage() === "ar" ? "ar-SA" : "en-US",
                {
                    dateStyle: "medium",
                    timeStyle: "short"
                }
            );

        $("refreshDashboard").disabled = false;
        isLoading = false;
    }

    /* ==========================================
       AUTHORIZATION
       ========================================== */

    async function verifyAdmin() {
        const me = await apiRequest("/user/my-account");

        if (me?.role !== "ADMIN") {
            window.location.replace("/forbidden");
            return false;
        }

        currentAdmin = me;
        return true;
    }

    /* ==========================================
       INITIALIZATION
       ========================================== */

    async function init() {
        try {
            const allowed = await verifyAdmin();

            if (!allowed) return;

        } catch (error) {
            console.error("Admin authorization:", error);

            if (
                error.message === "SESSION_EXPIRED" ||
                error.message === "FORBIDDEN"
            ) {
                return;
            }

            // Do not display privileged content without verification.
            const notice = document.createElement("p");
            notice.setAttribute("role", "alert");
            notice.style.cssText =
                "padding:28px;text-align:center;color:#A63535;";

            notice.textContent =
                getLanguage() === "ar"
                    ? "تعذر التحقق من الجلسة حاليًا. يرجى تحديث الصفحة."
                    : "Unable to verify your session. Please refresh the page.";

            $("adminDashboardPage").before(notice);
            return;
        }

        // Show the dashboard only after confirming ADMIN.
        $("adminDashboardPage").hidden = false;

        translatePage();

        $("refreshDashboard").addEventListener(
            "click",
            loadDashboard
        );

        document.addEventListener(
            "mergab:languageChanged",
            () => {
                translatePage();
                renderUserStats(USERS);
                renderRecentUsers();
            }
        );

        await loadDashboard();
    }

    init();

})();
