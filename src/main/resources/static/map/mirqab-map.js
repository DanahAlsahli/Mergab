/* =========================================================
   Mirqab Map — mirqab-map.js
   خريطة تفاعلية بـ HTML/CSS/JS فقط (بدون أي مكتبة، تشتغل بدون نت)
   وضعين:
     mode: '3d'     → مجسّم 3D يلف ويتحرك
     mode: 'image'  → صورة خريطة (أي صورة) وفوقها علامات المناطق

   الاستخدام:
     const map = MirqabMap.create('#mirqab-map', { apiUrl: '...', lang: 'ar' });

   التفاصيل كاملة في README.md
   ========================================================= */
(function (global) {
  'use strict';

  /* ---------- مستويات الازدحام ---------- */
  var LEVELS = [
    { key: 'critical', min: 0.9,  top: '#C0392B', side: '#8E2A20', ink: '#FFFFFF', text: { ar: 'مكتظ',  en: 'Critical' } },
    { key: 'high',     min: 0.75, top: '#E8964A', side: '#B5692B', ink: '#14211C', text: { ar: 'مزدحم', en: 'Busy' } },
    { key: 'medium',   min: 0.5,  top: '#F2D27A', side: '#BF9C45', ink: '#14211C', text: { ar: 'متوسط', en: 'Moderate' } },
    { key: 'low',      min: 0,    top: '#7FC49B', side: '#4F8F69', ink: '#14211C', text: { ar: 'هادئ',  en: 'Quiet' } }
  ];

  var TEXT = {
    ar: { you: 'أنت هنا', heat: 'عرض الازدحام', rotL: 'تدوير لليسار', rotR: 'تدوير لليمين', zoomIn: 'تكبير', zoomOut: 'تصغير', reset: 'إعادة ضبط العرض', north: 'ش', noImage: 'ما لقيت صورة الخريطة: ' },
    en: { you: 'You are here', heat: 'Crowd view', rotL: 'Rotate left', rotR: 'Rotate right', zoomIn: 'Zoom in', zoomOut: 'Zoom out', reset: 'Reset view', north: 'N', noImage: 'Map image not found: ' }
  };

  var WALK_M_PER_MIN = 70;     // سرعة المشي (متر/دقيقة)

  /* =========================================================
     تخطيط وضع 3D (الأرض 400×400 وحدة)
     - zones: المناطق. الربط مع الداتابيس يكون بالاسم (name.en أو name.ar أو aliases)
       type 'box' = مبنى (x,y = الزاوية العلوية، w عرض، d عمق، h ارتفاع)
       type 'dome' = ساحة بقبة (cx,cy مركز، r نصف قطر، h ارتفاع)
       door = نقطة الممر اللي يدخل منها المسار
     - nodes: نقاط الممرات
     - edges: الممرات. العنصر الثالث (اختياري) = المنطقة اللي يمر فيها الممر،
       فإذا كانت مزدحمة المسار الذكي يتجنبه
     ========================================================= */
  var DEFAULT_LAYOUT = {
    size: 400,
    metersPerUnit: 1.5,
    zones: [
      { key: 'northGate',      name: { ar: 'البوابة الشمالية', en: 'North Gate' },      type: 'box',  x: 165, y: 22,  w: 70, d: 36,  h: 14, color: '#E6EEE9', door: 'NG' },
      { key: 'mainPlaza',      name: { ar: 'الساحة الرئيسية',  en: 'Main Plaza' },      type: 'dome', cx: 200, cy: 200, r: 46, h: 44, color: '#E7C98A', door: 'C' },
      { key: 'innovationHub',  name: { ar: 'منطقة الابتكار',   en: 'Innovation Hub' },  type: 'box',  x: 295, y: 35,  w: 80, d: 105, h: 44, color: '#EDF1F5', door: 'D_IH' },
      { key: 'cultureZone',    name: { ar: 'المنطقة الثقافية', en: 'Culture Zone' },    type: 'box',  x: 25,  y: 270, w: 80, d: 105, h: 30, color: '#F3EBDC', door: 'D_CZ' },
      { key: 'foodCourt',      name: { ar: 'منطقة الطعام',     en: 'Food Court' },      type: 'box',  x: 25,  y: 40,  w: 80, d: 95,  h: 14, color: '#F5E6D6', door: 'D_FC' },
      { key: 'exhibitionHall', name: { ar: 'قاعة المعارض',     en: 'Exhibition Hall' }, type: 'box',  x: 295, y: 168, w: 80, d: 122, h: 34, color: '#F2ECE0', door: 'D_EH' },
      { key: 'familyZone',     name: { ar: 'المنطقة العائلية', en: 'Family Zone' },     type: 'box',  x: 25,  y: 160, w: 80, d: 85,  h: 20, color: '#E9F0E1', door: 'D_FZ' },
      { key: 'southGate',      name: { ar: 'البوابة الجنوبية', en: 'South Gate' },      type: 'box',  x: 165, y: 347, w: 70, d: 33,  h: 14, color: '#E6EEE9', door: 'SG' }
    ],
    nodes: {
      C: [200, 200],
      NW: [120, 120], N: [200, 120], NE: [280, 120], E: [280, 200],
      SE: [280, 280], S: [200, 280], SW: [120, 280], W: [120, 200],
      NG: [200, 58], SG: [200, 347],
      D_IH: [295, 120], D_EH: [295, 200], D_FC: [105, 120], D_FZ: [105, 200], D_CZ: [105, 280]
    },
    edges: [
      ['NW', 'N'], ['N', 'NE'], ['NE', 'E'], ['E', 'SE'], ['SE', 'S'], ['S', 'SW'], ['SW', 'W'], ['W', 'NW'],
      ['N', 'C', 'mainPlaza'], ['S', 'C', 'mainPlaza'], ['E', 'C', 'mainPlaza'], ['W', 'C', 'mainPlaza'],
      ['N', 'NG', 'northGate'], ['S', 'SG', 'southGate'],
      ['NE', 'D_IH', 'innovationHub'], ['E', 'D_EH', 'exhibitionHall'],
      ['NW', 'D_FC', 'foodCourt'], ['W', 'D_FZ', 'familyZone'], ['SW', 'D_CZ', 'cultureZone']
    ],
    areas: [
      { x: 127, y: 127, w: 146, d: 146, color: '#EFE6D3', radius: 6 },
      { x: 295, y: 305, w: 80,  d: 70,  color: '#C9DFB6', radius: 12 }
    ],
    trees: [
      [308, 318], [330, 318], [352, 318], [319, 340], [342, 340], [364, 340], [308, 362], [330, 362], [352, 362],
      [142, 142], [258, 142], [142, 258], [258, 258], [150, 90], [250, 90], [150, 320], [250, 320]
    ]
  };

  /* =========================================================
     تخطيط وضع الصورة
     - src: رابط الصورة
     - zones: لكل منطقة x و y = مكانها على الصورة بالنسبة المئوية (0 → 100)
       (شغّلي debug: true واضغطي على الصورة، يطبع لك الإحداثيات في الـ Console)
     - edges (اختياري): أي منطقة توصل لأي منطقة. لو ما حددتيها، كل منطقة
       تتوصل بأقرب منطقتين لها وبالـ hub
     - hub: المنطقة الوسطية اللي كل الطرق تمر فيها
     ========================================================= */
  var DEFAULT_IMAGE_LAYOUT = {
    src: 'sample-map.jpg',
    hub: 'mainPlaza',
    metersPerUnit: 8,
    zones: [
      { key: 'northGate',      name: { ar: 'البوابة الشمالية', en: 'North Gate' },      x: 41.6, y: 24.7 },
      { key: 'mainPlaza',      name: { ar: 'الساحة الرئيسية',  en: 'Main Plaza' },      x: 50.0, y: 40.5 },
      { key: 'innovationHub',  name: { ar: 'منطقة الابتكار',   en: 'Innovation Hub' },  x: 63.8, y: 18.6 },
      { key: 'cultureZone',    name: { ar: 'المنطقة الثقافية', en: 'Culture Zone' },    x: 34.8, y: 72.2 },
      { key: 'foodCourt',      name: { ar: 'منطقة الطعام',     en: 'Food Court' },      x: 23.6, y: 38.1 },
      { key: 'exhibitionHall', name: { ar: 'قاعة المعارض',     en: 'Exhibition Hall' }, x: 72.8, y: 40.2 },
      { key: 'familyZone',     name: { ar: 'المنطقة العائلية', en: 'Family Zone' },     x: 28.7, y: 54.0 },
      { key: 'southGate',      name: { ar: 'البوابة الجنوبية', en: 'South Gate' },      x: 60.1, y: 73.9 }
    ],
    edges: null
  };

  // بيانات تجريبية تُستخدم لو ما فيه apiUrl أو الـ API ما رد
  var DEMO_COUNTS = {
    northGate: [4000, 2600], mainPlaza: [10000, 9100], innovationHub: [8000, 6560], cultureZone: [6000, 2820],
    foodCourt: [7000, 2100], exhibitionHall: [12000, 7800], familyZone: [5000, 1250], southGate: [4000, 1400]
  };

  /* ---------- أدوات صغيرة ---------- */
  var SVGNS = 'http://www.w3.org/2000/svg';
  function px(n) { return n + 'px'; }
  function css(el, s) { if (s) { for (var k in s) { el.style[k] = s[k]; } } return el; }
  function div(cls, s, parent) {
    var d = document.createElement('div');
    if (cls) d.className = cls;
    css(d, s);
    if (parent) parent.appendChild(d);
    return d;
  }
  function rgb(hex) { var n = parseInt(hex.slice(1), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255]; }
  function shade(hex, f) { var c = rgb(hex); return 'rgb(' + c.map(function (v) { return Math.min(255, Math.round(v * f)); }).join(',') + ')'; }
  function alpha(hex, a) { return 'rgba(' + rgb(hex).join(',') + ',' + a + ')'; }
  function levelOf(r) { for (var i = 0; i < LEVELS.length; i++) { if (r >= LEVELS[i].min) return LEVELS[i]; } return LEVELS[LEVELS.length - 1]; }
  function getPath(obj, path) { return String(path).split('.').reduce(function (o, k) { return o == null ? undefined : o[k]; }, obj); }
  function pick(obj, f) {
    if (Array.isArray(f)) { for (var i = 0; i < f.length; i++) { var v = getPath(obj, f[i]); if (v != null) return v; } return undefined; }
    return getPath(obj, f);
  }
  function setPath(obj, path, val) {
    var parts = String(Array.isArray(path) ? path[0] : path).split('.'), cur = obj;
    for (var i = 0; i < parts.length - 1; i++) { cur = cur[parts[i]] = cur[parts[i]] || {}; }
    cur[parts[parts.length - 1]] = val;
  }
  function norm(s) { return String(s == null ? '' : s).trim().toLowerCase().replace(/\s+/g, ' '); }
  function dist(a, b) { var dx = b[0] - a[0], dy = b[1] - a[1]; return Math.sqrt(dx * dx + dy * dy); }

  // شريط مسطح بين نقطتين (ممر أو مسار) — وضع 3D
  function seg(parent, a, b, w, cls, z) {
    var len = dist(a, b), ang = Math.atan2(b[1] - a[1], b[0] - a[0]) * 180 / Math.PI;
    return div('mqm-p ' + cls, {
      left: px(a[0] - w / 2), top: px(a[1] - w / 2), width: px(len + w), height: px(w),
      borderRadius: px(w / 2), transformOrigin: px(w / 2) + ' 50%',
      transform: 'translateZ(' + z + 'px) rotate(' + ang + 'deg)'
    }, parent);
  }

  // Dijkstra عام: adj = { node: [{ to, e }] }، costFn(link, fromNode) = تكلفة الانتقال
  function dijkstra(adj, names, s, t, costFn) {
    var best = {}, prev = {}, done = {};
    best[s] = 0;
    while (true) {
      var u = null, min = Infinity;
      names.forEach(function (n) { if (!done[n] && best[n] != null && best[n] < min) { min = best[n]; u = n; } });
      if (u === null || u === t) break;
      done[u] = true;
      (adj[u] || []).forEach(function (a) {
        var nd = best[u] + costFn(a, u);
        if (best[a.to] == null || nd < best[a.to]) { best[a.to] = nd; prev[a.to] = { n: u, link: a }; }
      });
    }
    if (best[t] == null) return null;
    var nodes = [t], links = [], cur = t;
    while (cur !== s) { var p = prev[cur]; links.unshift({ link: p.link, from: p.n }); cur = p.n; nodes.unshift(cur); }
    return { nodes: nodes, links: links };
  }

  var ICONS = {
    rotL: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M4 12a8 8 0 1 0 2.4-5.7"/><path d="M4 4v4h4"/></svg>',
    rotR: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M20 12a8 8 0 1 1-2.4-5.7"/><path d="M20 4v4h-4"/></svg>',
    zoomIn: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
    zoomOut: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M5 12h14"/></svg>',
    target: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="3"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>',
    heat: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="9" cy="8" r="3"/><circle cx="17" cy="9" r="2.5"/><path d="M3 20c.6-3 3-5 6-5s5.4 2 6 5M14.5 15.5c2.6-.6 5.4.8 6.5 4.5"/></svg>'
  };

  /* =========================================================
     create
     ========================================================= */
  function create(target, options) {
    var root = typeof target === 'string' ? document.querySelector(target) : target;
    if (!root) throw new Error('MirqabMap: container not found: ' + target);

    options = options || {};
    var o = {
      mode: '3d',
      lang: 'ar',
      apiUrl: null,
      fetchOptions: {},
      refreshMs: 30000,
      matchBy: 'name',
      fields: {},
      zoneIds: {},
      start: 'southGate',
      heat: true,
      tilt: 55,
      rotation: -25,
      controls: true,
      demo: true,
      debug: false,
      imageFit: 'contain',
      onSelect: null,
      onUpdate: null,
      onError: null,
      layout: DEFAULT_LAYOUT,
      imageLayout: DEFAULT_IMAGE_LAYOUT
    };
    for (var k in options) { o[k] = options[k]; }
    o.fields = Object.assign({
      id: 'id',
      name: 'name',
      capacity: 'capacity',
      count: ['currentCount', 'currentVisitors', 'occupancy', 'initialVisitors']
    }, options.fields || {});

    var IMG = o.mode === 'image';
    var T = TEXT[o.lang] || TEXT.ar;
    var L = IMG ? o.imageLayout : o.layout;
    var MPU = L.metersPerUnit || (IMG ? 8 : 1.5);

    var zones = L.zones.map(function (z) {
      var c = Object.assign({}, z);
      if (o.zoneIds[z.key] != null) c.id = o.zoneIds[z.key];
      return c;
    });
    var byKey = {};
    zones.forEach(function (z) { byKey[z.key] = z; });

    var state = {
      rot: o.rotation, zoom: 1, fit: 1, pan: [0, 0], heat: o.heat, selected: null,
      data: {}, loaded: false, justDragged: false, timer: null,
      start: o.start, route: null, routeEls: [], imgRatio: 16 / 9
    };
    var refs = {};

    root.classList.add('mqm');
    root.classList.toggle('mqm-imode', IMG);
    root.setAttribute('dir', o.lang === 'ar' ? 'rtl' : 'ltr');
    root.innerHTML = '';

    /* =====================================================
       وضع 3D
       ===================================================== */
    var scene = null;

    function build3d() {
      scene = div('mqm-scene', { width: px(L.size), height: px(L.size) }, root);
      div('mqm-p mqm-ground', { left: '0', top: '0', width: px(L.size), height: px(L.size) }, scene);

      (L.areas || []).forEach(function (a) {
        div('mqm-p', {
          left: px(a.x), top: px(a.y), width: px(a.w), height: px(a.d),
          background: a.color, borderRadius: px(a.radius || 0), transform: 'translateZ(0.2px)'
        }, scene);
      });

      L.edges.forEach(function (e) { seg(scene, L.nodes[e[0]], L.nodes[e[1]], 14, 'mqm-walk', 0.4); });

      zones.forEach(function (z) {
        var r = refs[z.key] = { walls: [], discs: [] };
        var cx, cy, lz, gr;

        if (z.type === 'dome') {
          cx = z.cx; cy = z.cy; lz = z.h + 8; gr = z.r + 20;
          r.floor = div('mqm-p mqm-face mqm-plaza', {
            left: px(z.cx - z.r), top: px(z.cy - z.r), width: px(2 * z.r), height: px(2 * z.r),
            transform: 'translateZ(0.8px)'
          }, scene);
          r.floor.dataset.zone = z.key;
          var R = z.r * 0.82, n = 10;
          for (var i = 0; i <= n; i++) {
            var t = i / n, rad = Math.max(3, R * Math.sqrt(Math.max(0, 1 - t * t)));
            r.discs.push(div('mqm-p mqm-dome', {
              left: px(z.cx - rad), top: px(z.cy - rad), width: px(2 * rad), height: px(2 * rad),
              transform: 'translateZ(' + (1 + z.h * t) + 'px)'
            }, scene));
          }
        } else {
          cx = z.x + z.w / 2; cy = z.y + z.d / 2; lz = z.h + 6; gr = Math.max(z.w, z.d) / 2 + 22;
          r.top = div('mqm-p mqm-face mqm-top', {
            left: px(z.x), top: px(z.y), width: px(z.w), height: px(z.d),
            transform: 'translateZ(' + z.h + 'px)'
          }, scene);
          r.top.dataset.zone = z.key;
          // الجدران الأربعة: شمال، جنوب، غرب، شرق
          [
            { f: 0.82, c: '', s: { left: px(z.x), top: px(z.y), width: px(z.w), height: px(z.h), transformOrigin: '50% 0', transform: 'rotateX(90deg)' } },
            { f: 0.94, c: '', s: { left: px(z.x), top: px(z.y + z.d), width: px(z.w), height: px(z.h), transformOrigin: '50% 0', transform: 'rotateX(90deg)' } },
            { f: 0.76, c: ' mqm-wall-x', s: { left: px(z.x), top: px(z.y), width: px(z.h), height: px(z.d), transformOrigin: '0 50%', transform: 'rotateY(-90deg)' } },
            { f: 0.88, c: ' mqm-wall-x', s: { left: px(z.x + z.w), top: px(z.y), width: px(z.h), height: px(z.d), transformOrigin: '0 50%', transform: 'rotateY(-90deg)' } }
          ].forEach(function (w) {
            var el = div('mqm-p mqm-face mqm-wall' + w.c, w.s, scene);
            el._f = w.f;
            el.dataset.zone = z.key;
            r.walls.push(el);
          });
        }

        r.glow = div('mqm-p mqm-glow', {
          left: px(cx - gr), top: px(cy - gr), width: px(2 * gr), height: px(2 * gr),
          transform: 'translateZ(0.6px)', display: 'none'
        }, scene);

        r.label = makeLabel(z, 'mqm-label');
        r.label.style.left = px(cx);
        r.label.style.top = px(cy);
        r.label.style.setProperty('--z', lz + 'px');
        scene.appendChild(r.label);
      });

      (L.trees || []).forEach(function (t) {
        div('mqm-p', { left: px(t[0] - 1.5), top: px(t[1] - 1.5), width: '3px', height: '3px', background: '#7A5C3A', borderRadius: '50%', transform: 'translateZ(3px)' }, scene);
        [[7, 6], [5.5, 9], [3.5, 12]].forEach(function (c, i) {
          div('mqm-p', {
            left: px(t[0] - c[0]), top: px(t[1] - c[0]), width: px(2 * c[0]), height: px(2 * c[0]),
            borderRadius: '50%', background: i === 2 ? '#86BC6E' : '#5E9450', transform: 'translateZ(' + c[1] + 'px)'
          }, scene);
        });
      });

      state.meDot = div('mqm-p mqm-me', { width: '14px', height: '14px', transform: 'translateZ(2.5px)' }, scene);
      state.meLabel = div('mqm-billboard mqm-me-label', null, scene);
      state.meLabel.textContent = T.you;
      state.meLabel.style.setProperty('--z', '40px');   // مرفوع فوق أسماء المناطق عشان يبان دايم
    }

    function placeMe3d() {
      var z = byKey[state.start];
      if (!z) return;
      var d = L.nodes[z.door], p = d;
      // نحرك النقطة شوي على الممر عشان ما يغطيها جدار المبنى
      var e = L.edges.filter(function (ed) { return ed[0] === z.door || ed[1] === z.door; })[0];
      if (e) {
        var other = L.nodes[e[0] === z.door ? e[1] : e[0]], len = dist(d, other) || 1, step = Math.min(34, len / 2);
        p = [d[0] + (other[0] - d[0]) / len * step, d[1] + (other[1] - d[1]) / len * step];
      }
      css(state.meDot, { left: px(p[0] - 7), top: px(p[1] - 7) });
      css(state.meLabel, { left: px(p[0]), top: px(p[1]) });
    }

    function paint3d(z, r, lv, useHeat) {
      var base = useHeat ? lv.top : z.color;
      if (z.type === 'dome') {
        r.floor.style.backgroundColor = useHeat ? alpha(lv.top, 0.55) : '#F3EDE0';
        r.discs.forEach(function (d) {
          d.style.borderColor = useHeat ? lv.side : '#C8892B';
          d.style.backgroundColor = alpha(useHeat ? lv.top : '#E7C98A', 0.16);
        });
      } else {
        r.top.style.backgroundColor = base;
        r.walls.forEach(function (w) { w.style.backgroundColor = shade(base, w._f); });
      }
      var hot = useHeat && (lv.key === 'critical' || lv.key === 'high');
      r.glow.style.display = hot ? 'block' : 'none';
      if (hot) r.glow.style.background = 'radial-gradient(circle, ' + alpha(lv.top, 0.45) + ', ' + alpha(lv.top, 0) + ' 70%)';
      (r.top || r.floor).classList.toggle('mqm-selected', state.selected === z.key);
    }

    /* =====================================================
       وضع الصورة
       ===================================================== */
    var stage = null, img = null, svg = null, errBox = null;

    function buildImage() {
      stage = div('mqm-istage', null, root);
      img = document.createElement('img');
      img.className = 'mqm-img';
      img.alt = o.imageAlt || '';
      img.draggable = false;
      stage.appendChild(img);

      svg = document.createElementNS(SVGNS, 'svg');
      svg.setAttribute('class', 'mqm-isvg');
      svg.setAttribute('viewBox', '0 0 100 100');
      svg.setAttribute('preserveAspectRatio', 'none');
      svg.setAttribute('aria-hidden', 'true');
      stage.appendChild(svg);

      zones.forEach(function (z) {
        var r = refs[z.key] = {};
        r.ring = div('mqm-ring', { left: z.x + '%', top: z.y + '%' }, stage);
        r.label = makeLabel(z, 'mqm-marker');
        r.label.style.left = z.x + '%';
        r.label.style.top = z.y + '%';
        stage.appendChild(r.label);
      });

      state.meDot = div('mqm-ime', null, stage);
      state.meLabel = div('mqm-ime-label', null, stage);
      state.meLabel.textContent = T.you;

      img.addEventListener('load', function () {
        if (img.naturalWidth && img.naturalHeight) state.imgRatio = img.naturalWidth / img.naturalHeight;
        if (errBox) { errBox.remove(); errBox = null; }
        fit();
      });
      img.addEventListener('error', function () {
        if (!errBox) errBox = div('mqm-imgerr', null, root);
        errBox.textContent = T.noImage + (L.src || '');
      });
      img.src = L.src;
    }

    function placeMeImage() {
      var z = byKey[state.start];
      if (!z) return;
      css(state.meDot, { left: z.x + '%', top: z.y + '%' });
      css(state.meLabel, { left: z.x + '%', top: z.y + '%' });
    }

    function paintImage(z, r, lv, useHeat) {
      r.label.style.setProperty('--bg', useHeat ? lv.top : '#FFFFFF');
      r.label.style.setProperty('--ink', useHeat ? lv.ink : '#14211C');
      r.label.style.setProperty('--bd', useHeat ? lv.side : '#E2DCCF');
      r.dot.style.background = useHeat ? lv.ink : (lv ? lv.top : '#9AA5A0');
      r.ring.style.display = useHeat && lv.key === 'critical' ? 'block' : 'none';
      if (lv) r.ring.style.setProperty('--c', alpha(lv.top, 0.55));
    }

    // روابط المناطق في وضع الصورة
    function imageEdges() {
      if (Array.isArray(L.edges)) return L.edges;
      var out = [], seen = {};
      function add(a, b) { var id = [a, b].sort().join('|'); if (a !== b && !seen[id]) { seen[id] = 1; out.push([a, b]); } }
      zones.forEach(function (z) {
        if (L.hub && byKey[L.hub]) add(z.key, L.hub);
        zones.filter(function (x) { return x.key !== z.key && x.key !== L.hub; })
          .sort(function (a, b) { return ipDist(z, a) - ipDist(z, b); })
          .slice(0, 2)
          .forEach(function (x) { add(z.key, x.key); });
      });
      return out;
    }
    function ipt(z) { return [z.x * state.imgRatio, z.y]; }
    function ipDist(a, b) { return dist(ipt(a), ipt(b)); }

    /* =====================================================
       مشترك
       ===================================================== */
    function makeLabel(z, cls) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = cls;
      b.dataset.zone = z.key;
      var r = refs[z.key];
      r.dot = document.createElement('span');
      r.dot.className = 'mqm-dot';
      r.dot.setAttribute('aria-hidden', 'true');
      r.text = document.createElement('span');
      r.pct = document.createElement('span');
      r.pct.setAttribute('dir', 'ltr');   // عشان تطلع 82% مو %82 في العربي
      b.appendChild(r.dot);
      b.appendChild(r.text);
      b.appendChild(r.pct);
      return b;
    }

    function placeMe() { if (IMG) placeMeImage(); else placeMe3d(); }

    /* ---------- أزرار التحكم ---------- */
    var heatBtn = null;
    if (o.controls) {
      var ctr = div('mqm-controls', null, root);
      var html = '';
      if (!IMG) {
        html += '<button type="button" class="mqm-btn" data-act="rotL" aria-label="' + T.rotL + '">' + ICONS.rotL + '</button>' +
                '<button type="button" class="mqm-btn" data-act="rotR" aria-label="' + T.rotR + '">' + ICONS.rotR + '</button>';
      }
      html += '<button type="button" class="mqm-btn" data-act="zoomIn" aria-label="' + T.zoomIn + '">' + ICONS.zoomIn + '</button>' +
              '<button type="button" class="mqm-btn" data-act="zoomOut" aria-label="' + T.zoomOut + '">' + ICONS.zoomOut + '</button>';
      html += IMG
        ? '<button type="button" class="mqm-btn" data-act="reset" aria-label="' + T.reset + '">' + ICONS.target + '</button>'
        : '<button type="button" class="mqm-btn" data-act="reset" aria-label="' + T.reset + '">' +
            '<span class="mqm-compass" aria-hidden="true"><svg width="12" height="9" viewBox="0 0 14 10"><path d="M7 0l7 10H0z" fill="#C0392B"/></svg>' + T.north + '</span></button>';
      ctr.innerHTML = html;

      heatBtn = document.createElement('button');
      heatBtn.type = 'button';
      heatBtn.className = 'mqm-heat';
      heatBtn.dataset.act = 'heat';
      heatBtn.innerHTML = ICONS.heat + '<span>' + T.heat + '</span>';
      root.appendChild(heatBtn);

      var lg = div('mqm-legend', null, root);
      lg.innerHTML = LEVELS.slice().reverse().map(function (l) {
        return '<span><i style="background:' + l.top + '"></i>' + l.text[o.lang] + '</span>';
      }).join('');
    }

    var actions = {
      rotL: function () { rotate(-45); },
      rotR: function () { rotate(45); },
      zoomIn: function () { zoom(1.25); },
      zoomOut: function () { zoom(1 / 1.25); },
      reset: function () { resetView(); },
      heat: function () { setHeat(!state.heat); }
    };

    /* ---------- العرض ---------- */
    function apply() {
      if (heatBtn) heatBtn.setAttribute('aria-pressed', String(state.heat));
      if (IMG) {
        root.style.setProperty('--mqm-inv', String(1 / state.zoom));
        stage.style.transform = 'translate(-50%, -50%) translate(' + state.pan[0] + 'px, ' + state.pan[1] + 'px) scale(' + state.zoom + ')';
        return;
      }
      root.style.setProperty('--mqm-rot', state.rot + 'deg');
      root.style.setProperty('--mqm-tilt', o.tilt + 'deg');
      root.style.setProperty('--mqm-scale', String(state.fit * state.zoom));
    }
    function fit() {
      var w = root.clientWidth, h = root.clientHeight;
      if (!w || !h) return;
      if (IMG) {
        var r = state.imgRatio, cover = o.imageFit === 'cover';
        var useH = cover ? (w / h < r) : (w / h > r);
        var sw = useH ? h * r : w, sh = useH ? h : w / r;
        stage.style.width = px(sw);
        stage.style.height = px(sh);
      } else {
        state.fit = Math.max(0.3, Math.min(w / (L.size * 1.2), h / (L.size * 0.95)));
      }
      apply();
    }
    function rotate(deg) { if (IMG) return; state.rot += deg; apply(); }
    function zoom(f) {
      state.zoom = Math.min(IMG ? 4 : 2.5, Math.max(IMG ? 1 : 0.5, state.zoom * f));
      if (IMG && state.zoom === 1) state.pan = [0, 0];
      apply();
    }
    function resetView() { state.rot = o.rotation; state.zoom = 1; state.pan = [0, 0]; apply(); }

    /* ---------- البيانات ---------- */
    function info(key) {
      var z = byKey[key];
      if (!z) return null;
      var d = state.data[key];
      var ratio = d && d.capacity ? d.count / d.capacity : null;
      var lv = ratio == null ? null : levelOf(ratio);
      return {
        key: key,
        id: d && d.dbId != null ? d.dbId : (z.id != null ? z.id : null),
        name: z.name[o.lang] || z.name.ar,
        dbName: d ? d.dbName : null,
        capacity: d ? d.capacity : null,
        count: d ? d.count : null,
        ratio: ratio,
        percent: ratio == null ? null : Math.round(ratio * 100),
        level: lv ? lv.key : null,
        levelText: lv ? lv.text[o.lang] : '',
        color: lv ? lv.top : null,
        waitMinutes: ratio == null ? null : Math.max(0, Math.round((ratio - 0.3) * 45))
      };
    }
    function getZones() { return zones.map(function (z) { return info(z.key); }); }

    function paint() {
      zones.forEach(function (z) {
        var r = refs[z.key], inf = info(z.key);
        var lv = inf.ratio == null ? null : levelOf(inf.ratio);
        var useHeat = !!(state.heat && lv);

        if (IMG) paintImage(z, r, lv, useHeat); else paint3d(z, r, lv, useHeat);

        if (!IMG) r.dot.style.background = lv ? lv.top : '#9AA5A0';
        var showPct = state.heat && inf.percent != null;
        r.text.textContent = inf.name + (showPct ? ' · ' : '');
        r.pct.textContent = showPct ? inf.percent + '%' : '';
        r.label.setAttribute('aria-label', inf.name + (inf.levelText ? ' — ' + inf.levelText : ''));

        var sel = state.selected === z.key;
        r.label.classList.toggle('mqm-active', sel);
        r.label.setAttribute('aria-pressed', String(sel));
      });
    }

    // يلقى المنطقة اللي تطابق عنصر جاي من الـ API
    function findZone(item) {
      if (o.matchBy === 'id') {
        var id = pick(item, o.fields.id);
        return zones.filter(function (z) { return z.id != null && String(z.id) === String(id); })[0];
      }
      var n = norm(pick(item, o.fields.name));
      if (!n) return null;
      return zones.filter(function (z) {
        return [z.key, z.name.ar, z.name.en].concat(z.aliases || []).some(function (a) { return norm(a) === n; });
      })[0];
    }

    function ingest(list) {
      if (list && !Array.isArray(list)) list = list.data || list.content || list.zones || [];
      var unmatched = [];
      (list || []).forEach(function (item) {
        var z = findZone(item);
        if (!z) { unmatched.push(pick(item, o.fields.name) || pick(item, o.fields.id)); return; }
        state.data[z.key] = {
          dbId: pick(item, o.fields.id),
          dbName: pick(item, o.fields.name),
          capacity: Number(pick(item, o.fields.capacity)) || 0,
          count: Number(pick(item, o.fields.count)) || 0
        };
      });
      if (unmatched.length) console.info('[MirqabMap] مناطق ما لها مكان في الخريطة (ضيفيها في layout أو aliases):', unmatched);
      state.loaded = true;
      paint();
      if (state.route) showRoute(state.route.to, state.route.opts);
      var all = getZones();
      if (o.onUpdate) o.onUpdate(all);
      root.dispatchEvent(new CustomEvent('mqm:update', { detail: all }));
      if (state.selected && o.onSelect) o.onSelect(info(state.selected));
    }

    function demoList() {
      return zones.map(function (z, i) {
        var c = DEMO_COUNTS[z.key] || [5000, 1000 + ((i * 1777) % 4000)];
        var item = {};
        setPath(item, o.fields.id, z.id != null ? z.id : i + 1);
        setPath(item, o.fields.name, z.name.en);
        setPath(item, o.fields.capacity, c[0]);
        setPath(item, o.fields.count, c[1]);
        return item;
      });
    }

    function load() {
      if (!o.apiUrl) {
        if (o.demo && !state.loaded) {
          var saved = o.matchBy; o.matchBy = 'name';
          ingest(demoList());
          o.matchBy = saved;
        }
        return Promise.resolve();
      }
      return fetch(o.apiUrl, o.fetchOptions)
        .then(function (res) { if (!res.ok) throw new Error('HTTP ' + res.status); return res.json(); })
        .then(ingest)
        .catch(function (err) {
          console.warn('[MirqabMap] تعذر جلب البيانات:', err);
          if (o.onError) o.onError(err);
          if (o.demo && !state.loaded) {
            var saved = o.matchBy; o.matchBy = 'name';
            ingest(demoList());
            o.matchBy = saved;
          }
        });
    }

    /* ---------- الاختيار ---------- */
    function resolve(k) {
      if (k == null) return null;
      if (byKey[k]) return k;
      var n = norm(k);
      var z = zones.filter(function (zz) {
        var d = state.data[zz.key];
        return (d && d.dbId != null && String(d.dbId) === String(k)) ||
               (zz.id != null && String(zz.id) === String(k)) ||
               norm(zz.name.ar) === n || norm(zz.name.en) === n ||
               (d && norm(d.dbName) === n);
      })[0];
      return z ? z.key : null;
    }
    function select(k) {
      var key = resolve(k);
      if (!key) return null;
      state.selected = key;
      paint();
      var inf = info(key);
      if (o.onSelect) o.onSelect(inf);
      root.dispatchEvent(new CustomEvent('mqm:select', { detail: inf }));
      return inf;
    }

    /* ---------- المسار الذكي (Dijkstra) ---------- */
    function crowdFactor(zoneKey) {
      var d = state.data[zoneKey];
      var r = d && d.capacity ? d.count / d.capacity : 0;
      return r >= 0.9 ? 4 : r >= 0.75 ? 2.5 : r >= 0.5 ? 1.3 : 1;
    }

    var graph = { adj: {}, names: [], len: null, zoneOf: null, point: null };
    function buildGraph() {
      graph.adj = {};
      function link(a, b, e) {
        (graph.adj[a] = graph.adj[a] || []).push({ to: b, e: e });
        (graph.adj[b] = graph.adj[b] || []).push({ to: a, e: e });
      }
      if (IMG) {
        graph.names = zones.map(function (z) { return z.key; });
        imageEdges().forEach(function (e) { if (byKey[e[0]] && byKey[e[1]]) link(e[0], e[1], e); });
        graph.len = function (e) { return ipDist(byKey[e[0]], byKey[e[1]]); };
        // في وضع الصورة: الدخول لمنطقة مزدحمة (مو الوجهة) يرفع التكلفة
        graph.zoneOf = function (a) { return a.to; };
        graph.point = function (n) { return [byKey[n].x, byKey[n].y]; };
      } else {
        graph.names = Object.keys(L.nodes);
        L.edges.forEach(function (e) { link(e[0], e[1], e); });
        graph.len = function (e) { return dist(L.nodes[e[0]], L.nodes[e[1]]); };
        graph.zoneOf = function (a) { return a.e[2]; };
        graph.point = function (n) { return L.nodes[n]; };
      }
    }
    function nodeOf(key) { return IMG ? key : byKey[key].door; }

    function getRoute(toK, opts) {
      opts = opts || {};
      var to = resolve(toK), from = resolve(opts.from != null ? opts.from : state.start);
      if (!to || !from) return null;
      if (!graph.len) buildGraph();
      var s = nodeOf(from), t = nodeOf(to);

      function crowdCost(a) {
        var zk = graph.zoneOf(a), l = graph.len(a.e);
        if (!zk || zk === from || zk === to) return l;
        return l * crowdFactor(zk);
      }
      function plainCost(a) { return graph.len(a.e); }

      var best = dijkstra(graph.adj, graph.names, s, t, crowdCost);
      var direct = dijkstra(graph.adj, graph.names, s, t, plainCost);
      if (!best) return null;

      function summary(p) {
        var len = 0, cost = 0, passes = [];
        p.links.forEach(function (x) {
          len += graph.len(x.link.e);
          cost += crowdCost(x.link);
          var zk = graph.zoneOf(x.link);
          if (zk && zk !== from && zk !== to && passes.indexOf(zk) < 0) passes.push(zk);
        });
        return {
          nodes: p.nodes,
          points: p.nodes.map(graph.point),
          meters: Math.round(len * MPU),
          minutes: Math.max(1, Math.round(cost * MPU / WALK_M_PER_MIN)),
          passes: passes.map(function (k) { return info(k).name; })
        };
      }
      var b = summary(best), d = summary(direct);
      var same = b.nodes.join() === d.nodes.join();
      return {
        from: info(from),
        to: info(to),
        best: b,
        direct: same ? null : d,
        avoided: same ? [] : d.passes.filter(function (n) { return b.passes.indexOf(n) < 0; }),
        savedMinutes: same ? 0 : Math.max(0, d.minutes - b.minutes)
      };
    }

    function clearRoute() {
      state.routeEls.forEach(function (el) { el.remove(); });
      state.routeEls = [];
      state.route = null;
    }

    function drawPath(points, kind) {
      if (IMG) {
        var d = points.map(function (p, i) { return (i ? 'L' : 'M') + p[0] + ' ' + p[1]; }).join(' ');
        if (kind === 'best') state.routeEls.push(svgPath(d, 'mqm-ipath-casing'));
        state.routeEls.push(svgPath(d, kind === 'best' ? 'mqm-ipath' : 'mqm-ipath-alt'));
        return;
      }
      for (var i = 1; i < points.length; i++) {
        state.routeEls.push(kind === 'best'
          ? seg(scene, points[i - 1], points[i], 6, 'mqm-route', 2)
          : seg(scene, points[i - 1], points[i], 4, 'mqm-route-alt', 1.6));
      }
    }
    function svgPath(d, cls) {
      var p = document.createElementNS(SVGNS, 'path');
      p.setAttribute('d', d);
      p.setAttribute('class', cls);
      p.setAttribute('vector-effect', 'non-scaling-stroke');
      svg.appendChild(p);
      return p;
    }

    function showRoute(toK, opts) {
      opts = opts || {};
      var r = getRoute(toK, opts);
      clearRoute();
      if (!r) return null;
      if (r.direct && opts.compare !== false) drawPath(r.direct.points, 'alt');
      drawPath(r.best.points, 'best');
      state.route = { to: toK, opts: opts };
      return r;
    }

    /* ---------- التفاعل ---------- */
    function onClick(e) {
      var act = e.target.closest('[data-act]');
      if (act && root.contains(act)) { actions[act.dataset.act](); return; }
      if (state.justDragged) return;
      var z = e.target.closest('[data-zone]');
      if (z && root.contains(z)) { select(z.dataset.zone); return; }
      // وضع المعايرة: اضغطي على الصورة ويطبع لك مكان النقطة بالنسبة المئوية
      if (IMG && o.debug && e.target === img) {
        var b = stage.getBoundingClientRect();
        var x = +((e.clientX - b.left) / b.width * 100).toFixed(1);
        var y = +((e.clientY - b.top) / b.height * 100).toFixed(1);
        console.log('[MirqabMap] x: ' + x + ', y: ' + y);
        var m = div('mqm-debug-dot', { left: x + '%', top: y + '%' }, stage);
        m.textContent = x + ', ' + y;
      }
    }

    function onDown(e) {
      if (e.button !== 0 || e.target.closest('.mqm-controls, .mqm-heat, .mqm-legend')) return;
      var x0 = e.clientX, y0 = e.clientY, r0 = state.rot, p0 = state.pan.slice(), moved = false;
      function move(ev) {
        var dx = ev.clientX - x0, dy = ev.clientY - y0;
        if (!moved && Math.abs(dx) + Math.abs(dy) > 5) { moved = true; root.classList.add('mqm-dragging'); }
        if (!moved) return;
        if (IMG) state.pan = [p0[0] + dx, p0[1] + dy];
        else state.rot = r0 + dx * 0.4;
        apply();
      }
      function up() {
        window.removeEventListener('pointermove', move);
        window.removeEventListener('pointerup', up);
        window.removeEventListener('pointercancel', up);
        if (moved) {
          root.classList.remove('mqm-dragging');
          state.justDragged = true;
          setTimeout(function () { state.justDragged = false; }, 0);
        }
      }
      window.addEventListener('pointermove', move);
      window.addEventListener('pointerup', up);
      window.addEventListener('pointercancel', up);
    }

    root.addEventListener('click', onClick);
    root.addEventListener('pointerdown', onDown);

    var ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(fit) : null;
    if (ro) ro.observe(root); else window.addEventListener('resize', fit);

    function setHeat(on) { state.heat = !!on; apply(); paint(); }
    function setStart(k) {
      var key = resolve(k);
      if (!key) return;
      state.start = key;
      placeMe();
      if (state.route) showRoute(state.route.to, state.route.opts);
    }

    function destroy() {
      if (state.timer) clearInterval(state.timer);
      if (ro) ro.disconnect(); else window.removeEventListener('resize', fit);
      root.removeEventListener('click', onClick);
      root.removeEventListener('pointerdown', onDown);
      root.innerHTML = '';
      root.classList.remove('mqm', 'mqm-imode');
    }

    /* ---------- التشغيل ---------- */
    if (IMG) buildImage(); else build3d();
    state.start = resolve(state.start) ||
        (zones.length ? zones[0].key : null);
    placeMe();
    buildGraph();
    fit();
    apply();
    paint();
    load();
    if (o.apiUrl && o.refreshMs) state.timer = setInterval(load, o.refreshMs);

    return {
      select: select,
      getZones: getZones,
      getZone: function (k) { var key = resolve(k); return key ? info(key) : null; },
      setOccupancy: ingest,
      refresh: load,
      getRoute: getRoute,
      showRoute: showRoute,
      clearRoute: clearRoute,
      setStart: setStart,
      setHeat: setHeat,
      rotate: rotate,
      zoom: zoom,
      resetView: resetView,
      destroy: destroy
    };
  }

  global.MirqabMap = {
    create: create,
    LEVELS: LEVELS,
    DEFAULT_LAYOUT: DEFAULT_LAYOUT,
    DEFAULT_IMAGE_LAYOUT: DEFAULT_IMAGE_LAYOUT
  };
})(window);
