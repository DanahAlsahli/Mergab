/* =========================================================
   Mergab — organizer.js
   هيدر وفوتر لوحة المنظم والأدمن (نفس تصميم موقع الزائر)
   + أداة CRUD مشتركة (جدول + نموذج + حذف) لكل الصفحات
   ========================================================= */
(function (g) {
  'use strict';
  var M = g.Mergab, esc = M.esc, t = g.t;

  function icon(name, size, color, fill) { return M.icon(name, size, color, fill); }

  // كل منطقة (area) لها قائمتها والدور المسموح له يدخلها
  var AREAS = {
    organizer: {
      roles: ['ORGANIZER'],
      home: 'dashboard.html',
      badge: 'o.badge.organizer',
      nav: [
        ['dashboard', 'o.nav.dashboard', 'dashboard.html'],
        ['zones', 'o.nav.zones', 'zones.html'],
        ['connections', 'o.nav.connections', 'connections.html'],
        ['events', 'o.nav.events', 'events.html'],
        ['simulations', 'o.nav.simulations', 'simulations.html'],
        ['reports', 'o.nav.reports', 'reports.html']
      ]
    },
    admin: {
      roles: ['ADMIN'],
      home: 'index.html',
      badge: 'o.badge.admin',
      nav: [
        ['dashboard', 'o.nav.overview', 'index.html'],
        ['users', 'o.nav.users', 'users.html']
      ]
    }
  };

  function logo(light) {
    var cfg = g.MERGAB_CONFIG || {};
    var src = M.ROOT + (light ? (cfg.logoLight || 'assets/img/logo-white.png') : (cfg.logo || 'assets/img/logo.png'));
    return '<img class="logo-img" src="' + esc(src) + '" alt="' + esc(t('brand')) + '">';
  }

  /* ---------- الهيدر + الفوتر ---------- */
  function shell() {
    var areaKey = document.body.dataset.area || 'organizer';
    var area = AREAS[areaKey];
    // حماية الصفحة: لو مو داخل → صفحة الدخول، لو دوره غلط → 403
    var user = M.auth.require(area.roles);
    if (!user) return null;
    var page = document.body.dataset.page;

    var head = document.getElementById('vHeader');
    head.className = 'v-header o-header';
    head.innerHTML = '<div class="sadu sadu-thin" aria-hidden="true"></div><div class="v-header-in">' +
      '<a class="v-logo" href="' + area.home + '" aria-label="' + esc(t('brand')) + '">' + logo(false) + '</a>' +
      '<span class="o-badge">' + icon(areaKey === 'admin' ? 'shield' : 'organizer', 16) + esc(t(area.badge)) + '</span>' +
      '<nav class="v-nav" aria-label="' + esc(t('nav.main')) + '">' + area.nav.map(function (n) {
        return '<a href="' + n[2] + '"' + (n[0] === page ? ' aria-current="page"' : '') + '>' + icon(n[0], 18) + '<span>' + esc(t(n[1])) + '</span></a>';
      }).join('') + '</nav>' +
      '<div class="v-actions">' +
        '<a class="icon-btn" href="' + M.ROOT + 'visitor/index.html" title="' + esc(t('o.visitorSite')) + '" aria-label="' + esc(t('o.visitorSite')) + '">' + icon('open_in_new', 20) + '</a>' +
        '<button type="button" class="lang-btn" data-lang lang="' + (g.I18n.isAr ? 'en' : 'ar') + '">' + icon('globe', 16) + esc(t('lang.switch')) + '</button>' +
        '<span class="v-me" title="' + esc(user.email) + '"><span class="avatar" aria-hidden="true">' + esc(M.initials(user.fullName)) + '</span><span>' + esc(user.fullName.split(' ')[0]) + '</span></span>' +
        '<button type="button" class="icon-btn" data-logout aria-label="' + esc(t('auth.logout')) + '" title="' + esc(t('auth.logout')) + '">' + icon('logout', 18) + '</button>' +
      '</div></div>';

    var foot = document.getElementById('vFooter');
    if (foot) {
      foot.className = 'v-footer-sa najdi-top on-dark o-footer';
      foot.innerHTML = '<div class="v-footer-in o-footer-in">' +
        '<div><a class="v-logo" href="' + area.home + '">' + logo(true) + '</a><p style="color:#E6CFA2;font-weight:600;margin-top:6px">' + esc(t('motto')) + '</p></div>' +
        '<div><h4>' + esc(t(area.badge)) + '</h4><ul>' + area.nav.map(function (n) {
          return '<li><a href="' + n[2] + '">' + esc(t(n[1])) + '</a></li>';
        }).join('') + '</ul></div>' +
        '<div><h4>' + esc(t('o.footer.more')) + '</h4><ul>' +
          '<li><a href="' + M.ROOT + 'visitor/index.html">' + esc(t('o.visitorSite')) + '</a></li>' +
          '<li><a href="#" data-logout>' + esc(t('auth.logout')) + '</a></li></ul></div>' +
        '</div><div class="v-footer-bottom"><span>' + esc(t('footer.rights')) + '</span></div>';
    }

    document.querySelectorAll('[data-logout]').forEach(function (b) {
      b.addEventListener('click', function (e) { e.preventDefault(); M.auth.logout(); });
    });
    document.querySelectorAll('[data-lang]').forEach(function (b) { b.addEventListener('click', function () { g.I18n.toggle(); }); });
    document.querySelectorAll('[data-icon]').forEach(function (el) { el.innerHTML = icon(el.dataset.icon, Number(el.dataset.size) || 24); });
    return user;
  }

  /* ---------- نافذة تأكيد ---------- */
  function confirmBox(message) {
    return new Promise(function (resolve) {
      var d = document.createElement('dialog');
      d.className = 'modal';
      d.innerHTML = '<div class="modal-body" style="padding-top:24px"><h2 style="font-size:18px;margin-bottom:8px">' + esc(t('o.sure')) + '</h2><p style="margin:0;color:var(--muted)">' + esc(message) + '</p></div>' +
        '<div class="modal-foot"><button type="button" class="btn btn-ghost" value="no">' + esc(t('o.cancel')) + '</button><button type="button" class="btn btn-danger" value="yes">' + esc(t('o.delete')) + '</button></div>';
      document.body.appendChild(d);
      d.addEventListener('click', function (e) {
        var v = e.target.closest('button') && e.target.closest('button').value;
        if (v) { d.close(); resolve(v === 'yes'); }
      });
      d.addEventListener('close', function () { setTimeout(function () { d.remove(); }, 0); resolve(false); });
      d.showModal();
    });
  }

  /* =========================================================
     Org.mount — جدول + بحث + نموذج إضافة/تعديل + حذف
     ========================================================= */
  function mount(o) {
    var host = typeof o.el === 'string' ? document.querySelector(o.el) : o.el;
    var rows = [], query = '';
    var uid = 'c' + Math.random().toString(36).slice(2, 7);

    host.classList.add('card', 'o-table-card');
    host.innerHTML =
      '<div class="o-table-head">' +
        '<h2>' + esc(o.title) + ' <span class="badge o-count" data-count>0</span></h2>' +
        '<label class="o-search">' + icon('search', 18) + '<span class="sr-only">' + esc(t('o.search')) + '</span>' +
          '<input type="search" data-q placeholder="' + esc(t('o.search')) + '"></label>' +
        (o.create ? '<button type="button" class="btn btn-primary btn-sm" data-add>' + icon('plus', 18) + esc(o.addLabel || t('o.add')) + '</button>' : '') +
      '</div>' +
      '<div class="table-wrap"><table class="table"><thead><tr>' +
      o.columns.map(function (c) { return '<th scope="col">' + esc(c.label) + '</th>'; }).join('') +
      '<th scope="col"><span class="sr-only">' + esc(t('o.actions')) + '</span></th></tr></thead><tbody data-body></tbody></table></div>';

    var body = host.querySelector('[data-body]');
    var countEl = host.querySelector('[data-count]');

    var dlg = document.createElement('dialog');
    dlg.className = 'modal';
    dlg.setAttribute('aria-labelledby', uid + '-t');
    document.body.appendChild(dlg);

    function render() {
      var shown = rows.filter(function (r) {
        if (!query) return true;
        var s = o.searchText ? o.searchText(r) : JSON.stringify(r);
        return String(s).toLowerCase().indexOf(query) >= 0;
      });
      countEl.textContent = rows.length;
      if (!shown.length) {
        body.innerHTML = '<tr><td colspan="' + (o.columns.length + 1) + '"><div class="empty">' + esc(rows.length ? t('o.noResults') : (o.emptyText || t('o.empty'))) + '</div></td></tr>';
        return;
      }
      body.innerHTML = shown.map(function (r) {
        return '<tr data-id="' + esc(r.id) + '">' + o.columns.map(function (c) { return '<td>' + c.render(r) + '</td>'; }).join('') +
          '<td><div class="actions">' +
          (o.extraActions ? o.extraActions(r) : '') +
          (o.update ? '<button type="button" class="icon-btn" data-edit aria-label="' + esc(t('o.edit')) + '" title="' + esc(t('o.edit')) + '">' + icon('edit', 18) + '</button>' : '') +
          (o.remove ? '<button type="button" class="icon-btn danger" data-del aria-label="' + esc(t('o.delete')) + '" title="' + esc(t('o.delete')) + '">' + icon('trash', 18) + '</button>' : '') +
          '</div></td></tr>';
      }).join('');
    }

    function load() {
      body.innerHTML = '<tr><td colspan="' + (o.columns.length + 1) + '"><div class="empty">' + esc(t('loading')) + '</div></td></tr>';
      return Promise.resolve(o.load()).then(function (data) {
        rows = Array.isArray(data) ? data : [];
        if (o.sort) rows.sort(o.sort);
        render();
        if (o.afterLoad) o.afterLoad(rows);
        return rows;
      });
    }

    function openForm(row) {
      var editing = !!row;
      var fields = o.fields.filter(function (f) { return editing ? !f.createOnly : !f.editOnly; });
      Promise.all(fields.map(function (f) { return f.options ? Promise.resolve(f.options(row)) : null; })).then(function (opts) {
        dlg.innerHTML =
          '<form method="dialog" novalidate>' +
          '<div class="modal-head"><h2 id="' + uid + '-t">' + esc(editing ? (o.editTitle || t('o.edit')) : (o.addTitle || o.addLabel || t('o.add'))) + '</h2>' +
          '<button type="button" class="icon-btn" data-close aria-label="' + esc(t('card.close')) + '">' + icon('close', 18) + '</button></div>' +
          '<div class="modal-body"><div class="form-grid">' +
          fields.map(function (f, i) {
            var id = uid + '-' + f.name;
            var val = editing ? (f.value ? f.value(row) : row[f.name]) : (f.default != null ? (typeof f.default === 'function' ? f.default() : f.default) : '');
            if (f.type === 'datetime-local' && val) val = M.inputDateTime(val);
            if (f.type === 'date' && val) val = M.inputDate(val);
            var attrs = ' id="' + id + '" name="' + f.name + '"' + (f.required ? ' required' : '') +
              (f.min != null ? ' min="' + f.min + '"' : '') + (f.max != null ? ' max="' + (typeof f.max === 'function' ? f.max() : f.max) + '"' : '') +
              (f.step != null ? ' step="' + f.step + '"' : '') + (f.placeholder ? ' placeholder="' + esc(f.placeholder) + '"' : '') +
              (f.dir ? ' dir="' + f.dir + '"' : '');
            if (f.type === 'checkbox') {
              return '<label class="opt-card' + (f.full ? ' full' : '') + '" for="' + id + '"><input type="checkbox" class="switch"' + attrs + (val ? ' checked' : '') + '>' +
                (f.icon ? '<span class="ic">' + icon(f.icon, 22) + '</span>' : '') +
                '<span class="txt"><b>' + esc(f.label) + '</b>' + (f.help ? '<small>' + esc(f.help) + '</small>' : '') + '</span></label>';
            }
            var input;
            if (f.type === 'select') {
              input = '<select' + attrs + '><option value="">' + esc(t('o.select')) + '</option>' + (opts[i] || []).map(function (op) {
                return '<option value="' + esc(op.value) + '"' + (String(op.value) === String(val) ? ' selected' : '') + '>' + esc(op.label) + '</option>';
              }).join('') + '</select>';
            } else {
              input = '<input type="' + (f.type || 'text') + '"' + attrs + ' value="' + esc(val == null ? '' : val) + '">';
            }
            return '<label class="field' + (f.full ? ' full' : '') + '" for="' + id + '"><span>' + esc(f.label) + '</span>' + input + (f.help ? '<small>' + esc(f.help) + '</small>' : '') + '</label>';
          }).join('') +
          '</div></div>' +
          '<div class="modal-foot"><button type="button" class="btn btn-ghost" data-close>' + esc(t('o.cancel')) + '</button>' +
          '<button type="submit" class="btn btn-primary">' + esc(editing ? t('o.save') : t('o.create')) + '</button></div></form>';

        var form = dlg.querySelector('form');
        dlg.querySelectorAll('[data-close]').forEach(function (b) { b.addEventListener('click', function () { dlg.close(); }); });
        form.addEventListener('submit', function (e) {
          e.preventDefault();
          if (!form.checkValidity()) { form.reportValidity(); return; }
          var data = {};
          fields.forEach(function (f) {
            var el = form.elements[f.name], v = el.value;
            if (f.type === 'checkbox') v = el.checked;
            else if (f.type === 'number') v = v === '' ? null : Number(v);
            else if (f.type === 'datetime-local') v = v ? (v.length === 16 ? v + ':00' : v) : null;
            else if (v === '') v = null;
            data[f.name] = v;
          });
          var btn = form.querySelector('[type=submit]');
          btn.disabled = true;
          Promise.resolve(editing ? o.update(row, data) : o.create(data))
            .then(function () { dlg.close(); return load(); })
            .catch(function () { /* الرسالة طلعت في toast */ })
            .then(function () { btn.disabled = false; });
        });
        dlg.showModal();
        var first = form.querySelector('input:not([type=checkbox]),select');
        if (first) first.focus();
      });
    }

    host.addEventListener('click', function (e) {
      if (e.target.closest('[data-add]')) { openForm(null); return; }
      var tr = e.target.closest('tr[data-id]');
      if (!tr) return;
      var row = rows.filter(function (r) { return String(r.id) === tr.dataset.id; })[0];
      if (!row) return;
      if (e.target.closest('[data-edit]')) openForm(row);
      else if (e.target.closest('[data-del]')) {
        confirmBox(o.deleteText ? o.deleteText(row) : t('o.deleteText')).then(function (ok) {
          if (ok) Promise.resolve(o.remove(row)).then(load).catch(function () {});
        });
      } else if (o.onAction) {
        var act = e.target.closest('[data-action]');
        if (act) o.onAction(act.dataset.action, row);
      }
    });
    host.querySelector('[data-q]').addEventListener('input', function (e) { query = e.target.value.toLowerCase().trim(); render(); });

    load();
    return { reload: load, rows: function () { return rows; }, open: openForm };
  }

  // خيارات قائمة المناطق (تُستخدم في أكثر من صفحة)
  function zoneOptions() {
    return M.api.get('/zone/get').then(function (zs) {
      return zs.map(function (z) { return { value: z.id, label: M.zoneName(z) }; });
    });
  }
  function status(s) { return '<span class="badge badge-dot st-' + esc(s) + '">' + esc(t('st.' + s)) + '</span>'; }
  function zoneCell(z) {
    if (!z) return '—';
    var ar = M.zoneName(z);
    return '<b>' + esc(ar) + '</b>' + (ar !== z.name ? '<small class="o-sub" dir="ltr">' + esc(z.name) + '</small>' : '');
  }

  g.Org = { shell: shell, mount: mount, icon: icon, confirm: confirmBox, zoneOptions: zoneOptions, status: status, zoneCell: zoneCell };
})(window);
