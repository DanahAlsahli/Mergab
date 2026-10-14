# فرونت إند مرقاب (Mergab) — مع الصلاحيات

HTML و CSS و JavaScript عادي، بدون framework.
مبني على إن **Spring Security مفعّلة**: صفحة دخول وحدة للكل، وكل دور له منطقته، والصفحات محمية.

> **حاليًا غير مربوط بالباك إند.** يشتغل على باك إند وهمي داخل المتصفح (`js/mock-server.js`). هذا الباك إند الوهمي فيه:
> - نفس الـ endpoints ونفس شكل الـ JSON حق مشروع Mergab.
> - نفس رسائل الأخطاء.
> - نفس قواعد الصلاحيات المقترحة (401 لو مو داخل، 403 لو دورك ما يسمح).
>
> البيانات تنحفظ في المتصفح، فالإضافة والتعديل والحذف تشتغل وتبقى بين الصفحات.

## التشغيل
افتحي المجلد في VS Code، واضغطي كليك يمين على `../templates/login.html` واختاري **Open with Live Server**.
(فتح الملفات مباشرة بالضغط عليها يشتغل بعد، بس Live Server أضمن.)

### حسابات تجريبية (تطلع في صفحة الدخول)
| الدور | الإيميل | كلمة المرور | يروح لـ |
|---|---|---|---|
| أدمن | admin@mergab.sa | admin123 | `../templates/admin/index.html` |
| منظم | organizer@mergab.sa | org123 | `organizer/dashboard.html` |
| زائر | sara@gmail.com | visitor123 | `visitor/index.html` |

وفيه زر في صفحة الدخول يرجّع البيانات التجريبية لحالتها الأولى.

## الجديد في هذي النسخة (جزء الزائر)
- **عربي / English:** زر اللغة موجود في الهيدر وصفحة الدخول، والاختيار ينحفظ. كل النصوص في `js/i18n.js`.
- **هوية سعودية:**
  - خط كوفي للعناوين العربية (Noto Kufi Arabic).
  - شريط السدو.
  - الشرفات النجدية فوق الفوتر.
  - نقشة هندسية في الهيرو.
- **اللوقو والصور:** كل شي له مكان جاهز. شوفي `img/README.md`.
- **خريطة الزائر:** صورة خريطة حقيقية (تحطينها أنتِ)، ومكان كل منطقة عليها تحددينه بأداة `../templates/tools/calibrate.html`. الإعدادات كلها في `js/site-config.js`.

## الفلو

```
login.html  ──►  الباك إند يتحقق ويرجّع الدور (GET /api/v1/user/me)
                    ├─ VISITOR    → visitor/index.html
                    ├─ ORGANIZER  → organizer/dashboard.html
                    └─ ADMIN      → admin/index.html
```

| الدور | كيف ينشأ حسابه | وش يقدر يسوي |
|---|---|---|
| زائر | يسجّل بنفسه من `visitor/register.html`، وبعدها يدخل تلقائيًا | يتصفح الخريطة والفعاليات (حتى بدون دخول)، ويحفظ أماكن، ويعدّل حسابه أو يحذفه |
| منظم | **الأدمن** يضيفه من `../templates/admin/users.html` | المناطق والروابط والفعاليات والمحاكاة والتدخلات والتقارير |
| أدمن | أول أدمن من `data.sql`، وبعدها أدمن يضيف أدمن | إدارة الحسابات: إضافة منظم أو أدمن، وحذف حسابات |

**حماية الصفحات:**
- لو فتحتي صفحة منظم وأنتِ مو داخلة، تنتقلين لصفحة الدخول، وبعد الدخول ترجعين لنفس الصفحة.
- لو دخلتي كزائر وفتحتي صفحة منظم، تطلع لك صفحة `../templates/forbidden.html` (403).
- **مهم:** هذي الحماية في الفرونت للتجربة بس. الحماية الحقيقية تكون في الباك إند (شوفي `SECURITY.md`).

## الملفات
```
mergab-frontend/
├── login.html               صفحة الدخول الموحّدة
├── forbidden.html           403 — ما عندك صلاحية
├── index.html               يحوّل لموقع الزائر
├── assets/
│   ├── css/mergab.css
│   ├── js/api.js            ⚙️ MODE + الجلسة والصلاحيات + الطلبات
│   ├── js/mock-server.js    الباك إند الوهمي (يُحذف لما نربط)
│   ├── js/organizer.js      هيكل لوحتي المنظم والأدمن + أداة الجداول
│   ├── js/visitor.js        هيدر وفوتر الزائر + أماكني المحفوظة
│   └── map/                 الخريطة التفاعلية
├── assets/js/i18n.js        النصوص بالعربي والإنجليزي
├── assets/js/site-config.js اللوقو + الصور + إعداد خريطة الزائر
├── assets/img/              اللوقو وصور المناطق والخريطة (شوفي README داخله)
├── tools/calibrate.html     تحديد أماكن المناطق على صورة الخريطة
├── visitor/                 index · map · events · register · account
├── organizer/               dashboard · zones · connections · events · simulations · reports
└── admin/                   index (نظرة عامة) · users (المستخدمين والأدوار)
```

## الربط بالباك إند الحقيقي (لاحقًا)
1. في الباك إند: فعّلوا الـ Security حسب `SECURITY.md`، وأضيفوا `GET /api/v1/user/me`.
2. في `js/api.js` غيّروا `var MODE = ... 'mock'` إلى `'live'`.
3. احذفوا سطر `<script src="…/mock-server.js">` من الصفحات.
4. حطوا المجلد في `src/main/resources/static` وافتحوا `http://localhost:8080/login.html`.

