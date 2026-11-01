import Foundation

/// All user-facing text in the three supported languages (mirrors the Android app).
struct L10n {
    let appName, book, begin: String
    let tabHome, tabFavorites, tabTasbih, tabSettings: String
    let greetingMorning, greetingAfternoon, greetingEvening, greetingNight: String
    let morningAzkar, eveningAzkar, sleepAzkar, afterPrayer: String
    let recommendedNow, start, searchHint, categories, noResults: String
    let chapters: (Int) -> String
    let azkar: (Int) -> String
    let repeatTimes: (Int) -> String
    let completed, allDone, restart, share, copy, copied: String
    let noFavorites, noFavoritesHint: String
    let catNames: [String: String]
    let tasbih, target, total, reset, tapAnywhere: String
    let settings, language, appearance, themeSystem, themeLight, themeDark: String
    let reading, textSize, showTranslit, showMeaning, haptics: String
    let notifications, morningReminder, eveningReminder, generalReminders, generalRemindersHint: String
    let every, between, and, time, permissionDenied: String
    let widgets, widgetsHint, widgetDhikr, widgetQuick, widgetTasbih: String
    let about, aboutText, translationNote: String
    let shareImage, style, includeMeaning, shareNow: String
    let styleNames: [String]
    let notifMorningTitle, notifMorningBody, notifEveningTitle, notifEveningBody, notifGeneralTitle: String
    let done, chooseReading: String

    func category(_ key: String) -> String { catNames[key] ?? key }

    static func of(_ lang: Lang) -> L10n {
        switch lang {
        case .ar: return ar
        case .en: return en
        case .fr: return fr
        }
    }

    private static func arCount(_ n: Int, _ one: String, _ two: String, _ few: String, _ many: String) -> String {
        switch n {
        case 1: return one
        case 2: return two
        case 3...10: return "\(Lang.ar.num(n)) \(few)"
        default: return "\(Lang.ar.num(n)) \(many)"
        }
    }

    static let ar = L10n(
        appName: "أذكار إسلامية", book: "حصن المسلم", begin: "ابدأ",
        tabHome: "الرئيسية", tabFavorites: "المفضلة", tabTasbih: "المسبحة", tabSettings: "الإعدادات",
        greetingMorning: "صباح الخير", greetingAfternoon: "طاب يومك", greetingEvening: "مساء الخير", greetingNight: "ليلة طيبة",
        morningAzkar: "أذكار الصباح", eveningAzkar: "أذكار المساء", sleepAzkar: "أذكار النوم", afterPrayer: "أذكار الصلاة",
        recommendedNow: "مقترح الآن", start: "ابدأ الآن", searchHint: "ابحث في الأذكار والأدعية", categories: "الأقسام", noResults: "لا توجد نتائج",
        chapters: { arCount($0, "باب واحد", "بابان", "أبواب", "بابًا") },
        azkar: { arCount($0, "ذكر واحد", "ذكران", "أذكار", "ذكرًا") },
        repeatTimes: { n in n == 1 ? "مرة" : n == 2 ? "مرتان" : (3...10).contains(n) ? "\(Lang.ar.num(n)) مرات" : "\(Lang.ar.num(n)) مرة" },
        completed: "تمّ", allDone: "أتممت الأذكار، تقبّل الله منك", restart: "إعادة", share: "مشاركة", copy: "نسخ", copied: "تم النسخ",
        noFavorites: "لا توجد أذكار مفضلة بعد", noFavoritesHint: "اضغط على القلب في أي ذكر لحفظه هنا",
        catNames: ["prayer": "الصلاة والمسجد", "sleep": "النوم والاستيقاظ", "home": "البيت والطهارة", "hardship": "الكرب والحماية",
                   "illness": "المرض والجنائز", "food": "الطعام والصيام", "family": "الأسرة والمعاملات", "travel": "السفر",
                   "nature": "الطبيعة والأحوال", "hajj": "الحج والعمرة", "sunnah": "أدعية من السنة", "praise": "الذكر والاستغفار"],
        tasbih: "المسبحة", target: "الهدف", total: "المجموع", reset: "تصفير", tapAnywhere: "اضغط على الدائرة للتسبيح",
        settings: "الإعدادات", language: "اللغة", appearance: "المظهر", themeSystem: "تلقائي", themeLight: "فاتح", themeDark: "داكن",
        reading: "القراءة", textSize: "حجم الخط", showTranslit: "إظهار النطق اللاتيني", showMeaning: "إظهار الترجمة", haptics: "الاهتزاز عند العدّ",
        notifications: "التنبيهات", morningReminder: "تذكير أذكار الصباح", eveningReminder: "تذكير أذكار المساء",
        generalReminders: "تذكير بالذكر", generalRemindersHint: "ذكر قصير يصلك على مدار اليوم",
        every: "كل", between: "من", and: "إلى", time: "الوقت", permissionDenied: "يرجى السماح بالإشعارات من الإعدادات",
        widgets: "أدوات الشاشة الرئيسية",
        widgetsHint: "اضغط مطولًا على الشاشة الرئيسية، ثم «تعديل» ← «إضافة أداة»، وابحث عن «أذكار إسلامية».",
        widgetDhikr: "ذكر اليوم", widgetQuick: "أذكار سريعة", widgetTasbih: "المسبحة",
        about: "عن التطبيق", aboutText: "الأذكار من كتاب «حصن المسلم» للشيخ سعيد بن علي بن وهف القحطاني رحمه الله.",
        translationNote: "الترجمة الفرنسية اجتهادية، ويُستحسن مراجعتها مع أهل العلم.",
        shareImage: "مشاركة كصورة", style: "النمط", includeMeaning: "إضافة الترجمة", shareNow: "مشاركة",
        styleNames: ["زمرّدي", "فجر", "ليل", "رمل"],
        notifMorningTitle: "أذكار الصباح", notifMorningBody: "ابدأ يومك بذكر الله ☀️",
        notifEveningTitle: "أذكار المساء", notifEveningBody: "حصّن مساءك بذكر الله 🌙",
        notifGeneralTitle: "ألا بذكر الله تطمئن القلوب",
        done: "تم", chooseReading: "اختر بابًا للقراءة"
    )

