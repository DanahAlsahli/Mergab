/* =========================================================
   Mergab — visitor.js
   هيدر وفوتر موقع الزائر، اللوقو، زر اللغة، صور وألوان المناطق
   ========================================================= */
(function (g) {
  'use strict';
  var M = g.Mergab, esc = M.esc, t = g.t, CFG = g.MERGAB_CONFIG || {};

  // الأيقونات من Google Material Symbols (معرّفة في api.js)
  function icon(name, size, color, fill) { return M.icon(name, size, color, fill); }
  // سهم "التالي" حسب اتجاه اللغة
  function next(size) { return icon(g.I18n && g.I18n.isAr ? 'arrowStart' : 'arrowEnd', size || 18); }

  /* ---------- المناطق: لون + أيقونة + صورة ---------- */
  var STYLE = {
    'main plaza':      { icon: 'plaza',   a: '#C8892B', b: '#7A4E0E' },
    'innovation hub':  { icon: 'bulb',    a: '#E08A3C', b: '#8A3F12' },
    'culture zone':    { icon: 'culture', a: '#6B4FC2', b: '#2E1F66' },
    'food court':      { icon: 'food',    a: '#2E9E6A', b: '#10533A' },
    'exhibition hall': { icon: 'hall',    a: '#2F6FD0', b: '#173C7A' },
    'family zone':     { icon: 'family',  a: '#D4527C', b: '#7A1E40' },
    'north gate':      { icon: 'gate',    a: '#3F7F6E', b: '#173B33' },
    'south gate':      { icon: 'gate',    a: '#4F8A7A', b: '#1C463C' }
  };
  function zoneKey(z) { return String(z && (z.name || z) || '').trim().toLowerCase(); }
  function zoneStyle(z) { return STYLE[zoneKey(z)] || { icon: 'pin', a: '#5C7A70', b: '#22382F' }; }
  function zoneSlug(z) { return zoneKey(z).replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''); }
  function zoneImage(z) { return M.ROOT + (CFG.zoneImagesFolder || 'assets/img/zones/') + zoneSlug(z) + '.jpg'; }
  // الصورة فوق والتدرّج تحتها: لو الصورة مو موجودة يطلع التدرّج
  function zoneArt(z) {
    var s = zoneStyle(z);
    return "background-image: url('" + zoneImage(z) + "'), radial-gradient(circle at 80% 15%, rgba(255,255,255,.22), transparent 45%), linear-gradient(135deg, " + s.a + ", " + s.b + ");" +
      'background-size: cover, auto, auto; background-position: center;';
  }

  /* ---------- اللوقو ---------- */
  var FALLBACK_SVG = '<svg viewBox="0 0 48 48" width="42" height="42" aria-hidden="true"><g fill="none" stroke="#0F5C4A" stroke-width="2.4" stroke-linecap="round">' +
    '<path d="M6 34c3-10 9-16 18-16s15 6 18 16"/><path d="M11 34c2-7 7-11 13-11s11 4 13 11"/><path d="M16 34c1.5-4 4.5-6.5 8-6.5s6.5 2.5 8 6.5"/>' +
    '<path d="M24 18V8M18 12l6-4 6 4"/></g><path d="M4 38h40" stroke="#C8892B" stroke-width="2.4" stroke-linecap="round"/></svg>';
  function fallbackLogo(light) {
    return '<span class="brand-fallback">' + FALLBACK_SVG + '<span class="t"><b' + (light ? ' style="color:#fff"' : '') + '>' + esc(t('brand')) + '</b><small' + (light ? ' style="color:#B8CBC1"' : '') + '>MERGAB</small></span></span>';
  }
  function logoHtml(light) {
    var src = light && CFG.logoLight ? CFG.logoLight : CFG.logo;
    if (!src) return fallbackLogo(light);
    return '<img class="logo-img" data-logo="' + (light ? 'light' : 'dark') + '" src="' + esc(M.ROOT + src) + '" alt="' + esc(t('brand')) + '">';
  }
  // لو ملف اللوقو مو موجود نحط المؤقت
  function wireLogos(root) {
    root.querySelectorAll('img[data-logo]').forEach(function (img) {
      var light = img.getAttribute('data-logo') === 'light';
      function swap() {
        // النسخة البيضاء مو موجودة؟ جرّب العادية
        if (light && CFG.logo && img.getAttribute('src') !== M.ROOT + CFG.logo) { img.src = M.ROOT + CFG.logo; img.style.filter = 'brightness(0) invert(1)'; return; }
        var span = document.createElement('span');
        span.innerHTML = fallbackLogo(light);
        img.replaceWith(span.firstChild);
      }
      img.addEventListener('error', swap);
      if (img.complete && img.naturalWidth === 0) swap();
    });
  }

  function langButton() {
    return '<button type="button" class="lang-btn" data-lang aria-label="' + esc(t('lang.aria')) + '" lang="' + (g.I18n.isAr ? 'en' : 'ar') + '">' + icon('globe', 16) + esc(t('lang.switch')) + '</button>';
  }

  // الخريطة وتخطيط الرحلة صاروا صفحة وحدة (map.html فيها قسم "أفضل مسار لك")
  var NAV = [
    ['index.html', 'nav.home'],
    ['map.html', 'nav.map'],
    ['events.html', 'nav.events'],
    ['visit.html', 'nav.visit']
  ];

  function shell() {
    var page = location.pathname.split('/').pop() || 'index.html';
    var me = M.auth.get();
    var here = encodeURIComponent(location.pathname + location.search);
    var actions;
    if (!me) {
      actions = '<a class="btn btn-ghost btn-sm" href="' + M.ROOT + 'login.html?next=' + here + '">' + esc(t('auth.login')) + '</a>' +
        '<a class="btn btn-primary btn-sm" href="register.html">' + esc(t('auth.signup')) + '</a>';
    } else if (me.role === 'VISITOR') {
      actions = '<a class="v-me" href="account.html"' + (page === 'account.html' ? ' aria-current="page"' : '') + ' title="' + esc(t('auth.account')) + '"><span class="avatar" aria-hidden="true">' + esc(M.initials(me.fullName)) + '</span><span>' + esc(me.fullName.split(' ')[0]) + '</span></a>' +
        '<button type="button" class="icon-btn" data-logout aria-label="' + esc(t('auth.logout')) + '" title="' + esc(t('auth.logout')) + '">' + icon('logout', 18) + '</button>';
    } else {
      actions = '<a class="btn btn-ghost btn-sm" href="' + M.auth.home(me.role) + '">' + esc(t('auth.dashboard')) + '</a>' +
        '<button type="button" class="icon-btn" data-logout aria-label="' + esc(t('auth.logout')) + '" title="' + esc(t('auth.logout')) + '">' + icon('logout', 18) + '</button>';
    }

    var head = document.getElementById('vHeader');
    if (head) {
      head.className = 'v-header';
      head.innerHTML = '<div class="sadu sadu-thin" aria-hidden="true"></div><div class="v-header-in">' +
        '<a class="v-logo" href="index.html" aria-label="' + esc(t('brand')) + ' — ' + esc(t('nav.home')) + '">' + logoHtml(false) + '</a>' +
        '<nav class="v-nav" aria-label="' + esc(t('nav.main')) + '">' + NAV.map(function (n) {
          return '<a href="' + n[0] + '"' + (n[0] === page ? ' aria-current="page"' : '') + '>' + esc(t(n[1])) + '</a>';
        }).join('') + '</nav>' +
        '<div class="v-actions">' + langButton() + actions + '</div></div>';
      var lo = head.querySelector('[data-logout]');
      if (lo) lo.addEventListener('click', function () { M.auth.logout(); });
      wireLogos(head);
    }

    // الفوتر للزائر فقط: ما فيه أي روابط للمنظمين أو الإدارة
    var foot = document.getElementById('vFooter');
    if (foot) {
      var li = function (href, key) { return '<li><a href="' + href + '">' + esc(t(key)) + '</a></li>'; };
      var account = !me
        ? li(M.ROOT + 'login.html', 'auth.login') + li('register.html', 'auth.signup')
        : me.role === 'VISITOR'
          ? li('account.html', 'auth.account') + li('account.html#saved', 'footer.savedPlaces') + '<li><a href="#" data-logout>' + esc(t('auth.logout')) + '</a></li>'
          : li(M.auth.home(me.role), 'auth.dashboard') + '<li><a href="#" data-logout>' + esc(t('auth.logout')) + '</a></li>';
      foot.className = 'v-footer-sa najdi-top on-dark';
      foot.innerHTML = '<div class="v-footer-in">' +
        '<div><a class="v-logo" href="index.html">' + logoHtml(true) + '</a><p>' + esc(t('tagline')) + '</p>' +
          '<p style="color:#E6CFA2;font-weight:600;margin-top:4px">' + esc(t('motto')) + '</p></div>' +
        '<div><h4>' + esc(t('footer.explore')) + '</h4><ul>' +
          li('index.html', 'nav.home') + li('map.html', 'nav.map') + li('map.html#planner', 'nav.plan') + li('events.html', 'nav.events') + '</ul></div>' +
        '<div><h4>' + esc(t('footer.help')) + '</h4><ul>' +
          li('visit.html', 'nav.visit') + li('visit.html#facilities', 'footer.facilities') + li('visit.html#faq', 'footer.faq') + '</ul></div>' +
        '<div><h4>' + esc(t('footer.account')) + '</h4><ul>' + account + '</ul></div>' +
        '</div><div class="sadu sadu-thin" aria-hidden="true" style="max-width:1280px;margin:0 auto;width:calc(100% - 48px)"></div>' +
        '<div class="v-footer-bottom"><span>' + esc(t('footer.rights')) + '</span>' +
        (CFG.showImageCredit ? '<span>' + esc(t('footer.credit')) + '</span>' : '') + '</div>';
      wireLogos(foot);
      foot.querySelectorAll('[data-logout]').forEach(function (a) { a.addEventListener('click', function (e) { e.preventDefault(); M.auth.logout(); }); });
    }

    document.querySelectorAll('[data-lang]').forEach(function (b) {
      b.addEventListener('click', function () { g.I18n.toggle(); });
    });
  }

  // أماكني المحفوظة (للزائر المسجّل فقط — تنحفظ في المتصفح لين يصير لها endpoint)
  var saved = {
    key: function () { var me = M.auth.get(); return me && me.role === 'VISITOR' ? 'mergab.saved.' + me.id : null; },
    list: function () { var k = this.key(); if (!k) return []; try { return JSON.parse(localStorage.getItem(k)) || []; } catch (e) { return []; } },
    has: function (name) { return this.list().indexOf(name) >= 0; },
    toggle: function (name) {
      var k = this.key(); if (!k) return false;
      var l = this.list(), i = l.indexOf(name);
      if (i >= 0) l.splice(i, 1); else l.push(name);
      try { localStorage.setItem(k, JSON.stringify(l)); } catch (e) {}
      return i < 0;
    }
  };

  /* ---------- أوقات الصلاة (GET /prayer/today) ----------
     تظهر بس للزائر اللي فعّل "عرض أوقات الصلاة" (showPrayerTimes) */
  var ctxPromise = null;
  function prayerOn() { return M.auth.prayerOn(); }
  function demoQuery() {
    var q = new URLSearchParams(location.search);
    return q.get('simulateTime') ? 'simulateTime=' + encodeURIComponent(q.get('simulateTime')) : '';
  }
  function prayer() {
    if (!ctxPromise) { var dq = demoQuery(); ctxPromise = M.api.get('/prayer/today' + (dq ? '?' + dq : '')); }
    return ctxPromise;
  }
  function prayerName(k) { return t('pr.' + k); }
  function untilText(next) {
    var h = Math.floor(next.minutesLeft / 60), m = next.minutesLeft % 60;
    return h ? t('ctx.nextH', { p: prayerName(next.key), h: h, m: m }) : t('ctx.next', { p: prayerName(next.key), m: m });
  }
  function quietestPrayerZone(c) {
    return (c.prayerZones || []).slice().sort(function (a, b) { return a.initialVisitors / a.capacity - b.initialVisitors / b.capacity; })[0];
  }
  // شريط صغير: الصلاة القادمة + أهدأ مصلى
  function prayerBar(el) {
    if (!el) return;
    if (!prayerOn()) { el.innerHTML = ''; el.hidden = true; return; }
    prayer().then(function (c) {
      if (!c || !c.prayers) return;
      var pz = quietestPrayerZone(c);
      var pr = c.currentPrayer ? t('ctx.now', { p: prayerName(c.currentPrayer) }) : untilText(c.nextPrayer);
      el.hidden = false;
      el.innerHTML =
        '<div class="ctx-bar">' +
          '<div class="ctx-item"><span class="ic">' + icon('pray', 22) + '</span><span><b>' + esc(pr) + '</b>' +
            '<small>' + esc(c.currentPrayer ? '' : t('ctx.at', { t: c.nextPrayer.time })) + (pz ? (c.currentPrayer ? '' : ' · ') + esc(t('ctx.nearest', { z: M.zoneName(pz) })) : '') + '</small></span>' +
            '<a class="v-link" href="' + M.ROOT + 'visitor/visit.html#prayer">' + esc(t('ctx.allTimes')) + '</a>' +
            (pz ? '<a class="btn btn-ghost btn-sm" href="' + M.ROOT + 'visitor/map.html?zone=' + encodeURIComponent(pz.name) + '">' + icon('map', 16) + esc(t('ctx.toMap')) + '</a>' : '') + '</div>' +
          (c.source !== 'live' || c.simulated ? '<span class="ctx-src">' + esc(c.simulated ? 'Demo' : t('ctx.sample')) + '</span>' : '') +
        '</div>';
    });
  }

  g.Visitor = {
    prayer: prayer, prayerBar: prayerBar, prayerOn: prayerOn, demoQuery: demoQuery, prayerName: prayerName, untilText: untilText, quietestPrayerZone: quietestPrayerZone,
    shell: shell, icon: icon, next: next, zoneStyle: zoneStyle, zoneArt: zoneArt, zoneImage: zoneImage,
    logoHtml: logoHtml, wireLogos: wireLogos, langButton: langButton, saved: saved
  };
  document.addEventListener('DOMContentLoaded', shell);
})(window);