في وضع `live`، الدخول يكون بـ **httpBasic** (نفس اللي كان في `Configuration.java` عندكم): الإيميل وكلمة المرور ينرسلون مع كل طلب، وينحفظون في sessionStorage فقط، يعني ينمسحون لما تقفلين التبويب.

## ملاحظات
- **"أماكني المحفوظة"** تنحفظ في المتصفح حاليًا لأنه ما فيه لها endpoint. لو تبونها في الداتابيس، تحتاج جدول `SavedZone` (visitor + zone).
- **نتائج المحاكاة** تطلع للمحاكاة اللي حالتها COMPLETED، من زر الرسم البياني في صفحة Simulations.


## تحديث صور صفحة الزائر
- `img/hero.jpg`: صورة بانورامية تصورية جديدة مولّدة خصيصًا للمشروع.
- `img/map/expo-map-concept.jpg`: مخطط تصوري تفاعلي مولّد للمشروع، مع نقاط ضغط فوق المناطق.
- صور `img/zones/`: صور مصغرة محسّنة مشتقة من المشهد التصوري لتوحيد الشكل البصري.
- **مهم:** هذه الرسومات ليست صورًا رسمية من إكسبو 2030 الرياض، والخريطة ليست المخطط الرسمي الدقيق. أسماء المناطق والإحصائيات المعروضة في المشروع تجريبية. للحصول على الخريطة الرسمية المعتمدة، استخدمي مواد الإعلام الرسمية بعد التأكد من شروط الاستخدام.


## بطاقة الدخول + أوقات الصلاة (v7)

### وين تطلع بالموقع
| الخاصية | الصفحة |
|---|---|
| بطاقة الدخول (QR) + حالة الزائر + زياراتي | `visitor/account.html` |
| خيار "عرض أوقات الصلاة" | `visitor/register.html` و `visitor/account.html` |
| شريط الصلاة القادمة (للي فعّلها بس) | `visitor/index.html` و `visitor/map.html` |
| أوقات الصلوات الخمس (قائمة تحت بعض) | `visitor/visit.html#prayer` |
| توقع الازدحام (الحين / ٣٠ د / ساعة / ساعتين) | `visitor/map.html` و `organizer/dashboard.html` |
| تنبيهات الصلاة والازدحام للمنظم | `organizer/dashboard.html` |

### خيار أوقات الصلاة
- حقل جديد في `Visitor`: `showPrayerTimes` (boolean)، وينرسل مع `VisitorDTO` في `/visitor/register` و `/visitor/update/{id}`.
- `/user/me` يرجّعه داخل `visitor`: `{ id, phoneNumber, showPrayerTimes }`.
- لو `false`: ما تطلع للزائر أي أوقات صلاة، والتوقع يطلع له بسبب عام بدون ذكر الصلاة.
- التوقع يستخدم **نسبة الزوار اللي فعّلوه** لتحديد قوة ازدحام المصليات وقت الصلاة.

### Endpoints موجودة في مشروعكم (الفرونت يستخدمها مثل ما هي)
- `GET /visitor/qr-code/{visitorId}` → صورة PNG.
- `GET /zone-visit/history/{visitorId}` → `[ZoneVisitResponseDTO]`.

### Endpoints جديدة مقترحة
**1) `GET /prayer/today`** (PUBLIC)
```json
{
  "prayers": { "fajr": "04:39", "dhuhr": "11:42", "asr": "15:02", "maghrib": "17:31", "isha": "19:01" },
  "nextPrayer": { "key": "maghrib", "time": "17:31", "minutesLeft": 11 },
  "currentPrayer": null,
  "prayerZones": [ { "id": 2, "name": "Main Plaza", "capacity": 10000, "initialVisitors": 9100 } ],
  "localTime": "17:20", "source": "live", "simulated": false
}
```
**2) `GET /forecast/zones?hours=2&step=30`** (PUBLIC)
```json
{ "prayerOptInShare": 0.5, "generatedAt": "17:20",
  "zones": [ { "zone": { "id": 6, "name": "Exhibition Hall", "capacity": 12000 },
               "points": [ { "minute": 15, "time": "17:35", "expectedVisitors": 11300, "percent": 94,
                             "reasons": [ { "code": "PRAYER_NOW", "prayer": "maghrib" } ] } ] } ] }
```
أكواد الأسباب: `PRAYER_SOON`, `PRAYER_NOW`, `AFTER_PRAYER`, `LESS_PRAYER`.

**3) حقلين في `Zone`:** `hasPrayerArea` و `indoor` (boolean). المنظم يحددهم من صفحة المناطق.

**وضع التجربة للعرض:** ضيفي `?simulateTime=17:20` للرابط، مثلًا `organizer/dashboard.html?simulateTime=17:20`.

**المصدر الخارجي:** Aladhan بتقويم أم القرى (مجاني وبدون مفتاح):
`https://api.aladhan.com/v1/timingsByCity?city=Riyadh&country=Saudi%20Arabia&method=4`

## الأيقونات
كل الأيقونات من **Google Material Symbols (Rounded)** وتنحمّل من Google Fonts، فتحتاج إنترنت.
تتغير من مكان واحد: `ICONS` في `js/api.js`.

## لوحة المنظم والأدمن
نفس تصميم موقع الزائر (الألوان واللوقو والسدو)، وبالعربي والإنجليزي. نصوصها في `js/i18n-staff.js`.
