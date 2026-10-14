/* =========================================================
   Mergab — qr.js
   مولّد QR صغير بدون مكتبات (Byte mode، تصحيح أخطاء M، الإصدارات 1–10)
   يُستخدم في الوضع التجريبي فقط. في الوضع الحقيقي الصورة تجي من
   GET /api/v1/visitor/qr-code/{visitorId} (ZXing في الباك إند).

   الاستخدام:  MergabQR.toDataURL('النص', { scale: 8, margin: 4 })
   ========================================================= */
(function (g) {
  'use strict';

  // [مجموع الكلمات، كلمات التصحيح لكل بلوك، [[عدد البلوكات، بيانات البلوك], ...]] — مستوى M
  var SPEC = [null,
    [26, 10, [[1, 16]]], [44, 16, [[1, 28]]], [70, 26, [[1, 44]]], [100, 18, [[2, 32]]],
    [134, 24, [[2, 43]]], [172, 16, [[4, 27]]], [196, 18, [[4, 31]]], [242, 22, [[2, 38], [2, 39]]],
    [292, 22, [[3, 36], [2, 37]]], [346, 26, [[4, 43], [1, 44]]]
  ];
  var ALIGN = [null, [], [6, 18], [6, 22], [6, 26], [6, 30], [6, 34], [6, 22, 38], [6, 24, 42], [6, 26, 46], [6, 28, 50]];

  function dataCap(v) { return SPEC[v][2].reduce(function (s, b) { return s + b[0] * b[1]; }, 0); }

  function utf8(str) {
    var out = [], s = unescape(encodeURIComponent(str));
    for (var i = 0; i < s.length; i++) out.push(s.charCodeAt(i));
    return out;
  }

  /* ---------- Reed–Solomon على GF(256) ---------- */
  function mul(x, y) {
    var z = 0;
    for (var i = 7; i >= 0; i--) { z = (z << 1) ^ ((z >>> 7) * 0x11D); z ^= ((y >>> i) & 1) * x; }
    return z & 0xFF;
  }
  function divisor(deg) {
    var r = []; for (var i = 0; i < deg; i++) r.push(0);
    r[deg - 1] = 1;
    var root = 1;
    for (i = 0; i < deg; i++) {
      for (var j = 0; j < deg; j++) { r[j] = mul(r[j], root); if (j + 1 < deg) r[j] ^= r[j + 1]; }
      root = mul(root, 2);
    }
    return r;
  }
  function remainder(data, div) {
    var r = div.map(function () { return 0; });
    data.forEach(function (b) {
      var f = b ^ r.shift(); r.push(0);
      for (var i = 0; i < r.length; i++) r[i] ^= mul(div[i], f);
    });
    return r;
  }

  /* ---------- الترميز ---------- */
  function encode(text) {
    var bytes = utf8(text), v;
    for (v = 1; v <= 10; v++) {
      var bits = 4 + (v < 10 ? 8 : 16) + bytes.length * 8;
      if (bits <= dataCap(v) * 8) break;
    }
    if (v > 10) throw new Error('QR: text too long');
    var cap = dataCap(v), bb = [];
    function put(val, len) { for (var i = len - 1; i >= 0; i--) bb.push((val >>> i) & 1); }
    put(4, 4); put(bytes.length, v < 10 ? 8 : 16);
    bytes.forEach(function (b) { put(b, 8); });
    put(0, Math.min(4, cap * 8 - bb.length));
    while (bb.length % 8) bb.push(0);
    var data = [];
    for (var i = 0; i < bb.length; i += 8) { var x = 0; for (var k = 0; k < 8; k++) x = (x << 1) | bb[i + k]; data.push(x); }
    for (var p = 0; data.length < cap; p++) data.push(p % 2 ? 0x11 : 0xEC);

    // تقسيم البلوكات + التصحيح + التداخل
    var ecLen = SPEC[v][1], div = divisor(ecLen), blocks = [], ecs = [], off = 0;
    SPEC[v][2].forEach(function (grp) {
      for (var n = 0; n < grp[0]; n++) { var blk = data.slice(off, off + grp[1]); off += grp[1]; blocks.push(blk); ecs.push(remainder(blk, div)); }
    });
    var out = [], maxLen = Math.max.apply(null, blocks.map(function (b) { return b.length; }));
    for (i = 0; i < maxLen; i++) blocks.forEach(function (b) { if (i < b.length) out.push(b[i]); });
    for (i = 0; i < ecLen; i++) ecs.forEach(function (e) { out.push(e[i]); });
    return { version: v, codewords: out };
  }

  /* ---------- المصفوفة ---------- */
  function matrix(text) {
    var enc = encode(text), v = enc.version, size = v * 4 + 17;
    var mod = [], fn = [];
    for (var y = 0; y < size; y++) { mod.push(new Array(size).fill(false)); fn.push(new Array(size).fill(false)); }
    function setF(x, y, dark) { mod[y][x] = dark; fn[y][x] = true; }

    for (var i = 0; i < size; i++) { setF(6, i, i % 2 === 0); setF(i, 6, i % 2 === 0); }
    function finder(cx, cy) {
      for (var dy = -4; dy <= 4; dy++) for (var dx = -4; dx <= 4; dx++) {
        var d = Math.max(Math.abs(dx), Math.abs(dy)), xx = cx + dx, yy = cy + dy;
        if (xx >= 0 && xx < size && yy >= 0 && yy < size) setF(xx, yy, d !== 2 && d !== 4);
      }
    }
    finder(3, 3); finder(size - 4, 3); finder(3, size - 4);
    var al = ALIGN[v], n = al.length;
    for (i = 0; i < n; i++) for (var j = 0; j < n; j++) {
      if ((i === 0 && j === 0) || (i === 0 && j === n - 1) || (i === n - 1 && j === 0)) continue;
      for (var dy = -2; dy <= 2; dy++) for (var dx = -2; dx <= 2; dx++) setF(al[i] + dx, al[j] + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
    }
    function format(mask) {
      var data = (0 << 3) | mask, rem = data;            // M = 00
      for (var i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
      var bits = ((data << 10) | rem) ^ 0x5412;
      var b = function (k) { return ((bits >>> k) & 1) !== 0; };
      for (i = 0; i <= 5; i++) setF(8, i, b(i));
      setF(8, 7, b(6)); setF(8, 8, b(7)); setF(7, 8, b(8));
      for (i = 9; i < 15; i++) setF(14 - i, 8, b(i));
      for (i = 0; i < 8; i++) setF(size - 1 - i, 8, b(i));
      for (i = 8; i < 15; i++) setF(8, size - 15 + i, b(i));
      setF(8, size - 8, true);
    }
    format(0);
    if (v >= 7) {
      var rem = v;
      for (i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
      var vb = (v << 12) | rem;
      for (i = 0; i < 18; i++) {
        var bit = ((vb >>> i) & 1) !== 0, a = size - 11 + (i % 3), c = Math.floor(i / 3);
        setF(a, c, bit); setF(c, a, bit);
      }
    }

    // وضع البيانات (زِق زاق)
    var cw = enc.codewords, k = 0;
    for (var right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      for (var vert = 0; vert < size; vert++) for (j = 0; j < 2; j++) {
        var x = right - j, up = ((right + 1) & 2) === 0, yy = up ? size - 1 - vert : vert;
        if (!fn[yy][x] && k < cw.length * 8) { mod[yy][x] = ((cw[k >>> 3] >>> (7 - (k & 7))) & 1) !== 0; k++; }
      }
    }

    var MASKS = [
      function (x, y) { return (x + y) % 2 === 0; }, function (x, y) { return y % 2 === 0; },
      function (x) { return x % 3 === 0; }, function (x, y) { return (x + y) % 3 === 0; },
      function (x, y) { return (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0; },
      function (x, y) { return x * y % 2 + x * y % 3 === 0; },
      function (x, y) { return (x * y % 2 + x * y % 3) % 2 === 0; },
      function (x, y) { return ((x + y) % 2 + x * y % 3) % 2 === 0; }
    ];
    function applyMask(m) {
      for (var y = 0; y < size; y++) for (var x = 0; x < size; x++) if (!fn[y][x] && MASKS[m](x, y)) mod[y][x] = !mod[y][x];
    }
    function penalty() {
      var p = 0, x, y, run, dark = 0;
      for (y = 0; y < size; y++) {             // صفوف وأعمدة متتالية
        for (var dir = 0; dir < 2; dir++) {
          run = 1;
          for (x = 1; x < size; x++) {
            var cur = dir ? mod[x][y] : mod[y][x], prev = dir ? mod[x - 1][y] : mod[y][x - 1];
            if (cur === prev) { run++; if (run === 5) p += 3; else if (run > 5) p++; } else run = 1;
          }
        }
      }
      for (y = 0; y < size - 1; y++) for (x = 0; x < size - 1; x++) {   // مربعات 2×2
        var c = mod[y][x];
        if (c === mod[y][x + 1] && c === mod[y + 1][x] && c === mod[y + 1][x + 1]) p += 3;
      }
      var pat = [true, false, true, true, true, false, true, false, false, false, false];
      for (y = 0; y < size; y++) for (x = 0; x + 11 <= size; x++) {     // نمط يشبه المحدِّد
        for (var d = 0; d < 2; d++) {
          var okA = true, okB = true;
          for (var q = 0; q < 11; q++) {
            var mv = d ? mod[x + q][y] : mod[y][x + q];
            if (mv !== pat[q]) okA = false;
            if (mv !== pat[10 - q]) okB = false;
          }
          if (okA) p += 40; if (okB) p += 40;
        }
      }
      for (y = 0; y < size; y++) for (x = 0; x < size; x++) if (mod[y][x]) dark++;
      p += Math.floor(Math.abs(dark * 20 - size * size * 10) / (size * size)) * 10;
      return p;
    }
    var best = 0, bestP = Infinity;
    for (var m = 0; m < 8; m++) {
      applyMask(m); format(m);
      var pp = penalty();
      if (pp < bestP) { bestP = pp; best = m; }
      applyMask(m);
    }
    applyMask(best); format(best);
    return mod;
  }

  function toDataURL(text, opts) {
    opts = opts || {};
    var m = matrix(text), scale = opts.scale || 8, margin = opts.margin == null ? 4 : opts.margin;
    var size = m.length, px = (size + margin * 2) * scale;
    var c = document.createElement('canvas');
    c.width = c.height = px;
    var ctx = c.getContext('2d');
    ctx.fillStyle = opts.light || '#FFFFFF'; ctx.fillRect(0, 0, px, px);
    ctx.fillStyle = opts.dark || '#000000';
    for (var y = 0; y < size; y++) for (var x = 0; x < size; x++) if (m[y][x]) ctx.fillRect((x + margin) * scale, (y + margin) * scale, scale, scale);
    return c.toDataURL('image/png');
  }

  g.MergabQR = { matrix: matrix, toDataURL: toDataURL };
})(window);
