/* =========================================================
   Mergab — mock-server.js
   باك إند وهمي داخل المتصفح (نفس الـ endpoints ونفس شكل البيانات
   ونفس قواعد الصلاحيات المقترحة لـ Spring Security).
   البيانات تنحفظ في localStorage عشان تبقى بين الصفحات.

   لما يجهز الباك إند الحقيقي: غيّري MODE في api.js إلى 'live'
   واحذفي هذا الملف من الصفحات.
   ========================================================= */
(function (g) {
  'use strict';

  var KEY = 'mergab.mockdb.v1';
  var PUBLIC = 'PUBLIC';
  var ADMIN = ['ADMIN'];
  var ORG = ['ORGANIZER'];             // الأدمن يدير الحسابات فقط
  var ANY_USER = ['VISITOR', 'ORGANIZER', 'ADMIN'];

  function at(dayOffset, h, m) {
    var d = new Date(); d.setDate(d.getDate() + dayOffset); d.setHours(h, m || 0, 0, 0);
    var p = function (n) { return String(n).padStart(2, '0'); };
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + 'T' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':00';
  }
  function dateOnly(dayOffset) { return at(dayOffset, 0).slice(0, 10); }

  function seed() {
    return {
      nextId: 100,
      users: [
        { id: 1, fullName: 'System Admin', email: 'admin@mergab.sa', password: 'admin123', role: 'ADMIN' },
        { id: 2, fullName: 'Dana Alshammari', email: 'organizer@mergab.sa', password: 'org123', role: 'ORGANIZER', phoneNumber: '0551234567' },
        { id: 3, fullName: 'Razan Almadan', email: 'razan@mergab.sa', password: 'org123', role: 'ORGANIZER', phoneNumber: '0557654321' },
        { id: 4, fullName: 'Sara Alqahtani', email: 'sara@gmail.com', password: 'visitor123', role: 'VISITOR', phoneNumber: '0501112233', qrToken: '7c9e6679-7425-40de-944b-e07fc1f90ae7', showPrayerTimes: true },
        { id: 5, fullName: 'Fahad Alotaibi', email: 'fahad@outlook.com', password: 'visitor123', role: 'VISITOR', phoneNumber: '0509998877', qrToken: '1b4e28ba-2fa1-41d2-883f-0016d3cca427', showPrayerTimes: false }
      ],
      zones: [
        { id: 1, name: 'North Gate', capacity: 4000, initialVisitors: 2600, indoor: false, hasPrayerArea: false },
        { id: 2, name: 'Main Plaza', capacity: 10000, initialVisitors: 9100, indoor: false, hasPrayerArea: true },
        { id: 3, name: 'Innovation Hub', capacity: 8000, initialVisitors: 6560, indoor: true, hasPrayerArea: false },
        { id: 4, name: 'Culture Zone', capacity: 6000, initialVisitors: 2820, indoor: true, hasPrayerArea: false },
        { id: 5, name: 'Food Court', capacity: 7000, initialVisitors: 2100, indoor: false, hasPrayerArea: false },
        { id: 6, name: 'Exhibition Hall', capacity: 12000, initialVisitors: 7800, indoor: true, hasPrayerArea: true },
        { id: 7, name: 'Family Zone', capacity: 5000, initialVisitors: 1250, indoor: false, hasPrayerArea: false },
        { id: 8, name: 'South Gate', capacity: 4000, initialVisitors: 1400, indoor: false, hasPrayerArea: false }
      ],
      connections: [
        { id: 1, maxVisitorsPerMinute: 120, from: 1, to: 2 }, { id: 2, maxVisitorsPerMinute: 90, from: 2, to: 3 },
        { id: 3, maxVisitorsPerMinute: 80, from: 2, to: 4 }, { id: 4, maxVisitorsPerMinute: 70, from: 3, to: 6 },
        { id: 5, maxVisitorsPerMinute: 60, from: 4, to: 5 }, { id: 6, maxVisitorsPerMinute: 110, from: 8, to: 2 }
      ],
      events: [
        { id: 1, name: 'Tech Innovations Talk', startDateTime: at(0, 10), endDateTime: at(0, 12), expectedDemand: 1800, zone: 3, organizer: 2 },
        { id: 2, name: 'Cultural Performance', startDateTime: at(0, 13), endDateTime: at(0, 14), expectedDemand: 1200, zone: 4, organizer: 2 },
        { id: 3, name: 'Future Expo Panel', startDateTime: at(0, 16), endDateTime: at(0, 18), expectedDemand: 2500, zone: 2, organizer: 3 },
        { id: 4, name: 'Drone Light Show', startDateTime: at(0, 21), endDateTime: at(0, 22), expectedDemand: 6000, zone: 2, organizer: 2 },
        { id: 5, name: 'Family Workshop', startDateTime: at(1, 15), endDateTime: at(1, 17), expectedDemand: 600, zone: 7, organizer: 3 },
        { id: 6, name: 'Startup Showcase', startDateTime: at(1, 11), endDateTime: at(1, 15), expectedDemand: 2200, zone: 6, organizer: 2 },
        { id: 7, name: 'Food Festival', startDateTime: at(2, 12), endDateTime: at(2, 20), expectedDemand: 3000, zone: 5, organizer: 3 }
      ],
      attendance: [
        { id: 1, expoName: 'Expo Riyadh', attendanceDate: dateOnly(-6), totalAttendance: 41000, sourceUrl: 'https://example.com/report' },
        { id: 2, expoName: 'Expo Riyadh', attendanceDate: dateOnly(-5), totalAttendance: 38500, sourceUrl: 'https://example.com/report' },
        { id: 3, expoName: 'Expo Riyadh', attendanceDate: dateOnly(-4), totalAttendance: 44200, sourceUrl: 'https://example.com/report' },
        { id: 4, expoName: 'Expo Riyadh', attendanceDate: dateOnly(-3), totalAttendance: 57800, sourceUrl: 'https://example.com/report' },
        { id: 5, expoName: 'Expo Riyadh', attendanceDate: dateOnly(-2), totalAttendance: 61300, sourceUrl: 'https://example.com/report' },
        { id: 6, expoName: 'Expo Riyadh', attendanceDate: dateOnly(-1), totalAttendance: 52900, sourceUrl: 'https://example.com/report' }
      ],
      simulations: [
        { id: 1, durationMinutes: 120, status: 'COMPLETED', randomSeed: 42, startDateTime: at(-1, 17), endDateTime: at(-1, 19), attendance: 5, organizer: 2 },
        { id: 2, durationMinutes: 90, status: 'RUNNING', randomSeed: 7, startDateTime: at(0, 18), endDateTime: at(0, 19, 30), attendance: 6, organizer: 2 },
        { id: 3, durationMinutes: 60, status: 'PENDING', randomSeed: 1234, startDateTime: at(1, 20), endDateTime: at(1, 21), attendance: 6, organizer: 3 }
      ],
      // ZoneVisit: سجل الدخول والخروج بالـ QR (checkOut = null → داخل المنطقة الحين)
      zoneVisits: [
        { id: 1, visitor: 4, zone: 8, checkIn: at(-1, 16, 10), checkOut: at(-1, 16, 25), manual: false },
        { id: 2, visitor: 4, zone: 6, checkIn: at(-1, 16, 40), checkOut: at(-1, 18, 5), manual: false },
        { id: 3, visitor: 4, zone: 5, checkIn: at(-1, 18, 20), checkOut: at(-1, 19, 0), manual: true },
        { id: 4, visitor: 4, zone: 3, checkIn: at(0, 16, 15), checkOut: at(0, 17, 5), manual: false }
      ],
      interventions: [
        { id: 1, startMinute: 15, visitorsPerMinute: 40, simulation: 1, source: 2, target: 4 },
        { id: 2, startMinute: 30, visitorsPerMinute: 25, simulation: 1, source: 3, target: 6 }
      ]
    };
  }

  var db = null;
  function load() {
    if (db) return db;
    try { db = JSON.parse(localStorage.getItem(KEY)); } catch (e) { db = null; }
    if (!db || !db.users) { db = seed(); save(); }
    migrate();
    return db;
  }
  // نسخة قديمة محفوظة في المتصفح → نضيف لها الحقول الجديدة بدون ما نمسح بيانات المستخدم
  function migrate() {
    var fresh = seed(), changed = false;
    if (!db.zoneVisits) { db.zoneVisits = fresh.zoneVisits; changed = true; }
    db.zones.forEach(function (z) {
      var f = fresh.zones.filter(function (x) { return x.name === z.name; })[0];
      if (z.indoor == null) { z.indoor = f ? f.indoor : false; z.hasPrayerArea = f ? f.hasPrayerArea : false; changed = true; }
    });
    db.users.forEach(function (u) {
      if (u.role === 'VISITOR' && !u.qrToken) {
        var f = fresh.users.filter(function (x) { return x.id === u.id; })[0];
        u.qrToken = f && f.qrToken || uuid(); changed = true;
      }
      if (u.role === 'VISITOR' && u.showPrayerTimes == null) {
        var f2 = fresh.users.filter(function (x) { return x.id === u.id; })[0];
        u.showPrayerTimes = !!(f2 && f2.showPrayerTimes); changed = true;
      }
    });
    if (changed) save();
  }
  function uuid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      var r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 3 | 8)).toString(16);
    });
  }
  function save() { try { localStorage.setItem(KEY, JSON.stringify(db)); } catch (e) {} }
  function reset() { db = seed(); save(); }

  /* ---------- شكل الـ JSON مثل Spring ---------- */
  var J = {
    zone: function (z) { return z ? { id: z.id, name: z.name, capacity: z.capacity, initialVisitors: z.initialVisitors, indoor: !!z.indoor, hasPrayerArea: !!z.hasPrayerArea } : null; },
    user: function (u) {
      var out = { id: u.id, fullName: u.fullName, email: u.email, role: u.role, organizer: null, visitor: null };
      if (u.role === 'ORGANIZER') out.organizer = { id: u.id, phoneNumber: u.phoneNumber };
      if (u.role === 'VISITOR') out.visitor = { id: u.id, phoneNumber: u.phoneNumber, showPrayerTimes: !!u.showPrayerTimes };
      return out;
    },
    event: function (e) { return { id: e.id, name: e.name, startDateTime: e.startDateTime, endDateTime: e.endDateTime, expectedDemand: e.expectedDemand, zone: J.zone(zone(e.zone)) }; },
    conn: function (c) { return { id: c.id, maxVisitorsPerMinute: c.maxVisitorsPerMinute, fromZone: J.zone(zone(c.from)), toZone: J.zone(zone(c.to)) }; },
    sim: function (s) { return { id: s.id, durationMinutes: s.durationMinutes, status: s.status, randomSeed: s.randomSeed, startDateTime: s.startDateTime, endDateTime: s.endDateTime }; },
    int: function (i) { return { id: i.id, startMinute: i.startMinute, visitorsPerMinute: i.visitorsPerMinute, sourceZone: J.zone(zone(i.source)), targetZone: J.zone(zone(i.target)) }; },
    att: function (a) { return { id: a.id, expoName: a.expoName, attendanceDate: a.attendanceDate, totalAttendance: a.totalAttendance, sourceUrl: a.sourceUrl }; }
  };
  function find(list, id) { return list.filter(function (x) { return x.id === Number(id); })[0]; }
  function zone(id) { return find(db.zones, id); }
  function nid() { db.nextId += 1; return db.nextId; }

  function Err(status, message) { this.status = status; this.message = message; }
  function bad(m) { throw new Err(400, m); }
  function need(cond, m) { if (!cond) bad(m); }
  function ok(m) { return { message: m }; }
  function req(b, fields) {
    fields.forEach(function (f) { if (b == null || b[f] == null || b[f] === '') bad(f + ' is required'); });
  }
  function checkUserDTO(b, ignoreId) {
    req(b, ['fullName', 'email', 'password']);
    need(String(b.fullName).length >= 3 && String(b.fullName).length <= 50, 'Full name must be between 3 and 50 characters');
    need(/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(b.email), 'Email must be valid');
    need(String(b.password).length >= 6, 'Password must be at least 6 characters');
    need(!db.users.some(function (u) { return u.email.toLowerCase() === String(b.email).toLowerCase() && u.id !== ignoreId; }), 'Email already exists');
  }
  function checkPhone(b, ignoreId) {
    req(b, ['phoneNumber']);
    need(/^05\d{8}$/.test(b.phoneNumber), 'Phone number must start with 05 and be 10 digits');
    need(!db.users.some(function (u) { return u.phoneNumber === b.phoneNumber && u.id !== ignoreId; }), 'Phone number already exists');
  }
  function snapshotsFor(simId) {
    var s = find(db.simulations, simId);
    if (!s || s.status !== 'COMPLETED') return [];
    var out = [], id = 1;
    db.zones.slice(0, 5).forEach(function (z, zi) {
      for (var m = 0; m <= s.durationMinutes; m += 10) {
        var wave = Math.sin(m / s.durationMinutes * Math.PI);
        var occ = Math.min(z.capacity, Math.round(z.initialVisitors * (0.78 + 0.35 * wave) * (1 + ((s.randomSeed + zi) % 5) / 40)));
        out.push({ id: id++, minute: m, occupancy: occ, queueLength: Math.max(0, Math.round((occ - z.capacity * 0.85) / 20)), zone: J.zone(z), simulation: J.sim(s) });
      }
    });
    return out;
  }

  /* =========================================================
     الـ endpoints (نفس مشروع Mergab) + الصلاحيات
     ========================================================= */
  var R = [];
  function on(method, pattern, roles, fn) {
    var keys = [];
    var re = new RegExp('^' + pattern.replace(/\{(\w+)\}/g, function (_, k) { keys.push(k); return '([^/]+)'; }) + '$');
    R.push({ method: method, re: re, keys: keys, roles: roles, fn: fn });
  }

  // حساب
  on('GET', '/user/me', ANY_USER, function (p, b, me) { return J.user(find(db.users, me.id)); });

  // المستخدمين (أدمن)
  on('GET', '/user/get-all', ADMIN, function () { return db.users.map(J.user); });
  on('GET', '/user/get/{id}', ADMIN, function (p) { var u = find(db.users, p.id); if (!u) throw new Err(400, 'User not found'); return J.user(u); });
  on('GET', '/user/get-by-role/{role}', ADMIN, function (p) { return db.users.filter(function (u) { return u.role === p.role; }).map(J.user); });
  on('POST', '/user/add-admin', ADMIN, function (p, b) {
    checkUserDTO(b);
    db.users.push({ id: nid(), fullName: b.fullName, email: b.email, password: b.password, role: 'ADMIN' });
    return ok('Admin added successfully');
  });
  on('DELETE', '/user/delete/{id}', ADMIN, function (p, b, me) {
    need(Number(p.id) !== me.id, 'You cannot delete your own account');
    need(find(db.users, p.id), 'User not found');
    db.users = db.users.filter(function (u) { return u.id !== Number(p.id); });
    return ok('User deleted successfully');
  });

  // المنظمين
  on('GET', '/organizer/get-all', ADMIN, function () {
    return db.users.filter(function (u) { return u.role === 'ORGANIZER'; }).map(function (u) { return { id: u.id, phoneNumber: u.phoneNumber }; });
  });
  on('POST', '/organizer/register', ADMIN, function (p, b) {
    checkUserDTO(b); checkPhone(b);
    db.users.push({ id: nid(), fullName: b.fullName, email: b.email, password: b.password, role: 'ORGANIZER', phoneNumber: b.phoneNumber });
    return ok('Organizer registered successfully');
  });
  on('PUT', '/organizer/update/{id}', ADMIN, function (p, b) {
    var u = find(db.users, p.id); need(u && u.role === 'ORGANIZER', 'Organizer not found');
    checkUserDTO(b, u.id); checkPhone(b, u.id);
    Object.assign(u, { fullName: b.fullName, email: b.email, password: b.password, phoneNumber: b.phoneNumber });
    return ok('Organizer updated successfully');
  });
  on('DELETE', '/organizer/delete/{id}', ADMIN, function (p) {
    db.users = db.users.filter(function (u) { return !(u.id === Number(p.id) && u.role === 'ORGANIZER'); });
    return ok('Organizer deleted successfully');
  });

  // الزوار
  on('POST', '/visitor/register', PUBLIC, function (p, b) {
    checkUserDTO(b); checkPhone(b);
    db.users.push({ id: nid(), fullName: b.fullName, email: b.email, password: b.password, role: 'VISITOR', phoneNumber: b.phoneNumber, qrToken: uuid(), showPrayerTimes: !!b.showPrayerTimes });
    return ok('Visitor registered successfully');
  });
  on('GET', '/visitor/get-all', ADMIN, function () {
    return db.users.filter(function (u) { return u.role === 'VISITOR'; }).map(function (u) { return { id: u.id, phoneNumber: u.phoneNumber }; });
  });
  on('PUT', '/visitor/update/{id}', ['VISITOR', 'ADMIN'], function (p, b, me) {
    if (me.role === 'VISITOR' && Number(p.id) !== me.id) throw new Err(403, 'You can only update your own account');
    var u = find(db.users, p.id); need(u && u.role === 'VISITOR', 'Visitor not found');
    checkUserDTO(b, u.id); checkPhone(b, u.id);
    Object.assign(u, { fullName: b.fullName, email: b.email, password: b.password, phoneNumber: b.phoneNumber, showPrayerTimes: !!b.showPrayerTimes });
    return ok('Visitor updated successfully');
  });
  on('DELETE', '/visitor/delete/{id}', ['VISITOR', 'ADMIN'], function (p, b, me) {
    if (me.role === 'VISITOR' && Number(p.id) !== me.id) throw new Err(403, 'Forbidden');
    db.users = db.users.filter(function (u) { return u.id !== Number(p.id); });
    return ok('Visitor deleted successfully');
  });

  // المناطق
  on('GET', '/zone/get', PUBLIC, function () { return db.zones.map(J.zone); });
  on('GET', '/zone/get-zone-by-name/{name}', PUBLIC, function (p) {
    var z = db.zones.filter(function (x) { return x.name.toLowerCase() === decodeURIComponent(p.name).toLowerCase(); })[0];
    if (!z) throw new Err(400, 'Zone not found'); return J.zone(z);
  });
  function checkZone(b, ignoreId) {
    req(b, ['name', 'capacity', 'initialVisitors']);
    need(b.capacity > 0, 'Capacity must be greater than zero');
    need(b.initialVisitors >= 0, 'Initial visitors cannot be negative');
    need(b.initialVisitors <= b.capacity, 'Initial visitors cannot exceed zone capacity');
    need(!db.zones.some(function (z) { return z.name.toLowerCase() === String(b.name).toLowerCase() && z.id !== ignoreId; }), 'Zone name already exists');
  }
  on('POST', '/zone/add', ORG, function (p, b) {
    checkZone(b);
    db.zones.push({ id: nid(), name: b.name, capacity: b.capacity, initialVisitors: b.initialVisitors, indoor: !!b.indoor, hasPrayerArea: !!b.hasPrayerArea });
    return ok('Zone added successfully');
  });
  on('PUT', '/zone/update/{id}', ORG, function (p, b) {
    var z = zone(p.id); need(z, 'Zone not found'); checkZone(b, z.id);
    Object.assign(z, { name: b.name, capacity: b.capacity, initialVisitors: b.initialVisitors, indoor: !!b.indoor, hasPrayerArea: !!b.hasPrayerArea });
    return ok('Zone updated successfully');
  });
  on('DELETE', '/zone/delete/{id}', ORG, function (p) {
    var id = Number(p.id); need(zone(id), 'Zone not found');
    need(!db.connections.some(function (c) { return c.from === id || c.to === id; }), 'Cannot delete a zone with existing connections');
    db.zones = db.zones.filter(function (z) { return z.id !== id; });
    db.events = db.events.filter(function (e) { return e.zone !== id; });
    return ok('Zone deleted successfully');
  });

  // الروابط
  on('GET', '/zone-connection/get', PUBLIC, function () { return db.connections.map(J.conn); });
  function checkConn(from, to, ignoreId) {
    need(zone(from) && zone(to), 'Zone not found');
    need(Number(from) !== Number(to), 'A zone cannot connect to itself');
    need(!db.connections.some(function (c) { return c.from === Number(from) && c.to === Number(to) && c.id !== ignoreId; }), 'Zone connection already exists');
  }
  on('POST', '/zone-connection/add/{from}/{to}', ORG, function (p, b) {
    req(b, ['maxVisitorsPerMinute']); need(b.maxVisitorsPerMinute > 0, 'Maximum visitors must be greater than zero');
    checkConn(p.from, p.to);
    db.connections.push({ id: nid(), maxVisitorsPerMinute: b.maxVisitorsPerMinute, from: Number(p.from), to: Number(p.to) });
    return ok('Zone connection added successfully');
  });
  on('PUT', '/zone-connection/update/{id}', ORG, function (p, b) {
    var c = find(db.connections, p.id); need(c, 'Zone connection not found');
    req(b, ['maxVisitorsPerMinute']); need(b.maxVisitorsPerMinute > 0, 'Maximum visitors must be greater than zero');
    c.maxVisitorsPerMinute = b.maxVisitorsPerMinute; return ok('Zone connection updated successfully');
  });
  on('PUT', '/zone-connection/change-zones/{id}/{from}/{to}', ORG, function (p) {
    var c = find(db.connections, p.id); need(c, 'Zone connection not found');
    checkConn(p.from, p.to, c.id); c.from = Number(p.from); c.to = Number(p.to);
    return ok('Connection zones changed successfully');
  });
  on('DELETE', '/zone-connection/delete/{id}', ORG, function (p) {
    need(find(db.connections, p.id), 'Zone connection not found');
    db.connections = db.connections.filter(function (c) { return c.id !== Number(p.id); });
    return ok('Zone connection deleted successfully');
  });

  // الفعاليات
  on('GET', '/expo-event/get', PUBLIC, function () { return db.events.map(J.event); });
  on('GET', '/expo-event/get-by-id/{id}', PUBLIC, function (p) { var e = find(db.events, p.id); if (!e) throw new Err(400, 'Event not found'); return J.event(e); });
  on('GET', '/expo-event/get-by-zone/{zoneId}', PUBLIC, function (p) { return db.events.filter(function (e) { return e.zone === Number(p.zoneId); }).map(J.event); });
  on('GET', '/expo-event/get-by-organizer/{id}', ORG, function (p) { return db.events.filter(function (e) { return e.organizer === Number(p.id); }).map(J.event); });
  function checkEvent(b) {
    req(b, ['name', 'startDateTime', 'endDateTime', 'expectedDemand']);
    need(b.expectedDemand >= 0, 'Expected demand cannot be negative');
    need(new Date(b.endDateTime) > new Date(b.startDateTime), 'End date must be after start date');
  }
  on('POST', '/expo-event/add/{organizerId}/{zoneId}', ORG, function (p, b) {
    checkEvent(b); need(zone(p.zoneId), 'Zone not found');
    db.events.push({ id: nid(), name: b.name, startDateTime: b.startDateTime, endDateTime: b.endDateTime, expectedDemand: b.expectedDemand, zone: Number(p.zoneId), organizer: Number(p.organizerId) });
    return ok('Event added successfully');
  });
  on('PUT', '/expo-event/update/{id}', ORG, function (p, b) {
    var e = find(db.events, p.id); need(e, 'Event not found'); checkEvent(b);
    Object.assign(e, { name: b.name, startDateTime: b.startDateTime, endDateTime: b.endDateTime, expectedDemand: b.expectedDemand });
    return ok('Event updated successfully');
  });
  on('PUT', '/expo-event/change-zone/{id}/{zoneId}', ORG, function (p) {
    var e = find(db.events, p.id); need(e, 'Event not found'); need(zone(p.zoneId), 'Zone not found');
    need(e.zone !== Number(p.zoneId), 'Event is already assigned to this zone');
    e.zone = Number(p.zoneId); return ok('Event zone changed successfully');
  });
  on('DELETE', '/expo-event/delete/{id}', ORG, function (p) {
    need(find(db.events, p.id), 'Event not found');
    db.events = db.events.filter(function (e) { return e.id !== Number(p.id); });
    return ok('Event deleted successfully');
  });

  // الحضور التاريخي
  on('GET', '/attendance/get-all', ORG, function () { return db.attendance.map(J.att); });
  function checkAtt(b, ignoreId) {
    req(b, ['expoName', 'attendanceDate', 'totalAttendance', 'sourceUrl']);
    need(b.totalAttendance >= 0, 'Total attendance cannot be negative');
    need(b.attendanceDate <= dateOnly(0), 'Attendance date cannot be in the future');
    need(!db.attendance.some(function (a) { return a.expoName === b.expoName && a.attendanceDate === b.attendanceDate && a.id !== ignoreId; }), 'Attendance for this expo on this date already exists');
  }
  on('POST', '/attendance/add', ORG, function (p, b) {
    checkAtt(b); db.attendance.push(Object.assign({ id: nid() }, J.att(b), { id: db.nextId }));
    return ok('Historical attendance added successfully');
  });
  on('PUT', '/attendance/update/{id}', ORG, function (p, b) {
    var a = find(db.attendance, p.id); need(a, 'Historical attendance not found'); checkAtt(b, a.id);
    Object.assign(a, { expoName: b.expoName, attendanceDate: b.attendanceDate, totalAttendance: b.totalAttendance, sourceUrl: b.sourceUrl });
    return ok('Historical attendance updated successfully');
  });
  on('DELETE', '/attendance/delete/{id}', ORG, function (p) {
    var id = Number(p.id); need(find(db.attendance, id), 'Historical attendance not found');
    var simIds = db.simulations.filter(function (s) { return s.attendance === id; }).map(function (s) { return s.id; });
    db.interventions = db.interventions.filter(function (i) { return simIds.indexOf(i.simulation) < 0; });
    db.simulations = db.simulations.filter(function (s) { return s.attendance !== id; });
    db.attendance = db.attendance.filter(function (a) { return a.id !== id; });
    return ok('Historical attendance deleted successfully');
  });

  // المحاكاة
  on('GET', '/simulation/get-all', ORG, function () { return db.simulations.map(J.sim); });
  on('GET', '/simulation/get-by-organizer/{id}', ORG, function (p) { return db.simulations.filter(function (s) { return s.organizer === Number(p.id); }).map(J.sim); });
  function checkSim(b) {
    req(b, ['durationMinutes', 'randomSeed']);
    need(b.durationMinutes >= 1, 'Duration must be at least 1 minute');
    need(b.durationMinutes <= 600, 'Duration cannot exceed 600 minutes');
    if (b.status) need(/^(PENDING|RUNNING|COMPLETED|FAILED)$/.test(b.status), 'Status must be PENDING, RUNNING, COMPLETED or FAILED');
  }
  on('POST', '/simulation/add/{organizerId}/{attendanceId}', ORG, function (p, b) {
    checkSim(b); need(find(db.attendance, p.attendanceId), 'Historical attendance not found');
    db.simulations.push({ id: nid(), durationMinutes: b.durationMinutes, status: 'PENDING', randomSeed: b.randomSeed, startDateTime: b.startDateTime, endDateTime: b.endDateTime, attendance: Number(p.attendanceId), organizer: Number(p.organizerId) });
    return ok('Simulation added successfully');
  });
  on('PUT', '/simulation/update/{id}', ORG, function (p, b) {
    var s = find(db.simulations, p.id); need(s, 'Simulation not found');
    need(s.status === 'PENDING', 'Only pending simulations can be updated'); checkSim(b);
    Object.assign(s, { durationMinutes: b.durationMinutes, randomSeed: b.randomSeed, startDateTime: b.startDateTime, endDateTime: b.endDateTime, status: b.status || s.status });
    return ok('Simulation updated successfully');
  });
  on('DELETE', '/simulation/delete/{id}', ORG, function (p) {
    var s = find(db.simulations, p.id); need(s, 'Simulation not found');
    need(s.status !== 'RUNNING', 'Cannot delete a running simulation');
    db.interventions = db.interventions.filter(function (i) { return i.simulation !== s.id; });
    db.simulations = db.simulations.filter(function (x) { return x.id !== s.id; });
    return ok('Simulation deleted successfully');
  });
  on('GET', '/simulation-snapshot/get-by-simulation/{id}', ORG, function (p) { return snapshotsFor(p.id); });

  // التدخلات
  on('GET', '/intervention/get-all', ORG, function () { return db.interventions.map(J.int); });
  function checkInt(sim, b, src, tgt) {
    req(b, ['startMinute', 'visitorsPerMinute']);
    need(b.startMinute >= 0, 'Start minute cannot be negative');
    need(b.visitorsPerMinute > 0, 'Visitors per minute must be positive');
    need(zone(src), 'Source zone not found'); need(zone(tgt), 'Target zone not found');
    need(Number(src) !== Number(tgt), 'Source zone and target zone must be different');
    need(b.startMinute < sim.durationMinutes, 'Start minute must be less than simulation duration');
  }
  on('POST', '/intervention/add/{simId}/{src}/{tgt}', ORG, function (p, b) {
    var s = find(db.simulations, p.simId); need(s, 'Simulation not found');
    need(s.status !== 'RUNNING', 'Cannot add intervention to a running simulation');
    checkInt(s, b, p.src, p.tgt);
    db.interventions.push({ id: nid(), startMinute: b.startMinute, visitorsPerMinute: b.visitorsPerMinute, simulation: s.id, source: Number(p.src), target: Number(p.tgt) });
    return ok('Intervention added successfully');
  });
  on('PUT', '/intervention/update/{id}/{src}/{tgt}', ORG, function (p, b) {
    var i = find(db.interventions, p.id); need(i, 'Intervention not found');
    var s = find(db.simulations, i.simulation);
    need(s.status !== 'RUNNING', 'Cannot update intervention of a running simulation');
    checkInt(s, b, p.src, p.tgt);
    Object.assign(i, { startMinute: b.startMinute, visitorsPerMinute: b.visitorsPerMinute, source: Number(p.src), target: Number(p.tgt) });
    return ok('Intervention updated successfully');
  });
  on('DELETE', '/intervention/delete/{id}', ORG, function (p) {
    var i = find(db.interventions, p.id); need(i, 'Intervention not found');
    var s = find(db.simulations, i.simulation);
    need(!s || s.status !== 'RUNNING', 'Cannot delete intervention of a running simulation');
    db.interventions = db.interventions.filter(function (x) { return x.id !== i.id; });
    return ok('Intervention deleted successfully');
  });

  /* ---------- بطاقة الـ QR وسجل الزيارات (موجودة في مشروعكم) ---------- */
  function ownVisitor(p, me) {
    var u = find(db.users, p.visitorId);
    need(u && u.role === 'VISITOR', 'Visitor not found');
    if (me.role === 'VISITOR' && u.id !== me.id) throw new Err(403, 'Forbidden');
    return u;
  }
  // الحقيقي يرجّع صورة PNG (ZXing). هنا نولّدها بالمتصفح من qrToken
  on('GET', '/visitor/qr-code/{visitorId}', ANY_USER, function (p, b, me) {
    var u = ownVisitor(p, me);
    return { __image: g.MergabQR ? g.MergabQR.toDataURL(u.qrToken, { scale: 10, margin: 2, dark: '#0E3D32' }) : null };
  });
  // نفس شكل ZoneVisitResponseDTO
  function visitDTO(v) {
    var z = zone(v.zone);
    return { visitId: v.id, visitorId: v.visitor, zoneId: v.zone, zoneName: z ? z.name : null, checkInTime: v.checkIn, checkOutTime: v.checkOut, manualCheckOut: !!v.manual };
  }
  on('GET', '/zone-visit/history/{visitorId}', ANY_USER, function (p, b, me) {
    var u = ownVisitor(p, me);
    return db.zoneVisits.filter(function (v) { return v.visitor === u.id; }).map(visitDTO);
  });
  function nowStr() { var d = new Date(), q = function (n) { return String(n).padStart(2, '0'); }; return d.getFullYear() + '-' + q(d.getMonth() + 1) + '-' + q(d.getDate()) + 'T' + q(d.getHours()) + ':' + q(d.getMinutes()) + ':' + q(d.getSeconds()); }
  function byQr(b) { var u = db.users.filter(function (x) { return b && x.qrToken === b.qrToken; })[0]; if (!u) bad('Invalid visitor QR code'); return u; }
  // موظف البوابة (منظم) يمسح الرمز
  on('POST', '/zone-visit/check-in/{zoneId}', ORG, function (p, b) {
    var u = byQr(b), z = zone(p.zoneId); need(z, 'Zone not found');
    need(!db.zoneVisits.some(function (v) { return v.visitor === u.id && !v.checkOut; }), 'Visitor must check out from the current zone first');
    db.zoneVisits.push({ id: nid(), visitor: u.id, zone: z.id, checkIn: nowStr(), checkOut: null, manual: false });
    z.initialVisitors = Math.min(z.capacity, z.initialVisitors + 1);
    return ok('Check-in successful');
  });
  on('PUT', '/zone-visit/check-out/{zoneId}', ORG, function (p, b) {
    var u = byQr(b);
    var v = db.zoneVisits.filter(function (x) { return x.visitor === u.id && !x.checkOut; })[0];
    need(v, 'Visitor has no active zone visit');
    need(v.zone === Number(p.zoneId), 'Visitor is not inside this zone');
    v.checkOut = nowStr();
    var z = zone(v.zone); if (z) z.initialVisitors = Math.max(0, z.initialVisitors - 1);
    return ok('Check-out successful');
  });

  /* ---------- أوقات الصلاة (endpoints مقترحة) ----------
     في الباك إند: PrayerTimesService يجيبها من Aladhan مرة باليوم (@Scheduled) ويحفظها.
     هنا المتصفح يجيبها مباشرة (أو يستخدم أوقات تقريبية لو ما فيه نت). */
  var CTX_KEY = 'mergab.prayer.v2';
  var PRAYERS = ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha'];
  var SAMPLE = { prayers: { fajr: '04:41', dhuhr: '11:44', asr: '15:05', maghrib: '17:35', isha: '19:05' }, source: 'sample' };
  function timeout(ms, p) { return Promise.race([p, new Promise(function (_, r) { setTimeout(function () { r(new Error('timeout')); }, ms); })]); }
  function prayerTimes() {
    try {
      var c = JSON.parse(localStorage.getItem(CTX_KEY));
      if (c && c.day === new Date().toDateString()) return Promise.resolve(c.data);
    } catch (e) {}
    var A = 'https://api.aladhan.com/v1/timingsByCity?city=Riyadh&country=Saudi%20Arabia&method=4';
    return timeout(6000, fetch(A)).then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); }).then(function (j) {
      var tm = j.data.timings;
      var data = { prayers: { fajr: tm.Fajr.slice(0, 5), dhuhr: tm.Dhuhr.slice(0, 5), asr: tm.Asr.slice(0, 5), maghrib: tm.Maghrib.slice(0, 5), isha: tm.Isha.slice(0, 5) }, source: 'live' };
      try { localStorage.setItem(CTX_KEY, JSON.stringify({ day: new Date().toDateString(), data: data })); } catch (e) {}
      return data;
    }).catch(function () { return JSON.parse(JSON.stringify(SAMPLE)); });
  }
  function riyadhMinutes() {
    var s = new Date().toLocaleTimeString('en-GB', { timeZone: 'Asia/Riyadh', hour12: false });
    var p = s.split(':'); return Number(p[0]) * 60 + Number(p[1]);
  }
  function toMin(hhmm) { var p = String(hhmm).split(':'); return Number(p[0]) * 60 + Number(p[1]); }
  function hhmm(min) { min = ((Math.round(min) % 1440) + 1440) % 1440; var q = function (n) { return String(n).padStart(2, '0'); }; return q(Math.floor(min / 60)) + ':' + q(min % 60); }
  // نسبة الزوار اللي فعّلوا أوقات الصلاة → تحدد قوة ازدحام المصليات في التوقع
  function optInShare() {
    var v = db.users.filter(function (u) { return u.role === 'VISITOR'; });
    return v.length ? v.filter(function (u) { return u.showPrayerTimes; }).length / v.length : 0;
  }
  // ?simulateTime=17:20 → وضع تجربة للعرض
  function ctxBase(q) {
    return prayerTimes().then(function (data) {
      var simT = q.simulateTime ? toMin(q.simulateTime) : null;
      return { data: data, now: simT != null ? simT : riyadhMinutes(), simulated: simT != null };
    });
  }
  on('GET', '/prayer/today', PUBLIC, function (p) {
    return ctxBase(p._q).then(function (c) {
      var d = c.data, next = null, current = null;
      PRAYERS.forEach(function (k) {
        var diff = toMin(d.prayers[k]) - c.now;
        if (diff <= 0 && diff > -20) current = k;
        if (diff > 0 && (!next || diff < next.minutesLeft)) next = { key: k, time: d.prayers[k], minutesLeft: diff };
      });
      if (!next) next = { key: 'fajr', time: d.prayers.fajr, minutesLeft: toMin(d.prayers.fajr) + 1440 - c.now };
      return {
        prayers: d.prayers, nextPrayer: next, currentPrayer: current,
        prayerZones: db.zones.filter(function (z) { return z.hasPrayerArea; }).map(J.zone),
        localTime: hhmm(c.now), source: d.source, simulated: c.simulated
      };
    });
  });
  // توقع لكل منطقة كل step دقيقة لمدة hours ساعة (قاعدة بسيطة: أوقات الصلاة × نسبة المفعّلين)
  on('GET', '/forecast/zones', PUBLIC, function (p) {
    var q = p._q, hours = Math.min(6, Number(q.hours) || 2), step = Math.max(10, Number(q.step) || 30);
    var share = optInShare();
    return ctxBase(q).then(function (c) {
      var d = c.data;
      var zones = db.zones.map(function (z) {
        var gate = /gate/i.test(z.name), food = /food/i.test(z.name), points = [];
        for (var m = 0; m <= hours * 60; m += step) {
          var t = c.now + m, f = 1, reasons = [];
          PRAYERS.forEach(function (k) {
            var diff = toMin(d.prayers[k]) - (t % 1440);
            if (diff > 0 && diff <= 20) {
              if (z.hasPrayerArea) { f *= 1 + 0.6 * share; reasons.push({ code: 'PRAYER_SOON', prayer: k }); }
              else if (!gate) f *= 1 - 0.14 * share;
            } else if (diff <= 0 && diff >= -15) {
              if (z.hasPrayerArea) { f *= 1 + 0.9 * share; reasons.push({ code: 'PRAYER_NOW', prayer: k }); }
              else if (!gate) { f *= 1 - 0.3 * share; reasons.push({ code: 'LESS_PRAYER', prayer: k }); }
            } else if (diff < -15 && diff >= -40 && food) { f *= 1 + 0.6 * share; reasons.push({ code: 'AFTER_PRAYER', prayer: k }); }
          });
          var exp = Math.min(z.capacity, Math.round(z.initialVisitors * f));
          points.push({ minute: m, time: hhmm(t), expectedVisitors: exp, percent: Math.round(exp / z.capacity * 100), reasons: reasons });
        }
        return { zone: J.zone(z), points: points };
      });
      return { prayerOptInShare: Math.round(share * 100) / 100, generatedAt: hhmm(c.now), zones: zones };
    });
  });

  /* ---------- التنفيذ ---------- */
  function handle(method, path, body, session) {
    load();
    return new Promise(function (resolve, reject) {
      setTimeout(function () {
        try {
          var route = null, params = {}, qs = '';
          if (path.indexOf('?') >= 0) { qs = path.slice(path.indexOf('?') + 1); path = path.slice(0, path.indexOf('?')); }
          params._q = {};
          new URLSearchParams(qs).forEach(function (v, k) { params._q[k] = v; });
          for (var i = 0; i < R.length; i++) {
            var m = R[i].method === method && path.match(R[i].re);
            if (m) { route = R[i]; R[i].keys.forEach(function (k, j) { params[k] = decodeURIComponent(m[j + 1]); }); break; }
          }
          if (!route) throw new Err(404, 'No endpoint ' + method + ' ' + path);
          var me = null;
          if (route.roles !== PUBLIC) {
            if (!session) throw new Err(401, 'Unauthorized');
            me = find(db.users, session.id);
            if (!me) throw new Err(401, 'Unauthorized');
            if (route.roles.indexOf(me.role) < 0) throw new Err(403, 'Forbidden — your role (' + me.role + ') cannot do this');
          }
          var result = route.fn(params, body ? JSON.parse(JSON.stringify(body)) : body, me);
          if (method !== 'GET') save();
          Promise.resolve(result).then(function (r) { resolve(JSON.parse(JSON.stringify(r == null ? null : r))); }, reject);
        } catch (e) {
          if (e instanceof Err) reject({ status: e.status, message: e.message });
          else reject({ status: 500, message: String(e && e.message || e) });
        }
      }, 120);
    });
  }

  // مثل httpBasic: الإيميل + كلمة المرور → المستخدم
  function login(email, password) {
    load();
    return new Promise(function (resolve, reject) {
      setTimeout(function () {
        var u = db.users.filter(function (x) { return x.email.toLowerCase() === String(email).trim().toLowerCase(); })[0];
        if (!u || u.password !== password) reject({ status: 401, message: 'Invalid email or password' });
        else resolve(J.user(u));
      }, 200);
    });
  }

  g.MockServer = {
    handle: handle, login: login, reset: reset,
    // للعرض فقط: يمسح بطاقة الزائر كأنه موظف البوابة (دخول ↔ خروج)
    demoScan: function (visitorId, zoneId) {
      load();
      var u = find(db.users, visitorId);
      var active = db.zoneVisits.filter(function (v) { return v.visitor === u.id && !v.checkOut; })[0];
      var org = db.users.filter(function (x) { return x.role === 'ORGANIZER'; })[0];
      var s = { id: org.id };
      return active
        ? handle('PUT', '/zone-visit/check-out/' + active.zone, { qrToken: u.qrToken }, s).then(function () { return { out: true, zone: J.zone(zone(active.zone)) }; })
        : handle('POST', '/zone-visit/check-in/' + zoneId, { qrToken: u.qrToken }, s).then(function () { return { out: false, zone: J.zone(zone(zoneId)) }; });
    },
    demoAccounts: [
      { email: 'admin@mergab.sa', password: 'admin123', role: 'ADMIN' },
      { email: 'organizer@mergab.sa', password: 'org123', role: 'ORGANIZER' },
      { email: 'sara@gmail.com', password: 'visitor123', role: 'VISITOR' }
    ]
  };
})(window);
