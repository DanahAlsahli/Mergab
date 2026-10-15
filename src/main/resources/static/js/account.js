
/* ========================================
   MERGAB | ACCOUNT PAGE
   ======================================== */

document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const session = MergabAuth.requireRole(["ADMIN", "ORGANIZER"]);

    if (!session) return;

    const $ = id => document.getElementById(id);

    const profileForm = $("profileForm");
    const passwordForm = $("passwordForm");

    const fullNameInput = $("fullName");
    const emailInput = $("email");
    const roleInput = $("role");

    const roleNames = {
        ADMIN: "مدير النظام",
        ORGANIZER: "منظّم"
    };

    // Dashboard navigation
    const dashboardUrl = MergabAuth.getHomePage(session.role);

    $("dashboardLink").href = dashboardUrl;
    $("profileDashboardLink").href = dashboardUrl;

    $("logoutButton").addEventListener("click", () => {
        MergabAuth.logout();
    });

    function showMessage(id, message, type) {
        const element = $(id);

        element.textContent = message;
        element.className = `account-message ${type}`;
        element.hidden = false;
    }

    function clearMessage(id) {
        const element = $(id);

        element.hidden = true;
        element.textContent = "";
        element.className = "account-message";
    }


    let currentProfile = null;

    function updateProfileUI(user) {
        currentProfile = { ...user };

        const roleName = roleNames[user.role] || user.role;

        // Profile card
        $("profileName").textContent = user.fullName || "—";
        $("profileEmail").textContent = user.email || "—";
        $("profileRole").textContent = roleName;

        // View mode
        $("viewFullName").textContent = user.fullName || "—";
        $("viewEmail").textContent = user.email || "—";
        $("viewRole").textContent = roleName;

        // Edit mode
        fullNameInput.value = user.fullName || "";
        emailInput.value = user.email || "";
        roleInput.value = roleName;

        // Return to view mode
        $("profileView").hidden = false;
        profileForm.hidden = true;
        $("editProfileButton").style.display = "";
    }

    // Open edit mode
    $("editProfileButton").addEventListener("click", () => {
        if (!currentProfile) return;

        clearMessage("profileMessage");

        $("profileView").hidden = true;
        profileForm.hidden = false;
        $("editProfileButton").style.display = "none";

        fullNameInput.focus();
    });

    // Cancel editing
    $("cancelProfileButton").addEventListener("click", () => {
        if (!currentProfile) return;

        updateProfileUI(currentProfile);
        clearMessage("profileMessage");
    });


    function redirectToLogin() {
        MergabAuth.clearSession();

        window.location.replace(
            "/login?next=" +
            encodeURIComponent(window.location.pathname)
        );
    }

    async function request(url, options = {}) {
        const token = MergabAuth.getAuthorizationHeader();

        if (!token) {
            redirectToLogin();
            throw new Error("UNAUTHORIZED");
        }

        const response = await fetch(url, {
            ...options,
            headers: {
                "Authorization": token,
                "Accept": "application/json",
                "X-Requested-With": "XMLHttpRequest",
                ...(options.body
                    ? { "Content-Type": "application/json" }
                    : {}),
                ...options.headers
            },
            cache: "no-store"
        });

        if (response.status === 401) {
            redirectToLogin();
            throw new Error("UNAUTHORIZED");
        }

        if (!response.ok) {
            throw new Error(`REQUEST_FAILED_${response.status}`);
        }

        return response.status === 204
            ? null
            : response.json();
    }