    static let en = L10n(
        appName: "Islamic Azkar", book: "Hisn Al Muslim", begin: "Begin",
        tabHome: "Home", tabFavorites: "Favorites", tabTasbih: "Tasbih", tabSettings: "Settings",
        greetingMorning: "Good morning", greetingAfternoon: "Good afternoon", greetingEvening: "Good evening", greetingNight: "Peaceful night",
        morningAzkar: "Morning Azkar", eveningAzkar: "Evening Azkar", sleepAzkar: "Before Sleep", afterPrayer: "Prayer",
        recommendedNow: "Recommended now", start: "Start now", searchHint: "Search azkar and duas", categories: "Categories", noResults: "No results",
        chapters: { $0 == 1 ? "1 chapter" : "\($0) chapters" },
        azkar: { $0 == 1 ? "1 dhikr" : "\($0) azkar" },
        repeatTimes: { $0 == 1 ? "once" : "\($0) times" },
        completed: "Done", allDone: "All completed — may Allah accept from you", restart: "Restart", share: "Share", copy: "Copy", copied: "Copied",
        noFavorites: "No favorites yet", noFavoritesHint: "Tap the heart on any dhikr to keep it here",
        catNames: ["prayer": "Prayer & Mosque", "sleep": "Sleep & Waking", "home": "Home & Purity", "hardship": "Distress & Protection",
                   "illness": "Illness & Funerals", "food": "Food & Fasting", "family": "Family & Social", "travel": "Travel",
                   "nature": "Nature & Weather", "hajj": "Hajj & Umrah", "sunnah": "Duas from the Sunnah", "praise": "Praise & Forgiveness"],
        tasbih: "Tasbih", target: "Target", total: "Total", reset: "Reset", tapAnywhere: "Tap the bead to count",
        settings: "Settings", language: "Language", appearance: "Appearance", themeSystem: "Auto", themeLight: "Light", themeDark: "Dark",
        reading: "Reading", textSize: "Text size", showTranslit: "Show transliteration", showMeaning: "Show translation", haptics: "Haptic feedback",
        notifications: "Notifications", morningReminder: "Morning azkar reminder", eveningReminder: "Evening azkar reminder",
        generalReminders: "Dhikr reminders", generalRemindersHint: "A short dhikr throughout your day",
        every: "Every", between: "From", and: "To", time: "Time", permissionDenied: "Please allow notifications in Settings",
        widgets: "Home Screen widgets",
        widgetsHint: "Touch and hold the Home Screen, tap Edit → Add Widget, then search for “Islamic Azkar”.",
        widgetDhikr: "Dhikr of the day", widgetQuick: "Quick azkar", widgetTasbih: "Tasbih",
        about: "About", aboutText: "Azkar from “Hisn Al Muslim” (Fortress of the Muslim) by Sheikh Saʿīd ibn ʿAlī ibn Wahf Al-Qaḥṭānī.",
        translationNote: "The French translation is an independent rendering and should be reviewed by a qualified scholar.",
        shareImage: "Share as image", style: "Style", includeMeaning: "Include translation", shareNow: "Share",
        styleNames: ["Emerald", "Dawn", "Midnight", "Sand"],
        notifMorningTitle: "Morning Azkar", notifMorningBody: "Begin your day with the remembrance of Allah ☀️",
        notifEveningTitle: "Evening Azkar", notifEveningBody: "Protect your evening with the remembrance of Allah 🌙",
        notifGeneralTitle: "Hearts find rest in the remembrance of Allah",
        done: "Done", chooseReading: "Choose a chapter to read"
    )

