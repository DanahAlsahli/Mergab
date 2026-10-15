/* =========================================================
   Mergab — site-config.js
   إعدادات الموقع اللي تتغير بدون ما تلمسين باقي الكود:
   اللوقو، صور المناطق، خريطة الزائر
   (كل المسارات من جذر الموقع)
   ========================================================= */
window.MERGAB_CONFIG = {

  // اللوقو المعتمد: حطيه في assets/img/ وغيّري الاسم هنا (svg أو png)
  // لو الملف مو موجود، يطلع اللوقو المؤقت تلقائيًا
  logo: 'assets/img/logo.png',
  logoLight: 'assets/img/logo-white.png',   // نسخة بيضاء للخلفيات الداكنة

  // صورة الهيرو في الصفحة الرئيسية
  heroImage: 'assets/img/hero.jpg', // صورة بانورامية جديدة مولّدة للمشروع

  // صور المناطق: assets/img/zones/<اسم-المنطقة>.jpg
  // الاسم = اسم المنطقة بالإنجليزي، حروف صغيرة، والمسافات شرطات
  // مثال: Main Plaza → main-plaza.jpg
  zoneImagesFolder: 'assets/img/zones/',

  // سطر مصدر الصور في الفوتر (فعّليه لو استخدمتوا صور من موقع إكسبو)
  showImageCredit: false, // الصور الجديدة مولّدة للمشروع وليست من مكتبة إكسبو الرسمية

  // صفحة "معلومات الزيارة" — القيم هنا أمثلة للعرض، عدّليها حسب المعرض
  visit: {
    hours: { ar: 'يوميًا من 4:00 م إلى 12:00 ص', en: 'Daily, 4:00 PM – 12:00 AM' },
    tickets: { ar: 'التذاكر إلكترونية، وتقدر تحجزها من الموقع الرسمي للمعرض.', en: 'Tickets are digital and can be booked on the official expo website.' },
    transport: { ar: 'أقرب مدخل للمواقف ومحطة النقل هو البوابة الجنوبية.', en: 'The South Gate is the closest entrance to parking and public transport.' },
    accessibility: { ar: 'كراسي متحركة مجانية عند مركز الخدمات، والمسارات مهيأة لذوي الإعاقة.', en: 'Free wheelchairs at the services desk, and step-free paths across the site.' },
    // المرافق: zone = اسم المنطقة بالإنجليزي (يفتح مكانها على الخريطة)
    facilities: [
      { icon: 'pray',   name: { ar: 'مصلى',               en: 'Prayer room' },        zone: 'Main Plaza' },
      { icon: 'pray',   name: { ar: 'مصلى',               en: 'Prayer room' },        zone: 'Exhibition Hall' },
      { icon: 'wc',     name: { ar: 'دورات مياه',          en: 'Restrooms' },          zone: 'Food Court' },
      { icon: 'wc',     name: { ar: 'دورات مياه',          en: 'Restrooms' },          zone: 'Culture Zone' },
      { icon: 'aid',    name: { ar: 'إسعافات أولية',       en: 'First aid' },          zone: 'Main Plaza' },
      { icon: 'water',  name: { ar: 'نقاط مياه',           en: 'Water stations' },     zone: 'Family Zone' },
      { icon: 'charge', name: { ar: 'شحن الجوال',          en: 'Phone charging' },     zone: 'Innovation Hub' },
      { icon: 'info',   name: { ar: 'مكتب الاستعلامات والمفقودات', en: 'Info & lost and found' }, zone: 'North Gate' },
      { icon: 'wheel',  name: { ar: 'كراسي متحركة',        en: 'Wheelchairs' },        zone: 'South Gate' },
      { icon: 'car',    name: { ar: 'مواقف السيارات',      en: 'Parking' },            zone: 'South Gate' }
    ]
  },

  // خريطة الزائر (وضع الصورة)
  // بعد ما تحطين خريطة إكسبو في assets/img/map/expo-map.jpg
  // افتحي tools/calibrate.html وحددي مكان كل منطقة، وانسخي النتيجة هنا
  map: {
    src: 'assets/img/map/expo-map-concept.jpg',
    hub: 'mainPlaza',
    zones: [
      { key: 'northGate',      name: { ar: 'البوابة الشمالية', en: 'North Gate' },      x: 50.0, y: 9.0 },
      { key: 'mainPlaza',      name: { ar: 'الساحة الرئيسية',  en: 'Main Plaza' },      x: 50.5, y: 45.0 },
      { key: 'innovationHub',  name: { ar: 'منطقة الابتكار',   en: 'Innovation Hub' },  x: 66.5, y: 23.0 },
      { key: 'cultureZone',    name: { ar: 'المنطقة الثقافية', en: 'Culture Zone' },    x: 27.0, y: 27.0 },
      { key: 'foodCourt',      name: { ar: 'منطقة الطعام',     en: 'Food Court' },      x: 27.0, y: 61.0 },
      { key: 'exhibitionHall', name: { ar: 'قاعة المعارض',     en: 'Exhibition Hall' }, x: 73.0, y: 56.0 },
      { key: 'familyZone',     name: { ar: 'المنطقة العائلية', en: 'Family Zone' },     x: 50.5, y: 75.0 },
      { key: 'southGate',      name: { ar: 'البوابة الجنوبية', en: 'South Gate' },      x: 51.0, y: 92.0 }
    ]
  }
};