// Expand / collapse password settings
    const passwordToggle = $("togglePasswordSection");
    const passwordPanel = $("passwordPanel");

    passwordToggle.addEventListener("click", () => {
        const isOpen = passwordToggle.getAttribute("aria-expanded") === "true";

        passwordToggle.setAttribute("aria-expanded", String(!isOpen));
        passwordPanel.hidden = isOpen;

        if (isOpen) {
            passwordForm.reset();
            clearMessage("passwordMessage");
        }
    });

    // Load profile from backend
    async function loadProfile() {
        try {
            const user = await request(
                "/api/v1/user/my-account"
            );

            updateProfileUI(user);

            const currentSession = MergabAuth.getSession();

            if (currentSession) {
                MergabAuth.saveSession({
                    ...currentSession,
                    id: user.id,
                    fullName: user.fullName,
                    email: user.email,
                    role: user.role
                });
            }

        } catch (error) {
            if (error.message === "UNAUTHORIZED") return;

            showMessage(
                "profileMessage",
                "تعذر تحميل بيانات الحساب. يرجى تحديث الصفحة.",
                "error"
            );

            console.error("Account loading failed:", error);
        }
    }

    // Update full name and email
    profileForm.addEventListener("submit", async event => {
        event.preventDefault();
        clearMessage("profileMessage");

        const fullName = fullNameInput.value.trim();
        const email = emailInput.value.trim();

        if (fullName.length < 3 || fullName.length > 50) {
            showMessage(
                "profileMessage",
                "الاسم يجب أن يكون بين 3 و50 حرفًا.",
                "error"
            );
            return;
        }

        if (!emailInput.validity.valid || !email) {
            showMessage(
                "profileMessage",
                "يرجى إدخال بريد إلكتروني صحيح.",
                "error"
            );
            return;
        }

        const button = $("saveProfileButton");
        button.disabled = true;

        const previousSession = MergabAuth.getSession();
        const emailChanged =
            email !== previousSession?.email;

        try {
            await request("/api/v1/user/update-my-account", {
                method: "PUT",
                body: JSON.stringify({
                    fullName,
                    email
                })
            });

            updateProfileUI({
                fullName,
                email,
                role: session.role
            });

            if (emailChanged) {
                // HTTP Basic uses the email as username.
                // The old authorization token is now invalid.
                showMessage(
                    "profileMessage",
                    "تم تحديث البيانات. سجّلي الدخول مجددًا بالبريد الجديد.",
                    "success"
                );

                MergabAuth.clearSession();

                window.location.replace("/login");
                return;
            }

            MergabAuth.saveSession({
                ...previousSession,
                fullName,
                email
            });

            showMessage(
                "profileMessage",
                "تم حفظ بياناتك بنجاح.",
                "success"
            );

        } catch (error) {
            if (error.message === "UNAUTHORIZED") return;

            showMessage(
                "profileMessage",
                "تعذر حفظ البيانات. تأكدي من البريد الإلكتروني وحاولي مجددًا.",
                "error"
            );

            console.error("Profile update failed:", error);

        } finally {
            button.disabled = false;
        }
    });

    // Change password
    passwordForm.addEventListener("submit", async event => {
        event.preventDefault();
        clearMessage("passwordMessage");

        const oldPassword = $("oldPassword").value;
        const newPassword = $("newPassword").value;
        const confirmPassword = $("confirmPassword").value;

        if (newPassword.length < 6) {
            showMessage(
                "passwordMessage",
                "كلمة المرور الجديدة يجب ألا تقل عن 6 أحرف.",
                "error"
            );
            return;
        }

        if (newPassword !== confirmPassword) {
            showMessage(
                "passwordMessage",
                "تأكيد كلمة المرور غير مطابق.",
                "error"
            );
            return;
        }

        if (oldPassword === newPassword) {
            showMessage(
                "passwordMessage",
                "كلمة المرور الجديدة يجب أن تختلف عن الحالية.",
                "error"
            );
            return;
        }

        const button = $("changePasswordButton");
        button.disabled = true;

        try {
            await request("/api/v1/user/change-password", {
                method: "PUT",
                body: JSON.stringify({
                    oldPassword,
                    newPassword
                })
            });

            passwordForm.reset();

            // The saved HTTP Basic credentials are now outdated.
            MergabAuth.clearSession();

            window.location.replace("/login");

        } catch (error) {
            if (error.message === "UNAUTHORIZED") return;

            showMessage(
                "passwordMessage",
                "تعذر تغيير كلمة المرور. تأكدي من كلمة المرور الحالية.",
                "error"
            );

            console.error("Password change failed:", error);

        } finally {
            button.disabled = false;
        }
    });

    loadProfile();
});
