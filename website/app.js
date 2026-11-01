// Islamic Azkar — website. A small hash-routed app over the same azkar data as the Android app.
(() => {
  "use strict";

  // ───────────── Texts ─────────────
  const T = {
    ar: {
      dir: "rtl", appName: "أذكار إسلامية", tagline: "حصن المسلم بحُلّةٍ زجاجية أنيقة: أذكار اليوم والليلة مع العدّاد والترجمة.",
      home: "الرئيسية", favorites: "المفضّلة", tasbih: "المسبحة", settings: "الإعدادات",
      now: "الآن", recommended: "مقترح الآن", categories: "الأقسام", search: "ابحث في الأذكار والأدعية",
      morning: "أذكار الصباح", evening: "أذكار المساء", sleep: "أذكار النوم", prayer: "أذكار الصلاة",
      azkar: n => arCount(n, "ذكر واحد", "ذكران", "أذكار", "ذكرًا"), chapters: n => arCount(n, "باب واحد", "بابان", "أبواب", "بابًا"),
      of: (a, b) => `${num(a)} من ${num(b)}`, times: n => n === 1 ? "مرة" : arCount(n, "مرة", "مرتان", "مرات", "مرة"), done: "تمّ",
      back: "رجوع", hisn: "حصن المسلم", noResults: "لا نتائج", results: "نتائج البحث",
      noFav: "لم تُضِف أي ذكر إلى المفضّلة بعد. اضغط على القلب في أي ذكر لحفظه هنا.",
      copied: "تم النسخ", added: "أُضيف إلى المفضّلة", removed: "أُزيل من المفضّلة",
      congrats: "تقبّل الله منك", completed: t => `أتممتَ ${t}`, prayerFor: "دعاء لك", streak: n => `${num(n)} ${n === 1 ? "يوم" : n === 2 ? "يومان" : n <= 10 ? "أيام" : "يومًا"} متتالية`,
      seeEvening: "نلقاك في أذكار المساء إن شاء الله", seeMorning: "نلقاك في أذكار الصباح إن شاء الله", alhamdulillah: "الحمد لله",
      reps: n => arCount(n, "تكرار واحد", "تكراران", "تكرارات", "تكرارًا"),
      textSize: "حجم الخط", showTr: "إظهار النطق", showMean: "إظهار المعنى", theme: "المظهر", system: "تلقائي", light: "فاتح", dark: "داكن", close: "إغلاق",
      tapCount: "اضغط للعدّ", reset: "تصفير", total: n => `المجموع: ${num(n)}`, target: "العدد",
      getApp: "تطبيق أندرويد", appText: "أدوات للشاشة الرئيسية بخطٍّ عربيٍّ أصيل، مواقيت الصلاة، تذكيرات ومسبحة.",
      apk: "تنزيل التطبيق", apkNote: "ملف APK · أندرويد ٨ أو أحدث", join: "انضم إلى المختبِرين",
      joinText: "التطبيق في مرحلة الاختبار على Google Play. سجّل بريد Gmail المرتبط بمتجرك لنضيفك إلى المختبِرين ونرسل لك رابط التثبيت.",
      emailPh: "name@gmail.com", send: "سجّلني", sending: "جارٍ الإرسال…", badEmail: "أدخل عنوان بريد صحيحًا",
      thanks: "جزاك الله خيرًا! وصلنا بريدك وسنضيفك إلى المختبِرين قريبًا إن شاء الله.", failed: "تعذّر الإرسال، حاول مرة أخرى.",
      mushaf: "تطبيق المصحف", mushafText: "اقرأ القرآن الكريم بخط المصحف الشريف، صدقة جارية من فريقنا.", mushafCta: "حمّله من mushafapp.com",
      privacy: "سياسة الخصوصية", dedication: "صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم",
      cat: { prayer: "الصلاة والمسجد", sleep: "النوم والاستيقاظ", home: "البيت والطهارة", hardship: "الكرب والحماية", illness: "المرض والجنائز", food: "الطعام والصيام", family: "الأسرة والمعاملات", travel: "السفر", nature: "الطبيعة والأحوال", hajj: "الحج والعمرة", praise: "الذكر والاستغفار" },
    },
    en: {
      dir: "ltr", appName: "Islamic Azkar", tagline: "Hisn al-Muslim in a calm, elegant design: the daily azkar with a counter, transliteration and meaning.",
      home: "Home", favorites: "Favorites", tasbih: "Tasbih", settings: "Settings",
      now: "Now", recommended: "Recommended now", categories: "Categories", search: "Search azkar and duas",
      morning: "Morning Azkar", evening: "Evening Azkar", sleep: "Before Sleep", prayer: "After Prayer",
      azkar: n => n === 1 ? "1 dhikr" : `${n} azkar`, chapters: n => n === 1 ? "1 chapter" : `${n} chapters`,
      of: (a, b) => `${a} of ${b}`, times: n => n === 1 ? "time" : "times", done: "Done",
      back: "Back", hisn: "Hisn al-Muslim", noResults: "No results", results: "Search results",
      noFav: "No favorites yet. Tap the heart on any dhikr to keep it here.",
      copied: "Copied", added: "Added to favorites", removed: "Removed from favorites",
      congrats: "May Allah accept from you", completed: t => `You completed ${t}`, prayerFor: "A prayer for you", streak: n => `${n} day${n === 1 ? "" : "s"} in a row`,
      seeEvening: "See you at the evening azkar, in sha Allah", seeMorning: "See you at the morning azkar, in sha Allah", alhamdulillah: "Alhamdulillah",
      reps: n => n === 1 ? "1 repetition" : `${n} repetitions`,
      textSize: "Text size", showTr: "Show transliteration", showMean: "Show meaning", theme: "Appearance", system: "Auto", light: "Light", dark: "Dark", close: "Close",
      tapCount: "Tap to count", reset: "Reset", total: n => `Total: ${n}`, target: "Target",
      getApp: "Android app", appText: "Home-screen widgets in real Arabic calligraphy, prayer times, reminders and a tasbih.",
      apk: "Download the app", apkNote: "APK file · Android 8 or later", join: "Join the testers",
      joinText: "The app is in testing on Google Play. Leave the Gmail address you use on the Play Store and we’ll add you as a tester and send you the install link.",
      emailPh: "name@gmail.com", send: "Sign me up", sending: "Sending…", badEmail: "Enter a valid email address",
      thanks: "Jazak Allah khayran! We got your address and will add you to the testers soon, in sha Allah.", failed: "Couldn’t send it. Please try again.",
      mushaf: "Mushaf app", mushafText: "Read the Holy Quran in the script of the printed Mushaf, a sadaqa jariyah from our team.", mushafCta: "Download from mushafapp.com",
      privacy: "Privacy policy", dedication: "On behalf of Mohamad Al-Koussa, Dima Ghannoum and their families",
      cat: { prayer: "Prayer & Mosque", sleep: "Sleep & Waking", home: "Home & Purity", hardship: "Distress & Protection", illness: "Illness & Funerals", food: "Food & Fasting", family: "Family & Social", travel: "Travel", nature: "Nature & Weather", hajj: "Hajj & Umrah", praise: "Praise & Forgiveness" },
    },
    fr: {
      dir: "ltr", appName: "Islamic Azkar", tagline: "La Citadelle du musulman dans un design apaisant : les adhkar du quotidien avec compteur, translittération et traduction.",
      home: "Accueil", favorites: "Favoris", tasbih: "Tasbih", settings: "Réglages",
      now: "Maintenant", recommended: "Recommandé maintenant", categories: "Catégories", search: "Rechercher des invocations",
      morning: "Adhkar du matin", evening: "Adhkar du soir", sleep: "Avant de dormir", prayer: "Après la prière",
      azkar: n => n === 1 ? "1 invocation" : `${n} invocations`, chapters: n => n === 1 ? "1 chapitre" : `${n} chapitres`,
      of: (a, b) => `${a} sur ${b}`, times: n => n === 1 ? "fois" : "fois", done: "Terminé",
      back: "Retour", hisn: "La Citadelle du musulman", noResults: "Aucun résultat", results: "Résultats",
      noFav: "Aucun favori pour l’instant. Touchez le cœur d’une invocation pour la garder ici.",
      copied: "Copié", added: "Ajouté aux favoris", removed: "Retiré des favoris",
      congrats: "Qu’Allah accepte de toi", completed: t => `Tu as terminé : ${t}`, prayerFor: "Une invocation pour toi", streak: n => `${n} jour${n === 1 ? "" : "s"} d’affilée`,
      seeEvening: "À ce soir pour les adhkar du soir, in shâ Allah", seeMorning: "À demain matin pour les adhkar du matin, in shâ Allah", alhamdulillah: "Al-hamdu lillâh",
      reps: n => n === 1 ? "1 répétition" : `${n} répétitions`,
      textSize: "Taille du texte", showTr: "Afficher la translittération", showMean: "Afficher la traduction", theme: "Apparence", system: "Auto", light: "Clair", dark: "Sombre", close: "Fermer",
      tapCount: "Touchez pour compter", reset: "Remettre à zéro", total: n => `Total : ${n}`, target: "Objectif",
      getApp: "Application Android", appText: "Des widgets en véritable calligraphie arabe, les horaires de prière, des rappels et un chapelet.",
      apk: "Télécharger l’application", apkNote: "Fichier APK · Android 8 ou plus récent", join: "Devenir testeur",
      joinText: "L’application est en test sur Google Play. Laissez l’adresse Gmail de votre Play Store : nous vous ajouterons aux testeurs et vous enverrons le lien d’installation.",
      emailPh: "nom@gmail.com", send: "M’inscrire", sending: "Envoi…", badEmail: "Saisissez une adresse e-mail valide",
      thanks: "Jazak Allah khayran ! Nous avons bien reçu votre adresse et vous ajouterons aux testeurs très bientôt, in shâ Allah.", failed: "Échec de l’envoi. Réessayez.",
      mushaf: "Application Mushaf", mushafText: "Lisez le Saint Coran dans l’écriture du Mushaf imprimé, une sadaqa jariya de notre équipe.", mushafCta: "Télécharger sur mushafapp.com",
      privacy: "Confidentialité", dedication: "Au nom de Mohamad Al-Koussa, Dima Ghannoum et de leurs familles",
      cat: { prayer: "Prière & mosquée", sleep: "Sommeil & réveil", home: "Maison & purification", hardship: "Détresse & protection", illness: "Maladie & funérailles", food: "Repas & jeûne", family: "Famille & société", travel: "Voyage", nature: "Nature & météo", hajj: "Hajj & Omra", praise: "Louange & pardon" },
    },
  };

  const PRAYER_AR = "جَعَلَكَ اللَّهُ مِنَ الذَّاكِرِينَ اللَّهَ كَثِيرًا وَالذَّاكِرَاتِ، وَثَبَّتَ قَلْبَكَ عَلَى ذِكْرِهِ";
  const PRAYER_MEANING = {
    ar: "", en: "May Allah make you among those who remember Him much, and keep your heart firm upon His remembrance.",
    fr: "Qu’Allah fasse de toi l’un de ceux qui L’évoquent beaucoup, et qu’Il affermisse ton cœur dans Son rappel.",
  };
  const ENCOURAGEMENTS = [
    ["أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ", "متفق عليه", "The most beloved deeds to Allah are the most consistent, even if small.", "Les œuvres les plus aimées d’Allah sont les plus régulières, même si elles sont peu nombreuses.", "Al-Bukhārī & Muslim"],
    ["فَاذْكُرُونِي أَذْكُرْكُمْ", "البقرة ١٥٢", "So remember Me; I will remember you.", "Souvenez-vous de Moi, Je Me souviendrai de vous.", "Al-Baqarah 2:152"],
    ["أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "الرعد ٢٨", "Truly, in the remembrance of Allah do hearts find rest.", "C’est par le rappel d’Allah que les cœurs s’apaisent.", "Ar-Raʿd 13:28"],
    ["لَا يَزَالُ لِسَانُكَ رَطْبًا مِنْ ذِكْرِ اللَّهِ", "رواه الترمذي", "Keep your tongue moist with the remembrance of Allah.", "Que ta langue ne cesse d’être humectée par le rappel d’Allah.", "At-Tirmidhī"],
    ["مَثَلُ الَّذِي يَذْكُرُ رَبَّهُ وَالَّذِي لَا يَذْكُرُ رَبَّهُ مَثَلُ الْحَيِّ وَالْمَيِّتِ", "رواه البخاري", "The one who remembers his Lord and the one who does not are like the living and the dead.", "Celui qui évoque son Seigneur et celui qui ne L’évoque pas sont comme le vivant et le mort.", "Al-Bukhārī"],
  ];
  const PHRASES = ["سُبْحَانَ اللَّهِ", "الْحَمْدُ لِلَّهِ", "اللَّهُ أَكْبَرُ", "لَا إِلَهَ إِلَّا اللَّهُ", "أَسْتَغْفِرُ اللَّهَ", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"];
  const BULK = 100; // azkar said this many times get a single "Done" button

  // ───────────── Icons ─────────────
  const I = {
    home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
    heart: '<path d="M12 20.5s-7.5-4.6-9.3-9.2C1.4 8 3.6 4.5 7.1 4.5c2 0 3.6 1.1 4.9 2.8 1.3-1.7 2.9-2.8 4.9-2.8 3.5 0 5.7 3.5 4.4 6.8C19.5 15.9 12 20.5 12 20.5z"/>',
    beads: '<circle cx="12" cy="5" r="2.2"/><circle cx="6.5" cy="9" r="2.2"/><circle cx="17.5" cy="9" r="2.2"/><circle cx="8" cy="15.5" r="2.2"/><circle cx="16" cy="15.5" r="2.2"/><path d="M12 18v4"/>',
    gear: '<circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
    sun: '<circle cx="12" cy="12" r="4.2"/><path d="M12 2v2.5M12 19.5V22M4.2 4.2 6 6M18 18l1.8 1.8M2 12h2.5M19.5 12H22M4.2 19.8 6 18M18 6l1.8-1.8"/>',
    moon: '<path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"/>',
    bed: '<path d="M3 6v13M3 15h18v4M21 15v-2.5a3 3 0 0 0-3-3h-7.5V15"/><circle cx="6.8" cy="11.6" r="2"/>',
    mosque: '<path d="M12 3c.3 1 1.2 1.6 2.2 1.6-.6.4-1.4.6-2.2.4-.8.2-1.6 0-2.2-.4 1 0 1.9-.6 2.2-1.6zM12 6c-3 1.4-5 3.6-5 6.2h10c0-2.6-2-4.8-5-6.2zM6 13h12v8h-3.5v-3.5a2.5 2.5 0 0 0-5 0V21H6zM2.5 9h2v12h-2zM19.5 9h2v12h-2z"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
    chev: '<path d="m9 6 6 6-6 6"/>',
    back: '<path d="m15 6-6 6 6 6"/>',
    copy: '<rect x="8" y="8" width="12" height="12" rx="2.5"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
    share: '<path d="M12 3v12M7 8l5-5 5 5M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6"/>',
    check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
    text: '<path d="M4 7V5h16v2M9 19h6M12 5v14"/>',
    reset: '<path d="M4 12a8 8 0 1 0 2.4-5.7M4 4v4h4"/>',
    spark: '<path d="M12 3l1.8 5.4L19 10l-5.2 1.6L12 17l-1.8-5.4L5 10l5.2-1.6z"/>',
    download: '<path d="M12 3v12M7 10l5 5 5-5M5 19h14"/>',
    mail: '<rect x="3" y="5" width="18" height="14" rx="2.5"/><path d="m4 7 8 6 8-6"/>',
  };
  // Always the newest APK: GitHub redirects /releases/latest/download/<asset> to the latest release's file.
  const APK_URL = "https://github.com/Mkuz1/islamic-azkar-releases/releases/latest/download/IslamicAzkar.apk";
  const REGISTER_URL = /(^|\.)islamicazkar\.net$/.test(location.hostname) ? "/api/register" : "https://islamicazkar.net/api/register";
  const svg = (name, fill) => `<svg viewBox="0 0 24 24" fill="${fill ? "currentColor" : "none"}" stroke="${fill ? "none" : "currentColor"}" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${I[name]}</svg>`;
  const CAT_STYLE = {
    prayer: ["mosque", "#34c759"], sleep: ["bed", "#5856d6"], home: ["home", "#ff9500"], hardship: ["heart", "#ff3b30"], illness: ["spark", "#30b0c7"],
    food: ["sun", "#ffcc00"], family: ["heart", "#af52de"], travel: ["chev", "#007aff"], nature: ["moon", "#32ade6"], hajj: ["mosque", "#a2845e"], praise: ["beads", "#1e6b52"],
  };

  // ───────────── State ─────────────
  const store = {
    get(k, d) { try { const v = localStorage.getItem(k); return v === null ? d : JSON.parse(v); } catch { return d; } },
    set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* private mode */ } },
  };
  const guessLang = () => { const l = (navigator.language || "en").slice(0, 2); return l === "ar" ? "ar" : l === "fr" ? "fr" : "en"; };
  let lang = new URLSearchParams(location.search).get("lang") || store.get("lang", guessLang());
  if (!T[lang]) lang = "en";
  let prefs = Object.assign({ scale: 1, tr: true, mean: true, theme: "system" }, store.get("prefs", {}));
  let favs = new Set(store.get("favs", []));
  let data = null, index = null;

  const s = () => T[lang];
  const num = n => lang === "ar" ? String(n).replace(/\d/g, d => "٠١٢٣٤٥٦٧٨٩"[d]) : String(n);
  function arCount(n, one, two, few, many) { if (n === 1) return one; if (n === 2) return two; return `${num(n)} ${n <= 10 ? few : many}`; }
  const esc = t => String(t).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const today = () => { const d = new Date(); return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`; };
  const meaning = z => lang === "ar" ? "" : (z[lang] || z.en || "");
  const chapterTitle = c => c[lang] || c.en;
  const normalize = t => t.replace(/[ً-ٰٟـ]/g, "").replace(/[إأآٱ]/g, "ا").replace(/ى/g, "ي").replace(/ة/g, "ه").toLowerCase();

  function readingOf(key) {
    if (key === "morning") return { key, title: s().morning, items: data.morning };
    if (key === "evening") return { key, title: s().evening, items: data.evening };
    const id = +key.replace(/^c/, "");
    const c = index.chapters.get(id);
    return c ? { key, title: chapterTitle(c), items: c.items, chapter: c } : null;
  }

  // ───────────── Theme / prefs ─────────────
  function applyPrefs() {
    const root = document.documentElement;
    if (prefs.theme === "system") root.removeAttribute("data-theme"); else root.setAttribute("data-theme", prefs.theme);
    root.style.setProperty("--scale", prefs.scale);
    document.body.classList.toggle("hide-tr", !prefs.tr);
    document.body.classList.toggle("hide-mean", !prefs.mean);
    store.set("prefs", prefs);
  }
  function applyLang() {
    const html = document.documentElement;
    html.lang = lang; html.dir = s().dir;
    document.title = lang === "ar" ? "أذكار إسلامية · حصن المسلم" : lang === "fr" ? "Islamic Azkar · Citadelle du musulman" : "Islamic Azkar · Hisn al-Muslim";
    store.set("lang", lang);
  }

  // ───────────── Shell ─────────────
  const app = document.getElementById("app");
  function shell(active, body) {
    const t = s();
    const nav = [["#/", "home", t.home, "home"], ["#/fav", "heart", t.favorites, "fav"], ["#/tasbih", "beads", t.tasbih, "tasbih"]]
      .map(([h, ic, label, k]) => `<a href="${h}" class="press ${active === k ? "on" : ""}">${svg(ic, ic === "heart" && active === k)}<span>${label}</span></a>`).join("");
    return `
      <header class="topbar"><div class="wrap"><div class="bar glass">
        <a class="brand" href="#/"><img src="img/icon.png" alt=""><span class="cal">أذكار</span><span class="name">${lang === "ar" ? "" : "Islamic Azkar"}</span></a>
        <nav class="nav nav-top">${nav}</nav>
        <span class="spacer"></span>
        <div class="seg" role="group" aria-label="Language">${["ar", "en", "fr"].map(l => `<button data-lang="${l}" class="${l === lang ? "on" : ""}">${l === "ar" ? "ع" : l.toUpperCase()}</button>`).join("")}</div>
        <button class="icon-btn press" id="open-settings" aria-label="${t.settings}">${svg("text")}</button>
      </div></div></header>
      <nav class="nav nav-bottom">${nav}</nav>
      <main><div class="wrap ${active === "read" || active === "tasbih" ? "narrow" : ""}">${body}</div></main>
      <footer class="site">
        <div class="ded">${T.ar.dedication}</div>
        <div class="ded-l">${T.en.dedication} · ${T.fr.dedication}</div>
        <a href="privacy/">${t.privacy}</a> · © ${new Date().getFullYear()} Mkoussa
      </footer>`;
  }

  // ───────────── Screens ─────────────
  function momentKey() { const h = new Date().getHours(); return h >= 4 && h < 12 ? "morning" : h < 15 ? "prayer" : h < 20 ? "evening" : "sleep"; }
  function hijri() {
    try {
      const loc = lang === "ar" ? "ar-SA-u-ca-islamic-umalqura-nu-arab" : `${lang}-u-ca-islamic-umalqura`;
      return new Intl.DateTimeFormat(loc, { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(new Date());
    } catch { return ""; }
  }

  function home() {
    const t = s(), now = momentKey();
    const tiles = [
      ["morning", "#/read/morning", "sun", "#ff9500", data.morning.length],
      ["evening", "#/read/evening", "moon", "#5856d6", data.evening.length],
      ["sleep", "#/read/c28", "bed", "#007aff", index.chapters.get(28).items.length],
      ["prayer", "#/read/c25", "mosque", "#34c759", index.chapters.get(25).items.length],
    ].map(([k, h, ic, col, n]) => `<a class="tile glass press" href="${h}">${k === now ? `<span class="badge">${t.now}</span>` : ""}
        <span class="sym" style="background:${col}">${svg(ic)}</span><b>${t[k]}</b><small>${t.azkar(n)}</small></a>`).join("");
    const cats = data.categories.map(c => {
      const [ic, col] = CAT_STYLE[c.key] || ["spark", "#8e8e93"];
      return `<a class="row press" href="#/cat/${c.key}"><span class="sym" style="background:${col}">${svg(ic)}</span><span class="t"><b>${t.cat[c.key]}</b></span><span class="meta">${num(c.chapters.length)}</span>${svg("chev").replace("<svg", '<svg class="chev"')}</a>`;
    });
    const half = Math.ceil(cats.length / 2);
    return shell("home", `
      <section class="hero">
        <div class="basmala">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</div>
        <h1>أذكار إسلامية</h1>
        <div class="latin">${lang === "fr" ? "Islamic Azkar · Citadelle du musulman" : "Islamic Azkar · Hisn al-Muslim"}</div>
        <p>${t.tagline}</p>
        <div class="ornament"><i></i>${svg("spark", true).replace("<svg", '<svg width="12" height="12"')}<i></i></div>
        <span class="date">${esc(hijri())}</span>
      </section>
      <label class="search glass">${svg("search")}<input id="q" type="search" placeholder="${t.search}" autocomplete="off"></label>
      <div id="results"></div>
      <div id="home-body">
        <div class="section">${t.recommended}</div>
        <div class="tiles">${tiles}</div>
        <div class="section">${t.categories}</div>
        <div class="cols"><div class="group glass">${cats.slice(0, half).join("")}</div><div class="group glass">${cats.slice(half).join("")}</div></div>
        <div class="section">${t.getApp}</div>
        <div class="cols">
          <div class="promo glass app-card">
            <div class="app-head"><img src="img/icon.png" alt=""><div><b>Islamic Azkar</b><p>${t.appText}</p></div></div>
            <div class="app-acts">
              <a class="pill primary press" href="${APK_URL}" rel="noopener">${svg("download")}${t.apk}</a>
              <button class="pill press" id="join-toggle" aria-expanded="false" aria-controls="join">${svg("mail")}${t.join}</button>
            </div>
            <small class="apk-note">${t.apkNote}</small>
            <form id="join" class="join" hidden novalidate>
              <p>${t.joinText}</p>
              <div class="join-row">
                <input type="email" name="email" required autocomplete="email" inputmode="email" dir="ltr" placeholder="${t.emailPh}" aria-label="Gmail">
                <input type="text" name="website" tabindex="-1" autocomplete="off" class="hp" aria-hidden="true">
                <button class="pill primary press" type="submit">${t.send}</button>
              </div>
              <small class="join-msg" role="status"></small>
            </form>
          </div>
          <a class="promo glass press" href="https://www.mushafapp.com" target="_blank" rel="noopener"><span class="mushaf">مصحف</span><div><b style="color:var(--text)">${t.mushaf}</b><p>${t.mushafText}</p><span style="font-weight:650">${t.mushafCta}</span></div></a>
        </div>
      </div>`);
  }

  function category(key) {
    const t = s(), cat = data.categories.find(c => c.key === key);
    if (!cat) return notFound();
    const rows = cat.chapters.map((id, i) => {
      const c = index.chapters.get(id);
      const sub = lang === "ar" ? c.en : `<span class="ar-sub">${esc(c.ar)}</span>`;
      return `<a class="row press" href="#/read/c${id}"><span class="num">${num(i + 1)}</span><span class="t"><b>${esc(chapterTitle(c))}</b><small>${sub}</small></span><span class="meta">${num(c.items.length)}</span>${svg("chev").replace("<svg", '<svg class="chev"')}</a>`;
    }).join("");
    return shell("home", `<a class="back" href="#/">${svg("back")}${t.back}</a><h1 class="large">${t.cat[key]}</h1><p class="sub">${t.chapters(cat.chapters.length)}</p><div class="group glass">${rows}</div>`);
  }

  function progressOf(key) { const p = store.get("progress", {}); return (p.day === today() && p[key]) || {}; }
  function saveProgress(key, counts) { const p = store.get("progress", {}); const fresh = p.day === today() ? p : { day: today() }; fresh[key] = counts; store.set("progress", fresh); }

  function zikrCard(z, i, total, counts) {
    const t = s(), n = Math.max(1, z.n || 1), c = Math.min(counts[z.id] || 0, n), done = c >= n;
    const tr = z.tr && lang !== "ar" ? `<p class="tr">${esc(z.tr)}</p>` : "", m = meaning(z) ? `<p class="mean">${esc(meaning(z))}</p>` : "";
    return `<article class="zikr glass card ${done ? "done" : ""}" id="z-${z.id}" data-id="${z.id}" data-n="${n}">
      <header><span class="chip">${t.of(i + 1, total)}</span>${n > 1 ? `<span class="chip">× ${num(n)}</span>` : ""}
        <span class="acts"><button class="fav ${favs.has(z.id) ? "on" : ""}" data-act="fav" aria-label="${t.favorites}">${svg("heart", favs.has(z.id))}</button>
        <button data-act="copy" aria-label="copy">${svg("copy")}</button><button data-act="share" aria-label="share">${svg("share")}</button></span></header>
      <p class="ar">${esc(z.ar)}</p>${tr}${m}
      <footer>${counterHtml(n, c)}</footer></article>`;
  }
  function counterHtml(n, c) {
    const t = s();
    if (n >= BULK) return `<button class="counter done-btn press" data-act="count">${c >= n ? svg("check").replace("<svg", '<svg style="width:20px;height:20px"') : ""}${t.done}</button>`;
    const r = 15, L = 2 * Math.PI * r, f = c / n;
    const ring = `<svg viewBox="0 0 38 38"><circle cx="19" cy="19" r="${r}" fill="none" stroke="currentColor" stroke-opacity=".18" stroke-width="3.5"/>
      <circle cx="19" cy="19" r="${r}" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-dasharray="${L}" stroke-dashoffset="${L * (1 - f)}" transform="rotate(-90 19 19)"/>
      ${c >= n ? `<path d="m13 19.5 4 4 8-8.5" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>` : `<text x="19" y="23.5" text-anchor="middle" font-size="12" font-weight="700" fill="currentColor">${num(n - c)}</text>`}</svg>`;
    return `<button class="counter press" data-act="count">${ring}<span>${c >= n ? t.done : `${num(c)} / ${num(n)}`}</span></button>`;
  }

  function reader(key, focus) {
    const t = s(), r = readingOf(key);
    if (!r) return notFound();
    const counts = progressOf(key);
    const cards = r.items.map((z, i) => zikrCard(z, i, r.items.length, counts)).join("");
    const backHref = r.chapter ? `#/cat/${(data.categories.find(c => c.chapters.includes(r.chapter.id)) || {}).key}` : "#/";
    return shell("read", `
      <div class="reader-head glass"><a class="back" href="${backHref}">${svg("back")}${t.back}</a><span class="progress"><i id="prog"></i></span><span class="prog-label" id="prog-label"></span></div>
      <h1 class="large">${esc(r.title)}</h1><p class="sub">${t.hisn} · ${t.azkar(r.items.length)}</p>
      <div id="cards" data-key="${key}" data-focus="${focus || ""}">${cards}</div>`);
  }

  function favorites() {
    const t = s(), items = [...favs].map(id => index.items.get(id)).filter(Boolean);
    if (!items.length) return shell("fav", `<h1 class="large">${t.favorites}</h1><div class="empty glass card">${svg("heart").replace("<svg", '<svg style="width:40px;height:40px;color:var(--text3)"')}<p>${t.noFav}</p></div>`);
    const rows = items.map(({ z, key, title }) => `<a class="row press" href="#/read/${key}/${z.id}"><span class="sym" style="background:#ff3b30">${svg("heart", true)}</span><span class="t"><b style="font-family:Amiri,serif;font-size:18px">${esc(z.ar.slice(0, 90))}${z.ar.length > 90 ? "…" : ""}</b><small>${esc(title())}</small></span>${svg("chev").replace("<svg", '<svg class="chev"')}</a>`).join("");
    return shell("fav", `<h1 class="large">${t.favorites}</h1><p class="sub">${t.azkar(items.length)}</p><div class="group glass">${rows}</div>`);
  }

  function tasbih() {
    const t = s(), st = Object.assign({ phrase: 0, target: 33, count: 0, total: 0 }, store.get("tasbih", {}));
    const r = 46, L = 2 * Math.PI * r, f = st.target ? Math.min(st.count / st.target, 1) : 0;
    return shell("tasbih", `<h1 class="large">${t.tasbih}</h1><p class="sub">${t.total(st.total)}</p>
      <div class="glass card tasbih">
        <div class="phrases">${PHRASES.map((p, i) => `<button class="press ${i === st.phrase ? "on" : ""}" data-phrase="${i}">${p}</button>`).join("")}</div>
        <button class="bead press" id="bead" aria-label="${t.tapCount}">
          <svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="${r}" fill="none" stroke="var(--fill)" stroke-width="4"/>
          <circle id="bead-ring" cx="50" cy="50" r="${r}" fill="none" stroke="var(--accent)" stroke-width="4" stroke-linecap="round" stroke-dasharray="${L}" stroke-dashoffset="${L * (1 - f)}"/></svg>
          <span><span class="ph">${PHRASES[st.phrase]}</span><div class="n" id="bead-n">${num(st.count)}</div><div class="of">${st.target ? (lang === "ar" ? `من ${num(st.target)}` : lang === "fr" ? `sur ${st.target}` : `of ${st.target}`) : t.tapCount}</div></span>
        </button>
        <div class="seg" style="margin-top:14px">${[33, 99, 100, 0].map(n => `<button data-target="${n}" class="${n === st.target ? "on" : ""}">${n ? num(n) : "∞"}</button>`).join("")}</div>
        <div style="margin-top:16px"><button class="btn ghost press" id="tasbih-reset">${svg("reset").replace("<svg", '<svg style="width:18px;height:18px"')}${t.reset}</button></div>
      </div>`);
  }

  function notFound() { return shell("home", `<div class="empty">404 · <a href="#/">${s().home}</a></div>`); }

  // ───────────── Behaviour ─────────────
  function toast(msg) { const el = document.createElement("div"); el.className = "toast glass"; el.textContent = msg; document.body.appendChild(el); setTimeout(() => el.remove(), 1800); }
  function vibrate(ms) { try { navigator.vibrate && navigator.vibrate(ms); } catch { /* unsupported */ } }

  function updateProgress(key) {
    const r = readingOf(key), counts = progressOf(key);
    const done = r.items.filter(z => (counts[z.id] || 0) >= Math.max(1, z.n || 1)).length;
    const bar = document.getElementById("prog"); if (bar) bar.style.width = `${(done / r.items.length) * 100}%`;
    const lab = document.getElementById("prog-label"); if (lab) lab.textContent = s().of(done, r.items.length);
    return done === r.items.length;
  }

  function count(card, key) {
    const id = +card.dataset.id, n = +card.dataset.n, counts = progressOf(key);
    let c = counts[id] || 0;
    if (c >= n) return;
    c = n >= BULK ? n : c + 1;
    counts[id] = c; saveProgress(key, counts); vibrate(c >= n ? [12, 40, 12] : 8);
    card.querySelector("footer").innerHTML = counterHtml(n, c);
    if (c >= n) {
      card.classList.add("done");
      const next = [...document.querySelectorAll(".zikr")].find(el => !el.classList.contains("done"));
      if (next) setTimeout(() => next.scrollIntoView({ behavior: "smooth", block: "center" }), 280);
    }
    if (updateProgress(key)) celebrate(key);
  }

  function celebrate(key) {
    const t = s(), r = readingOf(key), counts = progressOf(key);
    if (counts.__celebrated) return;
    counts.__celebrated = true; saveProgress(key, counts);
    const st = store.get("streaks", {}), prev = st[key] || {}, d = today();
    const y = new Date(); y.setDate(y.getDate() - 1);
    const yd = `${y.getFullYear()}-${y.getMonth() + 1}-${y.getDate()}`;
    const streak = prev.last === d ? prev.n : prev.last === yd ? prev.n + 1 : 1;
    st[key] = { last: d, n: streak }; store.set("streaks", st);
    const reps = r.items.reduce((a, z) => a + Math.max(1, z.n || 1), 0);
    const e = ENCOURAGEMENTS[Math.floor(Math.random() * ENCOURAGEMENTS.length)];
    const next = key === "morning" ? t.seeEvening : key === "evening" ? t.seeMorning : "";
    const scrim = document.createElement("div");
    scrim.className = "scrim";
    scrim.innerHTML = `<div class="sheet glass" role="dialog" aria-modal="true">
      <div class="check">${svg("check")}</div>
      <h2>${t.congrats}</h2><div style="color:var(--text2)">${t.completed(esc(r.title))}</div>
      <div style="margin-top:10px;display:flex;gap:8px;justify-content:center;flex-wrap:wrap"><span class="chip">${t.azkar(r.items.length)}</span><span class="chip">${t.reps(reps)}</span></div>
      ${streak > 1 ? `<span class="streak">${svg("spark", true).replace("<svg", '<svg style="width:14px;height:14px"')}${t.streak(streak)}</span>` : ""}
      <div class="ornament" style="margin:18px auto 8px"><i></i>✦<i></i></div>
      <div style="color:var(--text2);font-size:13px;font-weight:600">${t.prayerFor}</div>
      <div class="prayer">${PRAYER_AR}</div>${PRAYER_MEANING[lang] ? `<div>${PRAYER_MEANING[lang]}</div>` : ""}
      <div class="enc"><div class="a">${e[0]}</div>${lang === "ar" ? "" : `<div style="color:var(--text2);font-size:14px">${lang === "fr" ? e[3] : e[2]}</div>`}<div style="color:var(--text3);font-size:12px;margin-top:4px">${lang === "ar" ? e[1] : e[4]}</div></div>
      ${next ? `<p style="color:var(--text2)">${next}</p>` : ""}
      <button class="btn press" style="width:100%;margin-top:14px" id="close-sheet">${t.alhamdulillah}</button></div>`;
    document.body.appendChild(scrim);
    const close = () => scrim.remove();
    scrim.addEventListener("click", ev => { if (ev.target === scrim || ev.target.id === "close-sheet") close(); });
    vibrate([20, 60, 20, 60, 30]);
  }

  function openSettings() {
    const t = s();
    const scrim = document.createElement("div");
    scrim.className = "scrim";
    const render = () => {
      scrim.innerHTML = `<div class="sheet glass settings-sheet" role="dialog" aria-modal="true">
        <h2 style="margin-top:0">${t.settings}</h2>
        <div class="row" style="display:block"><b>${t.textSize}</b><input type="range" min="0.8" max="1.6" step="0.1" value="${prefs.scale}" id="scale">
          <p class="ar" style="margin:0">سُبْحَانَ اللَّهِ وَبِحَمْدِهِ</p></div>
        ${lang === "ar" ? "" : `<button class="row" data-toggle="tr" style="width:100%"><span class="t"><b>${t.showTr}</b></span><span class="switch ${prefs.tr ? "on" : ""}"></span></button>
        <button class="row" data-toggle="mean" style="width:100%"><span class="t"><b>${t.showMean}</b></span><span class="switch ${prefs.mean ? "on" : ""}"></span></button>`}
        <div class="row" style="display:block"><b>${t.theme}</b><div class="seg" style="margin-top:8px;display:flex">${["system", "light", "dark"].map(k => `<button style="flex:1" data-theme-set="${k}" class="${prefs.theme === k ? "on" : ""}">${t[k]}</button>`).join("")}</div></div>
        <button class="btn press" style="width:100%;margin-top:12px" id="close-sheet">${t.close}</button></div>`;
    };
    render();
    document.body.appendChild(scrim);
    scrim.addEventListener("input", ev => { if (ev.target.id === "scale") { prefs.scale = +ev.target.value; applyPrefs(); } });
    scrim.addEventListener("click", ev => {
      const el = ev.target.closest("button");
      if (ev.target === scrim || (el && el.id === "close-sheet")) return scrim.remove();
      if (!el) return;
      if (el.dataset.toggle) { prefs[el.dataset.toggle] = !prefs[el.dataset.toggle]; applyPrefs(); render(); }
      if (el.dataset.themeSet) { prefs.theme = el.dataset.themeSet; applyPrefs(); render(); }
    });
  }

  function search(q) {
    const t = s(), box = document.getElementById("results"), body = document.getElementById("home-body");
    if (!q.trim()) { box.innerHTML = ""; body.style.display = ""; return; }
    body.style.display = "none";
    const nq = normalize(q.trim());
    const chapters = data.chapters.filter(c => [c.ar, c.en, c.fr].some(x => x && normalize(x).includes(nq))).slice(0, 20);
    const items = [];
    for (const [id, it] of index.items) {
      const z = it.z;
      if ([z.ar, z.en, z.fr, z.tr].some(x => x && normalize(x).includes(nq))) items.push(it);
      if (items.length >= 40) break;
    }
    if (!chapters.length && !items.length) { box.innerHTML = `<div class="empty">${t.noResults}</div>`; return; }
    const crows = chapters.map(c => `<a class="row press" href="#/read/c${c.id}"><span class="num">${num(c.items.length)}</span><span class="t"><b>${esc(chapterTitle(c))}</b></span>${svg("chev").replace("<svg", '<svg class="chev"')}</a>`).join("");
    const irows = items.map(({ z, key, title }) => `<a class="row press" href="#/read/${key}/${z.id}"><span class="t"><b style="font-family:Amiri,serif;font-size:18px">${esc(z.ar.slice(0, 110))}${z.ar.length > 110 ? "…" : ""}</b><small>${esc(title())}</small></span>${svg("chev").replace("<svg", '<svg class="chev"')}</a>`).join("");
    box.innerHTML = `<div class="section">${t.results}</div>${crows ? `<div class="group glass" style="margin-bottom:14px">${crows}</div>` : ""}${irows ? `<div class="group glass">${irows}</div>` : ""}`;
  }

  // ───────────── Router ─────────────
  function route() {
    const parts = (location.hash.replace(/^#\/?/, "") || "").split("/").filter(Boolean);
    let html, after = () => {};
    if (!parts.length) html = home();
    else if (parts[0] === "cat") html = category(parts[1]);
    else if (parts[0] === "read") {
      html = reader(parts[1], parts[2]);
      after = () => {
        const key = parts[1]; updateProgress(key);
        const f = parts[2] && document.getElementById(`z-${parts[2]}`);
        if (f) { f.classList.add("focus"); setTimeout(() => f.scrollIntoView({ behavior: "smooth", block: "center" }), 60); }
      };
    } else if (parts[0] === "fav") html = favorites();
    else if (parts[0] === "tasbih") html = tasbih();
    else html = notFound();
    app.innerHTML = html;
    if (!parts[2]) window.scrollTo(0, 0);
    after();
  }

  app.addEventListener("click", ev => {
    const btn = ev.target.closest("button");
    const langBtn = ev.target.closest("[data-lang]");
    if (langBtn) { lang = langBtn.dataset.lang; applyLang(); route(); return; }
    if (btn && btn.id === "open-settings") return openSettings();
    if (btn && btn.id === "join-toggle") {
      const form = document.getElementById("join"), open = form.hidden;
      form.hidden = !open; btn.setAttribute("aria-expanded", open); btn.classList.toggle("on", open);
      if (open) form.querySelector("input[name=email]").focus();
      return;
    }

    const card = ev.target.closest(".zikr");
    if (card) {
      const key = document.getElementById("cards").dataset.key, id = +card.dataset.id, act = btn && btn.dataset.act;
      const z = index.items.get(id)?.z;
      if (act === "fav") {
        favs.has(id) ? favs.delete(id) : favs.add(id); store.set("favs", [...favs]);
        btn.classList.toggle("on", favs.has(id)); btn.innerHTML = svg("heart", favs.has(id)); toast(favs.has(id) ? s().added : s().removed); return;
      }
      if (act === "copy" || act === "share") {
        const text = `${z.ar}\n\n${meaning(z) ? meaning(z) + "\n\n" : ""}— ${s().appName}`;
        if (act === "share" && navigator.share) { navigator.share({ text }).catch(() => {}); return; }
        navigator.clipboard?.writeText(text).then(() => toast(s().copied)); return;
      }
      if (!ev.target.closest("a")) count(card, key);
      return;
    }

    // Tasbih
    const st = Object.assign({ phrase: 0, target: 33, count: 0, total: 0 }, store.get("tasbih", {}));
    if (btn && btn.dataset.phrase !== undefined) { st.phrase = +btn.dataset.phrase; st.count = 0; store.set("tasbih", st); route(); return; }
    if (btn && btn.dataset.target !== undefined) { st.target = +btn.dataset.target; st.count = 0; store.set("tasbih", st); route(); return; }
    if (btn && btn.id === "tasbih-reset") { st.count = 0; store.set("tasbih", st); route(); return; }
    if (ev.target.closest("#bead")) {
      st.count += 1; st.total += 1;
      const hit = st.target && st.count === st.target;
      vibrate(hit ? [20, 50, 20] : 8);
      if (st.target && st.count > st.target) st.count = 1;
      store.set("tasbih", st);
      document.getElementById("bead-n").textContent = num(st.count);
      const ring = document.getElementById("bead-ring"), L = 2 * Math.PI * 46;
      ring.setAttribute("stroke-dashoffset", L * (1 - (st.target ? st.count / st.target : 0)));
      document.querySelector(".sub").textContent = s().total(st.total);
    }
  });
  app.addEventListener("input", ev => { if (ev.target.id === "q") search(ev.target.value); });
  app.addEventListener("submit", async ev => {
    if (ev.target.id !== "join") return;
    ev.preventDefault();
    const form = ev.target, t = s(), msg = form.querySelector(".join-msg"), send = form.querySelector("button[type=submit]");
    const email = form.email.value.trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) { msg.textContent = t.badEmail; msg.className = "join-msg err"; form.email.focus(); return; }
    send.disabled = true; send.textContent = t.sending; msg.textContent = ""; msg.className = "join-msg";
    try {
      const r = await fetch(REGISTER_URL, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ email, lang, website: form.website.value }) });
      if (!r.ok) throw new Error(r.status);
      form.innerHTML = `<p class="join-done">${svg("check")}<span>${t.thanks}</span></p>`;
    } catch {
      msg.textContent = t.failed; msg.className = "join-msg err"; send.disabled = false; send.textContent = t.send;
    }
  });
  window.addEventListener("hashchange", route);

  // ───────────── Boot ─────────────
  applyLang(); applyPrefs();
  app.innerHTML = `<div class="loading">…</div>`;
  fetch("data/azkar.json").then(r => r.json()).then(d => {
    data = d;
    index = { chapters: new Map(d.chapters.map(c => [c.id, c])), items: new Map() };
    const add = (z, key, title) => index.items.set(z.id, { z, key, title });
    d.morning.forEach(z => add(z, "morning", () => s().morning));
    d.evening.forEach(z => add(z, "evening", () => s().evening));
    d.chapters.forEach(c => c.items.forEach(z => add(z, `c${c.id}`, () => chapterTitle(c))));
    route();
  }).catch(() => { app.innerHTML = `<div class="loading">Could not load azkar.</div>`; });
})();
