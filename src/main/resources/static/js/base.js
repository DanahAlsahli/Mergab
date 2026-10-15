
/* ===== Shared Navbar & Footer ===== */

document.addEventListener('DOMContentLoaded', () => {

    const menuButton = document.getElementById('navMenuToggle');
    const navigation = document.getElementById('mainNavigation');

    if (menuButton && navigation) {
        menuButton.addEventListener('click', () => {
            const isOpen = navigation.classList.toggle('is-open');

            menuButton.setAttribute('aria-expanded', String(isOpen));

            menuButton.querySelector('.material-symbols-rounded')
                .textContent = isOpen ? 'close' : 'menu';
        });

        navigation.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                navigation.classList.remove('is-open');
                menuButton.setAttribute('aria-expanded', 'false');
                menuButton.querySelector('.material-symbols-rounded')
                    .textContent = 'menu';
            });
        });
    }

    // Highlight current navigation link.
    const currentPath = window.location.pathname;

    document.querySelectorAll('.header-nav a').forEach(link => {
        const href = link.getAttribute('href');

        if (href === currentPath) {
            link.setAttribute('aria-current', 'page');
        }
    });

    // Account destination based on saved session.
    const accountLink = document.getElementById('accountLink');

    if (accountLink) {
        try {
            const session = JSON.parse(
                sessionStorage.getItem('mergab.session') || 'null'
            );

            if (session?.role && session?.token) {
                const destinations = {
                    ADMIN: '/admin',
                    ORGANIZER: '/organizer/dashboard',
                    VISITOR: '/visitor/account'
                };

                accountLink.href = destinations[session.role] || '/login';
                accountLink.dataset.t = 'account';
                accountLink.textContent =
                    document.documentElement.lang === 'en'
                        ? 'My account'
                        : 'حسابي';
            }
        } catch (error) {
            console.warn('Invalid session data');
        }
    }
});


/* ===== Shared Language Management ===== */

window.Mergab = window.Mergab || {};

Mergab.getLanguage = function () {
    return localStorage.getItem('mergab.lang') === 'en' ? 'en' : 'ar';
};

Mergab.setLanguage = function (language) {
    const lang = language === 'en' ? 'en' : 'ar';

    localStorage.setItem('mergab.lang', lang);

    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';

    document.dispatchEvent(
        new CustomEvent('mergab:languageChanged', {
            detail: { language: lang }
        })
    );
};

Mergab.toggleLanguage = function () {
    const next = Mergab.getLanguage() === 'ar' ? 'en' : 'ar';
    Mergab.setLanguage(next);
};



/* ===== Shared Layout Translations ===== */

Mergab.layoutTranslations = {
    ar: {
        navHome: 'الرئيسية',
        navMap: 'الخريطة',
        navEvents: 'الفعاليات',
        navFeatures: 'المميزات',
        login: 'تسجيل الدخول',
        account: 'حسابي',
        footerTag: 'استكشف بذكاء، واستمتع بكل لحظة.'
    },
    en: {
        navHome: 'Home',
        navMap: 'Map',
        navEvents: 'Events',
        navFeatures: 'Features',
        login: 'Log in',
        account: 'My account',
        footerTag: 'Explore smarter, enjoy every moment.'
    }
};

Mergab.updateLayoutLanguage = function () {
    const lang = Mergab.getLanguage();
    const translations = Mergab.layoutTranslations[lang];

    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';

    document.querySelectorAll(
        '.site-header [data-t], .site-footer [data-t]'
    ).forEach(element => {
        const key = element.dataset.t;

        if (translations[key]) {
            element.textContent = translations[key];
        }
    });

    const languageButton = document.getElementById('langSwitch');

    if (languageButton) {
        languageButton.textContent =
            lang === 'ar' ? 'English' : 'العربية';
    }
};

document.addEventListener('mergab:languageChanged', () => {
    Mergab.updateLayoutLanguage();
});

document.addEventListener('DOMContentLoaded', () => {
    Mergab.updateLayoutLanguage();
});


/* ===== Shared Language Switch ===== */

document.addEventListener('DOMContentLoaded', () => {
    const languageButton = document.getElementById('langSwitch');

    if (languageButton) {
        languageButton.addEventListener('click', () => {
            Mergab.toggleLanguage();
        });
    }
});