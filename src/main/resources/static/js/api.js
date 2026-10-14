/* =========================================================
   Mergab — api.js
   الاتصال بالباك إند + تسجيل الدخول والصلاحيات + أدوات مشتركة

   ⚙️ MODE:
     'mock' → باك إند وهمي داخل المتصفح (mock-server.js) — الوضع الحالي
     'live' → Spring Boot الحقيقي (httpBasic: الإيميل + كلمة المرور في كل طلب)
   ========================================================= */
(function (g) {
  'use strict';

  var MODE = g.MERGAB_MODE || 'mock';
  var BASE = g.MERGAB_API_BASE != null
    ? g.MERGAB_API_BASE
    : (/^https?:$/.test(location.protocol) && location.port === '8080' ? '' : 'http://localhost:8080');
  var PREFIX = '/api/v1';
  var isAr = (document.documentElement.lang || 'en').indexOf('ar') === 0;

  // مسار جذر الموقع (عشان نوجّه لـ login.html من أي مجلد)
  var ROOT = (function () {
    var s = document.currentScript && document.currentScript.getAttribute('src');
    return s ? s.replace(/assets\/js\/api\.js.*$/, '') : '';
  })();

  /* =========================================================
     الجلسة والصلاحيات
     ========================================================= */
  var SESSION_KEY = 'mergab.session';
  var HOME = { VISITOR: 'visitor/index.html', ORGANIZER: 'organizer/dashboard.html', ADMIN: 'admin/index.html' };

  var auth = {
    // { id, fullName, email, role, token }
    get: function () { try { return JSON.parse(sessionStorage.getItem(SESSION_KEY)); } catch (e) { return null; } },
    set: function (s) { try { sessionStorage.setItem(SESSION_KEY, JSON.stringify(s)); } catch (e) {} },
    clear: function () { try { sessionStorage.removeItem(SESSION_KEY); } catch (e) {} },
    user: function () { return this.get(); },
    role: function () { var s = this.get(); return s ? s.role : null; },

    // تسجيل الدخول: يرجّع المستخدم ودوره من الباك إند
    login: function (email, password) {
      var self = this;
      if (MODE === 'mock') {
        return g.MockServer.login(email, password).then(function (u) {
          var s = { id: u.id, fullName: u.fullName, email: u.email, role: u.role, showPrayerTimes: !!(u.visitor && u.visitor.showPrayerTimes) };
          self.set(s); return s;
        });
      }
      // live: Spring Security httpBasic → GET /api/v1/user/me
      var token = 'Basic ' + btoa(unescape(encodeURIComponent(email + ':' + password)));
      return fetch(BASE + PREFIX + '/user/me', { headers: { Authorization: token } }).then(function (res) {
        if (res.status === 401) throw { status: 401, message: isAr ? 'الإيميل أو كلمة المرور غير صحيحة' : 'Invalid email or password' };
        if (!res.ok) throw { status: res.status, message: 'HTTP ' + res.status };
        return res.json();
      }).then(function (u) {
        var s = { id: u.id, fullName: u.fullName, email: u.email, role: u.role, token: token, showPrayerTimes: !!(u.visitor && u.visitor.showPrayerTimes) };
        self.set(s); return s;
      });
    },

    logout: function () {
      this.clear();
      location.href = ROOT + 'login.html';
    },

    // الزائر اختار يشوف أوقات الصلاة؟ (حقل showPrayerTimes في Visitor)
    prayerOn: function () { var s = this.get(); return !!(s && s.role === 'VISITOR' && s.showPrayerTimes); },

    home: function (role) { return ROOT + (HOME[role] || 'visitor/index.html'); },

    // حماية الصفحة: roles = ['ORGANIZER'] مثلًا
    require: function (roles) {
      var s = this.get();
      if (!s) {
        location.replace(ROOT + 'login.html?next=' + encodeURIComponent(location.pathname + location.search));
        return null;
      }
      if (roles && roles.indexOf(s.role) < 0) {
        location.replace(ROOT + 'forbidden.html');
        return null;
      }
      return s;
    }
  };

  /* =========================================================
     الطلبات
     ========================================================= */
  function request(method, path, body) {
    var session = auth.get();
    var p;
    if (MODE === 'mock') {
      p = g.MockServer.handle(method, path, body, session);
    } else {
      var opts = { method: method, headers: {} };
      if (session && session.token) opts.headers.Authorization = session.token;
      if (body !== undefined) { opts.headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(body); }
      p = fetch(BASE + PREFIX + path, opts).then(function (res) {
        return res.text().then(function (text) {
          var data = null;
          try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
          if (!res.ok) throw { status: res.status, message: (data && (data.message || data.error)) || ('HTTP ' + res.status) };
          return data;
        });
      }, function () {
        throw { status: 0, message: isAr ? 'تعذر الاتصال بالخادم' : 'Cannot reach the server' };
      });
    }
    return p.catch(function (err) {
      // انتهت الجلسة → نرجع لصفحة الدخول
      if (err.status === 401 && session) { auth.clear(); location.replace(ROOT + 'login.html?expired=1'); }
      if (err.status === 403) err.message = isAr ? 'ليس لديك صلاحية لهذا الإجراء' : 'You don\'t have permission to do this';
      throw err;
    });
  }

  function get(path) {
    return request('GET', path).catch(function (err) { toast(err.message, 'error'); return []; });
  }
  function write(method, path, body) {
    return request(method, path, body).then(function (data) {
      if (data && data.message) toast(data.message, 'success');
      return data;
    }, function (err) { toast(err.message, 'error'); throw err; });
  }
  var api = {
    get: get,
    post: function (p, b) { return write('POST', p, b === undefined ? {} : b); },
    put: function (p, b) { return write('PUT', p, b === undefined ? {} : b); },
    del: function (p) { return write('DELETE', p); },
    raw: request,
    // صورة من الباك إند (مثل QR بصيغة PNG) → رابط نحطه في <img src>
    image: function (path) {
      if (MODE === 'mock') return request('GET', path).then(function (d) { return d && d.__image; });
      var session = auth.get(), headers = {};
      if (session && session.token) headers.Authorization = session.token;
      return fetch(BASE + PREFIX + path, { headers: headers }).then(function (res) {
        if (!res.ok) throw { status: res.status, message: 'HTTP ' + res.status };
        return res.blob();
      }).then(function (b) { return URL.createObjectURL(b); });
    }
  };

  /* ---------- رسائل ---------- */
  var toastBox = null;
  function toast(msg, type) {
    if (!toastBox) {
      toastBox = document.createElement('div');
      toastBox.className = 'm-toasts';
      toastBox.setAttribute('role', 'status');
      toastBox.setAttribute('aria-live', 'polite');
      document.body.appendChild(toastBox);
    }
    var t = document.createElement('div');
    t.className = 'm-toast m-toast-' + (type || 'info');
    t.textContent = msg;
    toastBox.appendChild(t);
    setTimeout(function () { t.classList.add('m-toast-out'); }, 3600);
    setTimeout(function () { t.remove(); }, 4000);
  }

  // شارة صغيرة توضح إن الموقع على بيانات تجريبية
  if (MODE === 'mock') {
    document.addEventListener('DOMContentLoaded', function () {
      var b = document.createElement('div');
      b.className = 'm-offline';
      b.textContent = isAr ? 'وضع تجريبي — غير مربوط بالباك إند' : 'Demo mode — not connected to the backend';
      document.body.appendChild(b);
    });
  }

  /* ---------- أدوات ---------- */
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function num(n) { return n == null || isNaN(n) ? '—' : Number(n).toLocaleString('en-US'); }
  function toDate(v) {
    if (!v) return null;
    if (Array.isArray(v)) return new Date(v[0], v[1] - 1, v[2] || 1, v[3] || 0, v[4] || 0);
    var d = new Date(v);
    return isNaN(d) ? null : d;
  }
  function fmtDate(v) {
    var d = toDate(v);
    return d ? d.toLocaleDateString(isAr ? 'ar-SA-u-ca-gregory-nu-latn' : 'en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—';
  }
  function fmtTime(v) {
    var d = toDate(v);
    return d ? d.toLocaleTimeString(isAr ? 'ar-SA-u-nu-latn' : 'en-US', { hour: 'numeric', minute: '2-digit' }) : '—';
  }
  function fmtDateTime(v) { var d = toDate(v); return d ? fmtDate(d) + ' · ' + fmtTime(d) : '—'; }
  function inputDateTime(v) {
    var d = toDate(v);
    if (!d) return '';
    var p = function (n) { return String(n).padStart(2, '0'); };
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + 'T' + p(d.getHours()) + ':' + p(d.getMinutes());
  }
  function inputDate(v) { return inputDateTime(v).slice(0, 10); }
  function isToday(v) { var d = toDate(v), n = new Date(); return d && d.toDateString() === n.toDateString(); }

  var LEVELS = [
    { key: 'critical', min: 0.9,  color: '#C0392B', ink: '#FFFFFF', soft: '#F7E3E0', text: { ar: 'مكتظ',  en: 'Critical' } },
    { key: 'high',     min: 0.75, color: '#E8964A', ink: '#14211C', soft: '#FBEBDB', text: { ar: 'مزدحم', en: 'Busy' } },
    { key: 'medium',   min: 0.5,  color: '#F2D27A', ink: '#14211C', soft: '#FBF3DA', text: { ar: 'متوسط', en: 'Moderate' } },
    { key: 'low',      min: 0,    color: '#7FC49B', ink: '#14211C', soft: '#E3F2E9', text: { ar: 'هادئ',  en: 'Quiet' } }
  ];
  function level(count, capacity) {
    var r = capacity ? count / capacity : 0;
    for (var i = 0; i < LEVELS.length; i++) {
      if (r >= LEVELS[i].min) return Object.assign({ ratio: r, percent: Math.round(r * 100), label: LEVELS[i].text[isAr ? 'ar' : 'en'] }, LEVELS[i]);
    }
  }

  var ZONE_AR = {
    'north gate': 'البوابة الشمالية', 'main plaza': 'الساحة الرئيسية', 'innovation hub': 'منطقة الابتكار',
    'culture zone': 'المنطقة الثقافية', 'food court': 'منطقة الطعام', 'exhibition hall': 'قاعة المعارض',
    'family zone': 'المنطقة العائلية', 'south gate': 'البوابة الجنوبية'
  };
  function zoneName(z) {
    var n = z && (z.name || z);
    if (!isAr) return n || '—';
    return ZONE_AR[String(n || '').trim().toLowerCase()] || n || '—';
  }
  function initials(name) {
    return String(name || '?').trim().split(/\s+/).slice(0, 2).map(function (p) { return p[0]; }).join('').toUpperCase();
  }
  var ROLE_STYLE = {
    ADMIN: { bg: '#EEE9FB', ink: '#5B3FB0', ar: 'أدمن', en: 'Admin' },
    ORGANIZER: { bg: '#FBF0DD', ink: '#8A5A12', ar: 'منظم', en: 'Organizer' },
    VISITOR: { bg: '#E3F2E9', ink: '#1F6B45', ar: 'زائر', en: 'Visitor' }
  };
  function roleBadge(role) {
    var s = ROLE_STYLE[role] || { bg: '#EEE', ink: '#333', ar: role, en: role };
    return '<span class="badge" style="background:' + s.bg + ';color:' + s.ink + '">' + esc(isAr ? s.ar : s.en) + '</span>';
  }

  /* ---------- الأيقونات: Google Material Symbols (Rounded) ----------
     الخط ينحمّل من Google Fonts في <head> كل صفحة.
     icon('mosque', 20) أو بأسمائنا القديمة icon('pray', 20) */
  var ICONS = {
    // مناطق
    plaza: 'festival', gate: 'door_front', bulb: 'lightbulb', culture: 'museum', food: 'restaurant',
    hall: 'storefront', family: 'family_restroom', pin: 'location_on',
    // عام
    map: 'map', calendar: 'calendar_month', route: 'route', spark: 'auto_awesome', clock: 'schedule',
    search: 'search', arrowStart: 'chevron_left', arrowEnd: 'chevron_right', walk: 'directions_walk',
    nav: 'near_me', globe: 'language', logout: 'logout', heart: 'favorite', pray: 'mosque', wc: 'wc',
    aid: 'medical_services', water: 'water_drop', charge: 'battery_charging_full', info: 'info',
    wheel: 'accessible', car: 'local_parking', ticket: 'confirmation_number', bus: 'directions_bus',
    users: 'groups', qr: 'qr_code_2', download: 'download', expand: 'fullscreen', scan: 'qr_code_scanner',
    lock: 'lock', bell: 'notifications', check: 'check_circle', warning: 'warning', trend: 'trending_up',
    // لوحة التحكم
    dashboard: 'space_dashboard', zones: 'location_on', connections: 'hub', events: 'event',
    simulations: 'monitoring', reports: 'bar_chart', plus: 'add', edit: 'edit', trash: 'delete',
    close: 'close', shield: 'admin_panel_settings', chart: 'insights', eye: 'visibility', person: 'person',
    organizer: 'badge', visitor: 'person_pin', home: 'home', settings: 'tune', alarm: 'notifications_active'
  };
  function icon(name, size, color, fill) {
    var n = ICONS[name] || name || 'location_on';
    return '<span class="icon msr' + (fill ? ' fill' : '') + '" aria-hidden="true" style="font-size:' + (size || 20) + 'px' +
      (color ? ';color:' + color : '') + '">' + n + '</span>';
  }

  g.Mergab = {
    icon: icon, ICONS: ICONS,
    MODE: MODE, BASE: BASE, ROOT: ROOT, api: api, auth: auth, toast: toast, esc: esc, num: num,
    toDate: toDate, fmtDate: fmtDate, fmtTime: fmtTime, fmtDateTime: fmtDateTime,
    inputDateTime: inputDateTime, inputDate: inputDate, isToday: isToday,
    LEVELS: LEVELS, level: level, zoneName: zoneName, initials: initials, roleBadge: roleBadge, isAr: isAr
  };
})(window);
