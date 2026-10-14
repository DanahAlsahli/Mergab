# الصور — وين تحطينها وبأي اسم

ما تحتاجين تعدلين أي كود. حطي الصورة بالاسم الصحيح، وتطلع في مكانها تلقائيًا.
وإذا الصورة مو موجودة، يطلع مكانها تصميم مؤقت ملوّن.

| الصورة | المسار والاسم |
|---|---|
| اللوقو | `assets/img/logo.svg` (أو png، وغيّري الاسم في `assets/js/site-config.js`) |
| لوقو أبيض للخلفيات الداكنة (اختياري) | `assets/img/logo-white.svg` |
| صورة الهيرو في الرئيسية | `assets/img/hero.jpg` (عرضها 1920 تقريبًا) |
| خريطة المعرض للزائر | `assets/img/map/expo-map.jpg`، وبعدها افتحي `tools/calibrate.html` |

## صور المناطق: `assets/img/zones/`
الاسم = اسم المنطقة بالإنجليزي (مثل ما هو في الداتابيس)، بحروف صغيرة، والمسافات تصير شرطات، والامتداد `.jpg`.

| المنطقة | اسم الملف |
|---|---|
| North Gate | `north-gate.jpg` |
| Main Plaza | `main-plaza.jpg` |
| Innovation Hub | `innovation-hub.jpg` |
| Culture Zone | `culture-zone.jpg` |
| Food Court | `food-court.jpg` |
| Exhibition Hall | `exhibition-hall.jpg` |
| Family Zone | `family-zone.jpg` |
| South Gate | `south-gate.jpg` |

أي منطقة جديدة تنطبق عليها نفس القاعدة: `VIP Lounge` تصير `vip-lounge.jpg`.
صور الفعاليات تاخذ صورة المنطقة اللي فيها الفعالية.

**مقاس مقترح لصور المناطق:** 800×500 أو أكبر، بصيغة jpg. ويفضّل تخفيف حجمها (أقل من 300KB) عشان الموقع يكون سريع.

> صور إكسبو 2030 الرياض والخريطة ملك إكسبو. للعرض الداخلي في المشروع ما فيه مشكلة، بشرط يبقى سطر المصدر ظاهر في الفوتر (`showImageCredit: true` في site-config.js). ولا تستخدمين شعارات إكسبو أو أيقوناته.
