
/* ==========================================
   MERGAB — ADMIN USERS MANAGEMENT
   ========================================== */

(() => {
    "use strict";

    const $ = id => document.getElementById(id);

    const auth = window.MergabAuth;

    // Security: check authentication before displaying the page.
    if (!auth) {
        window.location.replace("/login");
        return;
    }

    const session = auth.requireRole(["ADMIN"]);
    if (!session) return;

    const API = "/api/v1";

    let users = [];
    let userToDelete = null;
    let loading = false;

    const translations = {
        ar: {
            eyebrow: "لوحة الإدارة",
            pageTitle: "إدارة المستخدمين",
            pageDesc: "إدارة حسابات مِرقاب وإضافة المنظّمين والمسؤولين.",
            dashboard: "لوحة التحكم",
            addUser: "إضافة حساب",
            allUsers: "إجمالي المستخدمين",
            organizers: "المنظّمون",
            visitors: "الزوار",
            admins: "المسؤولون",
            accountsTitle: "الحسابات المسجلة",
            accountsDesc: "ابحث عن المستخدمين وصفِّ الحسابات حسب الدور.",
            searchPlaceholder: "ابحث بالاسم أو البريد الإلكتروني",
            filterAll: "جميع الأدوار",
            name: "الاسم",
            email: "البريد الإلكتروني",
            role: "الدور",
            phone: "رقم الجوال",
            actions: "الإجراءات",
            newAccount: "حساب جديد",
            addUserDesc: "أنشئ حساب منظّم أو مسؤول جديد في مِرقاب.",
            organizer: "منظّم",
            admin: "مسؤول",
            fullName: "الاسم الكامل",
            phoneHint: "مطلوب للمنظّم، ويبدأ بـ 05 ويتكوّن من 10 أرقام.",
            password: "كلمة المرور",
            passwordHint: "ستُستخدم كلمة المرور لتسجيل دخول الحساب الجديد.",
            cancel: "إلغاء",
            createAccount: "إنشاء الحساب",
            confirmDelete: "تأكيد حذف الحساب",
            deleteWarning: "هل أنت متأكد من حذف هذا الحساب؟ لا يمكن التراجع عن العملية.",
            deleteAccount: "حذف الحساب",
            loading: "جاري تحميل المستخدمين...",
            empty: "لا توجد حسابات مطابقة للبحث.",
            usersCount: "عدد الحسابات المعروضة: ",
            you: "أنت",
            loadingError: "تعذر تحميل الحسابات حاليًا. حاول مرة ثانية.",
            sessionExpired: "انتهت جلسة الدخول، يرجى تسجيل الدخول مجددًا.",
            saved: "تم إنشاء الحساب بنجاح.",
            deleted: "تم حذف الحساب بنجاح.",
            saving: "جاري إنشاء الحساب...",
            deleting: "جاري حذف الحساب...",
            delete: "حذف",
            refresh: "تحديث",
            selfDelete: "لا يمكنك حذف حسابك من هذه الصفحة.",
            required: "يرجى تعبئة جميع الحقول المطلوبة.",
            invalidPhone: "رقم الجوال لازم يبدأ بـ 05 ويتكون من 10 أرقام.",
            invalidPassword: "كلمة المرور لازم تكون 6 أحرف على الأقل.",
            duplicateEmail: "البريد الإلكتروني مستخدم مسبقًا.",
            duplicatePhone: "رقم الجوال مستخدم مسبقًا.",
            deleteBlocked: "لا يمكن حذف هذا الزائر لوجود سجل زيارات مرتبط بحسابه.",
            genericError: "تعذرت العملية حاليًا. يرجى المحاولة مرة أخرى.",
            serverError: "حدث خطأ أثناء معالجة الطلب. يرجى المحاولة لاحقًا."
        },

        en: {
            eyebrow: "Administration",
            pageTitle: "User Management",
            pageDesc: "Manage Mergab accounts and create organizer or admin accounts.",
            dashboard: "Dashboard",
            addUser: "Add Account",
            allUsers: "Total Users",
            organizers: "Organizers",
            visitors: "Visitors",
            admins: "Administrators",
            accountsTitle: "Registered Accounts",
            accountsDesc: "Search users and filter accounts by role.",
            searchPlaceholder: "Search by name or email",
            filterAll: "All Roles",
            name: "Name",
            email: "Email",
            role: "Role",
            phone: "Phone Number",
            actions: "Actions",
            newAccount: "New Account",
            addUserDesc: "Create an organizer or administrator account.",
            organizer: "Organizer",
            admin: "Administrator",
            fullName: "Full Name",
            phoneHint: "Required for organizers. Must start with 05 and contain 10 digits.",
            password: "Password",
            passwordHint: "This password will be used to log into the new account.",
            cancel: "Cancel",
            createAccount: "Create Account",
            confirmDelete: "Confirm Account Deletion",
            deleteWarning: "Are you sure you want to delete this account? This action cannot be undone.",
            deleteAccount: "Delete Account",
            loading: "Loading users...",
            empty: "No matching accounts found.",
            usersCount: "Accounts displayed: ",
            you: "You",
            loadingError: "Unable to load accounts. Please try again.",
            sessionExpired: "Your session has expired. Please log in again.",
            saved: "Account created successfully.",
            deleted: "Account deleted successfully.",
            saving: "Creating account...",
            deleting: "Deleting account...",
            delete: "Delete",
            refresh: "Refresh",
            selfDelete: "You cannot delete your own account here.",
            required: "Please complete all required fields.",
            invalidPhone: "Phone number must start with 05 and contain 10 digits.",
            invalidPassword: "Password must contain at least 6 characters.",
            duplicateEmail: "This email address is already registered.",
            duplicatePhone: "This phone number is already registered.",
            deleteBlocked: "This visitor cannot be deleted because the account has visit history.",
            genericError: "Unable to complete the request. Please try again.",
            serverError: "An error occurred while processing your request."
        }
    };

    const lang = () =>
        window.Mergab?.getLanguage?.() === "en" ? "en" : "ar";

    const t = key => translations[lang()][key] || key;

    const escapeHtml = value =>
        String(value ?? "").replace(/[&<>"']/g, char => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[char]);

    function translatePage() {
        document.querySelectorAll("[data-a]").forEach(element => {
            element.textContent = t(element.dataset.a);
        });

        document.querySelectorAll("[data-a-placeholder]").forEach(element => {
            element.placeholder = t(element.dataset.aPlaceholder);
        });

        $("refreshUsers").setAttribute("aria-label", t("refresh"));
    }

    /* ==========================================
       AUTHENTICATED API REQUESTS
       ========================================== */

    async function apiRequest(path, options = {}) {
        const token = auth.getAuthorizationHeader();

        if (!token) {
            auth.clearSession();
            window.location.replace("/login");
            throw new Error("SESSION_EXPIRED");
        }

        const response = await fetch(API + path, {
            method: options.method || "GET",
            headers: {
                "Accept": "application/json",
                "Authorization": token,
                ...(options.body !== undefined
                    ? { "Content-Type": "application/json" }
                    : {})
            },
            body: options.body !== undefined
                ? JSON.stringify(options.body)
                : undefined,
            cache: "no-store"
        });

        if (response.status === 401) {
            auth.clearSession();

            window.location.replace(
                "/login?next=" +
                encodeURIComponent(window.location.pathname)
            );

            throw new Error("SESSION_EXPIRED");
        }

        if (response.status === 403) {
            window.location.replace("/forbidden");
            throw new Error("FORBIDDEN");
        }

        const contentType =
            response.headers.get("content-type") || "";

        let data = null;

        if (contentType.includes("application/json")) {
            data = await response.json().catch(() => null);
        }

        if (!response.ok) {
            const error = new Error(
                data?.message ||
                data?.error ||
                "HTTP " + response.status
            );

            error.status = response.status;
            throw error;
        }

        return data;
    }

    function errorMessage(error) {
        const message = String(error?.message || "");

        if (message.includes("Email already exists")) {
            return t("duplicateEmail");
        }

        if (message.includes("Phone number already exists")) {
            return t("duplicatePhone");
        }

        if (message.includes("Cannot delete a visitor with zone visit history")) {
            return t("deleteBlocked");
        }

        if (message.includes("SESSION_EXPIRED")) {
            return t("sessionExpired");
        }

        if (error?.status >= 500) {
            return t("serverError");
        }

        if (error?.status >= 400) {
            return message && !message.startsWith("HTTP ")
                ? message
                : t("genericError");
        }

        return t("genericError");
    }

    /* ==========================================
       MESSAGES
       ========================================== */

    function showMessage(text, type = "") {
        const element = $("usersMessage");

        element.textContent = text;
        element.className = "admin-message";

        if (type) element.classList.add(type);
    }

    function showDialogError(id, text) {
        const element = $(id);

        element.textContent = text;
        element.hidden = false;
    }

    function clearDialogError(id) {
        $(id).textContent = "";
        $(id).hidden = true;
    }

    /* ==========================================
       STATISTICS
       ========================================== */

    function updateStatistics() {
        $("totalUsers").textContent = users.length;

        $("totalAdmins").textContent =
            users.filter(u => u.role === "ADMIN").length;

        $("totalOrganizers").textContent =
            users.filter(u => u.role === "ORGANIZER").length;

        $("totalVisitors").textContent =
            users.filter(u => u.role === "VISITOR").length;
    }

    /* ==========================================
       USERS TABLE
       ========================================== */

    function getFilteredUsers() {
        const query = $("userSearch").value
            .trim()
            .toLowerCase();

        const selectedRole = $("roleFilter").value;

        return users.filter(user => {
            const matchesRole =
                selectedRole === "ALL" ||
                user.role === selectedRole;

            const searchText = [
                user.fullName,
                user.email,
                user.id
            ].join(" ").toLowerCase();

            return matchesRole && searchText.includes(query);
        });
    }

    function roleBadge(role) {
        const names = {
            ADMIN: t("admin"),
            ORGANIZER: t("organizer"),
            VISITOR: t("visitors")
        };

        const type = ["ADMIN", "ORGANIZER", "VISITOR"].includes(role)
            ? role.toLowerCase()
            : "";

        return `
            <span class="admin-role-badge ${type}">
                ${escapeHtml(names[role] || role)}
            </span>
        `;
    }

    function renderUsers() {
        const filtered = getFilteredUsers().sort(
            (a, b) => Number(b.id) - Number(a.id)
        );

        $("usersCount").textContent =
            t("usersCount") + filtered.length;

        if (!filtered.length) {
            $("usersTableBody").innerHTML = `
                <tr>
                    <td colspan="5" style="text-align:center;padding:35px">
                        ${escapeHtml(t("empty"))}
                    </td>
                </tr>
            `;
            return;
        }

        $("usersTableBody").innerHTML = filtered.map(user => {
            const isCurrentUser =
                String(user.id) === String(session.id);

            const phone =
                user.organizer?.phoneNumber ||
                user.visitor?.phoneNumber ||
                "—";

            return `
                <tr>
                    <td>
                        <strong>${escapeHtml(user.fullName)}</strong>
                        ${isCurrentUser
                ? `<small>(${escapeHtml(t("you"))})</small>`
                : ""}
                    </td>

                    <td>
                        <span class="admin-user-email" dir="ltr">
                            ${escapeHtml(user.email)}
                        </span>
                    </td>

                    <td>${roleBadge(user.role)}</td>

                    <td dir="ltr">
                        ${escapeHtml(phone)}
                    </td>

                    <td>
                        <div class="admin-table-actions">
                            <button
                                type="button"
                                class="admin-table-delete"
                                data-delete-id="${escapeHtml(user.id)}"
                                aria-label="${escapeHtml(t("delete"))}"
                                title="${escapeHtml(
                isCurrentUser
                    ? t("selfDelete")
                    : t("delete")
            )}"
                                ${isCurrentUser ? "disabled" : ""}>

                                <span class="material-symbols-rounded">
                                    delete
                                </span>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join("");
    }

    /* ==========================================
       LOAD USERS
       ========================================== */

    async function loadUsers() {
        if (loading) return;

        loading = true;
        $("refreshUsers").disabled = true;

        showMessage(t("loading"));

        try {
            const data = await apiRequest("/user/get-all");

            if (!Array.isArray(data)) {
                throw new Error("INVALID_USERS_RESPONSE");
            }

            users = data;

            updateStatistics();
            renderUsers();

            showMessage("");

        } catch (error) {
            console.error("Mergab Admin Users:", error);

            showMessage(
                error.message === "INVALID_USERS_RESPONSE"
                    ? t("loadingError")
                    : errorMessage(error),
                "error"
            );

        } finally {
            loading = false;
            $("refreshUsers").disabled = false;
        }
    }

    /* ==========================================
       CREATE ADMIN / ORGANIZER
       ========================================== */

    function updateRoleFields() {
        const isOrganizer =
            $("newUserRole").value === "ORGANIZER";

        $("organizerPhoneField").hidden = !isOrganizer;
        $("newUserPhone").required = isOrganizer;

        if (!isOrganizer) {
            $("newUserPhone").value = "";
        }
    }

    function openAddDialog() {
        $("addUserForm").reset();

        $("newUserRole").value = "ORGANIZER";

        updateRoleFields();
        clearDialogError("addUserError");

        $("addUserDialog").showModal();
    }

    function closeAddDialog() {
        $("addUserDialog").close();
    }

    async function createUser(event) {
        event.preventDefault();

        const role = $("newUserRole").value;

        const fullName = $("newUserName").value.trim();
        const email = $("newUserEmail").value.trim();
        const password = $("newUserPassword").value;
        const phoneNumber = $("newUserPhone").value.trim();

        clearDialogError("addUserError");

        if (!fullName || !email || !password) {
            showDialogError("addUserError", t("required"));
            return;
        }

        if (password.length < 6) {
            showDialogError("addUserError", t("invalidPassword"));
            return;
        }

        if (
            role === "ORGANIZER" &&
            !/^05[0-9]{8}$/.test(phoneNumber)
        ) {
            showDialogError("addUserError", t("invalidPhone"));
            return;
        }

        const payload = {
            fullName,
            email,
            password
        };

        let endpoint;

        if (role === "ORGANIZER") {
            endpoint = "/organizer/register";
            payload.phoneNumber = phoneNumber;

        } else {
            endpoint = "/user/add-admin";
            payload.role = "ADMIN";
        }

        const button = $("submitAddUser");

        button.disabled = true;

        try {
            await apiRequest(endpoint, {
                method: "POST",
                body: payload
            });

            closeAddDialog();

            await loadUsers();

            showMessage(t("saved"), "success");

        } catch (error) {
            console.error("Create account:", error);

            showDialogError(
                "addUserError",
                errorMessage(error)
            );

        } finally {
            button.disabled = false;
        }
    }

    /* ==========================================
       DELETE USER
       ========================================== */

    function openDeleteDialog(id) {
        const user = users.find(
            item => String(item.id) === String(id)
        );

        if (!user) return;

        if (String(user.id) === String(session.id)) {
            showMessage(t("selfDelete"), "error");
            return;
        }

        userToDelete = user;

        $("deleteUserName").textContent =
            user.fullName + " — " + user.email;

        clearDialogError("deleteUserError");

        $("deleteUserDialog").showModal();
    }

    function closeDeleteDialog() {
        $("deleteUserDialog").close();
        userToDelete = null;
    }

    async function deleteUser() {
        if (!userToDelete) return;

        const button = $("confirmDeleteUser");
        const userId = userToDelete.id;

        button.disabled = true;

        clearDialogError("deleteUserError");

        try {
            await apiRequest("/user/delete/" + userId, {
                method: "DELETE"
            });

            closeDeleteDialog();

            await loadUsers();

            showMessage(t("deleted"), "success");

        } catch (error) {
            console.error("Delete account:", error);

            showDialogError(
                "deleteUserError",
                errorMessage(error)
            );

        } finally {
            button.disabled = false;
        }
    }

    /* ==========================================
       LANGUAGE CHANGE
       ========================================== */

    function updateLanguage() {
        translatePage();

        renderUsers();
        updateStatistics();
        updateRoleFields();
    }

    /* ==========================================
       EVENT LISTENERS
       ========================================== */

    function bindEvents() {
        $("userSearch").addEventListener(
            "input",
            renderUsers
        );

        $("roleFilter").addEventListener(
            "change",
            renderUsers
        );

        $("refreshUsers").addEventListener(
            "click",
            loadUsers
        );

        $("openAddUser").addEventListener(
            "click",
            openAddDialog
        );

        $("closeAddUser").addEventListener(
            "click",
            closeAddDialog
        );

        $("cancelAddUser").addEventListener(
            "click",
            closeAddDialog
        );

        $("newUserRole").addEventListener(
            "change",
            updateRoleFields
        );

        $("addUserForm").addEventListener(
            "submit",
            createUser
        );

        $("usersTableBody").addEventListener(
            "click",
            event => {
                const button =
                    event.target.closest("[data-delete-id]");

                if (!button || button.disabled) return;

                openDeleteDialog(button.dataset.deleteId);
            }
        );

        $("cancelDeleteUser").addEventListener(
            "click",
            closeDeleteDialog
        );

        $("confirmDeleteUser").addEventListener(
            "click",
            deleteUser
        );

        document.addEventListener(
            "mergab:languageChanged",
            updateLanguage
        );
    }

    /* ==========================================
       INITIALIZATION
       ========================================== */

    async function init() {
        // Check the real server identity, not only sessionStorage.
        try {
            const me = await apiRequest("/user/my-account");

            if (me?.role !== "ADMIN") {
                window.location.replace("/forbidden");
                return;
            }

        } catch (error) {
            console.error("Admin authorization:", error);

            // apiRequest handles 401 and 403 redirects.
            if (
                error.message !== "SESSION_EXPIRED" &&
                error.message !== "FORBIDDEN"
            ) {
                window.location.replace("/login");
            }

            return;
        }

        $("adminUsersPage").hidden = false;

        translatePage();
        bindEvents();
        updateRoleFields();

        await loadUsers();

        // Direct link: /admin/users#add
        if (window.location.hash === "#add") {
            openAddDialog();
            history.replaceState(null, "", "/admin/users");
        }
    }

    init();

})();