    static let fr = L10n(
        appName: "Islamic Azkar", book: "La Citadelle du musulman", begin: "Commencer",
        tabHome: "Accueil", tabFavorites: "Favoris", tabTasbih: "Tasbih", tabSettings: "Réglages",
        greetingMorning: "Bonjour", greetingAfternoon: "Bon après-midi", greetingEvening: "Bonsoir", greetingNight: "Douce nuit",
        morningAzkar: "Adhkar du matin", eveningAzkar: "Adhkar du soir", sleepAzkar: "Avant de dormir", afterPrayer: "La prière",
        recommendedNow: "Recommandé maintenant", start: "Commencer", searchHint: "Rechercher des invocations", categories: "Catégories", noResults: "Aucun résultat",
        chapters: { $0 == 1 ? "1 chapitre" : "\($0) chapitres" },
        azkar: { $0 == 1 ? "1 invocation" : "\($0) invocations" },
        repeatTimes: { $0 == 1 ? "une fois" : "\($0) fois" },
        completed: "Terminé", allDone: "Tout est terminé — qu'Allah accepte de vous", restart: "Recommencer", share: "Partager", copy: "Copier", copied: "Copié",
        noFavorites: "Aucun favori pour l'instant", noFavoritesHint: "Touchez le cœur d'une invocation pour la garder ici",
        catNames: ["prayer": "Prière & mosquée", "sleep": "Sommeil & réveil", "home": "Maison & purification", "hardship": "Détresse & protection",
                   "illness": "Maladie & funérailles", "food": "Repas & jeûne", "family": "Famille & société", "travel": "Voyage",
                   "nature": "Nature & météo", "hajj": "Hajj & Omra", "sunnah": "Invocations de la Sunna", "praise": "Louange & pardon"],
        tasbih: "Tasbih", target: "Objectif", total: "Total", reset: "Remettre à zéro", tapAnywhere: "Touchez la perle pour compter",
        settings: "Réglages", language: "Langue", appearance: "Apparence", themeSystem: "Auto", themeLight: "Clair", themeDark: "Sombre",
        reading: "Lecture", textSize: "Taille du texte", showTranslit: "Afficher la translittération", showMeaning: "Afficher la traduction", haptics: "Retour haptique",
        notifications: "Notifications", morningReminder: "Rappel des adhkar du matin", eveningReminder: "Rappel des adhkar du soir",
        generalReminders: "Rappels de dhikr", generalRemindersHint: "Un court dhikr au fil de la journée",
        every: "Toutes les", between: "De", and: "À", time: "Heure", permissionDenied: "Veuillez autoriser les notifications dans Réglages",
        widgets: "Widgets de l'écran d'accueil",
        widgetsHint: "Touchez longuement l'écran d'accueil, puis Modifier → Ajouter un widget, et cherchez « Islamic Azkar ».",
        widgetDhikr: "Dhikr du jour", widgetQuick: "Adhkar rapides", widgetTasbih: "Tasbih",
        about: "À propos", aboutText: "Invocations tirées de « Hisn Al Muslim » (La Citadelle du musulman) du Cheikh Saʿîd ibn ʿAlî ibn Wahf Al-Qahtânî.",
        translationNote: "La traduction française est une adaptation indépendante ; il est conseillé de la faire relire par une personne de science.",
        shareImage: "Partager en image", style: "Style", includeMeaning: "Inclure la traduction", shareNow: "Partager",
        styleNames: ["Émeraude", "Aube", "Nuit", "Sable"],
        notifMorningTitle: "Adhkar du matin", notifMorningBody: "Commencez votre journée par le rappel d'Allah ☀️",
        notifEveningTitle: "Adhkar du soir", notifEveningBody: "Protégez votre soirée par le rappel d'Allah 🌙",
        notifGeneralTitle: "C'est par le rappel d'Allah que les cœurs s'apaisent",
        done: "OK", chooseReading: "Choisissez un chapitre"
    )
}

/// SF Symbol per category.
func categorySymbol(_ key: String) -> String {
    switch key {
    case "prayer": return "hands.and.sparkles.fill"
    case "sleep": return "moon.zzz.fill"
    case "home": return "house.fill"
    case "hardship": return "shield.lefthalf.filled"
    case "illness": return "cross.case.fill"
    case "food": return "fork.knife"
    case "family": return "person.3.fill"
    case "travel": return "airplane"
    case "nature": return "cloud.bolt.rain.fill"
    case "hajj": return "location.north.circle.fill"
    case "sunnah": return "book.closed.fill"
    default: return "sparkles"
    }
}
