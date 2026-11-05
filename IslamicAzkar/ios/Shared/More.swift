import Foundation

/// Texts for prayer times, the completion card, widget styling and the widget gallery
/// (mirrors the Android app's More and Strings additions).
struct More {
    let prayerTimes: String
    let prayers: [String]
    let nextPrayer: String
    let location, notSet, useMyLocation, locating, locationFailed, chooseCity, myLocation: String
    let method: String
    let methods: [String]
    let asrHanafi, asrHint, setLocationWidget: String
    let widgetStyle, palette, calligraphy, hijriOffset, hijriOffsetHint: String
    let days: (Int) -> String
    let sources: [String]
    let day, month, year, hijriYear: String
    let ourApps, mushafTitle, mushafText, mushafCta: String
    let today: String
    // Completion card
    let congratsTitle: String
    let congratsDone: (String) -> String
    let repetitions: (Int) -> String
    let prayerLabel, prayerMeaning: String
    let streak: (Int) -> String
    let seeYouEvening, seeYouMorning, alhamdulillah: String
    // Widget gallery
    let tabWidgets, widgetsTitle, widgetsHint, add, howToAdd: String
    let addSteps: [String]
    let lockScreenHint: String
    let groups: [String]           // prayer & time, azkar, quran & wisdom, tasbih
    let dayAzkar, nightAzkar: String
    let widgetNames: [String: String]
    let widgetDescriptions: [String: String]
    let counter, weeklyStats: String
    let roundsDone: (String) -> String
    let in_: String

    static func of(_ lang: Lang) -> More {
        switch lang { case .ar: ar; case .en: en; case .fr: fr }
    }

    static let ar = More(
        prayerTimes: "مواقيت الصلاة",
        prayers: ["الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"],
        nextPrayer: "الصلاة القادمة",
        location: "الموقع", notSet: "غير محدد", useMyLocation: "استخدام موقعي الحالي", locating: "جارٍ تحديد الموقع…",
        locationFailed: "تعذّر تحديد الموقع، اختر مدينة من القائمة", chooseCity: "أو اختر مدينة", myLocation: "موقعي",
        method: "طريقة الحساب",
        methods: ["رابطة العالم الإسلامي", "أم القرى (مكة)", "الهيئة المصرية العامة للمساحة", "أمريكا الشمالية (ISNA)", "جامعة العلوم الإسلامية، كراتشي", "الخليج", "دبي · دلوك (الشؤون الإسلامية بدبي)", "فرنسا (١٢°)", "تركيا (ديانت)"],
        asrHanafi: "العصر على المذهب الحنفي",
        asrHint: "يُحسب العصر عادةً حين يصير ظل الشيء مثله، وعند الحنفية مثليه.",
        setLocationWidget: "حدّد موقعك في التطبيق لعرض مواقيت الصلاة",
        widgetStyle: "مظهر الأدوات", palette: "الألوان", calligraphy: "الخط العربي", hijriOffset: "تعديل التاريخ الهجري",
        hijriOffsetHint: "عدّل التاريخ الهجري يوماً أو يومين ليوافق رؤية الهلال في بلدك.",
        days: { n in n == 0 ? "بدون تعديل" : (n > 0 ? "+" : "−") + Lang.ar.num(abs(n)) + " يوم" },
        sources: ["منوّع", "القرآن الكريم", "الحديث الشريف", "الأذكار", "الشعر", "الحِكَم"],
        day: "اليوم", month: "الشهر", year: "السنة", hijriYear: "السنة الهجرية",
        ourApps: "تطبيقاتنا", mushafTitle: "تطبيق المصحف",
        mushafText: "اقرأ القرآن الكريم بخط المصحف الشريف، صدقة جارية من فريقنا. حمّله مجاناً من موقعنا.",
        mushafCta: "تحميل من mushafapp.com", today: "اليوم",
        congratsTitle: "تقبّل الله منك", congratsDone: { "أتممتَ \($0)" },
        repetitions: { n in n == 1 ? "مرة واحدة" : n == 2 ? "مرتان" : (3...10).contains(n) ? "\(Lang.ar.num(n)) مرات" : "\(Lang.ar.num(n)) مرة" },
        prayerLabel: "دعاء لك", prayerMeaning: "",
        streak: { n in n <= 1 ? "اليوم الأول — واصل" : "\(Lang.ar.num(n)) أيام متتالية" },
        seeYouEvening: "نلقاك في أذكار المساء", seeYouMorning: "نلقاك غدًا في أذكار الصباح", alhamdulillah: "الحمد لله",
        tabWidgets: "الأدوات", widgetsTitle: "أدوات الشاشة الرئيسية",
        widgetsHint: "اختر الأداة التي تريدها، ثم أضفها إلى الشاشة الرئيسية أو شاشة القفل.",
        add: "إضافة", howToAdd: "إضافة أداة",
        addSteps: ["اضغط مطولًا على مكان فارغ في الشاشة الرئيسية حتى تهتز الأيقونات.",
                   "اضغط «تعديل» في الأعلى ثم «إضافة أداة».",
                   "ابحث عن «أذكار إسلامية» واختر الأداة والحجم، ثم «إضافة أداة»."],
        lockScreenHint: "لإضافة أداة إلى شاشة القفل: اضغط مطولًا على شاشة القفل، ثم «تخصيص» ← «شاشة القفل» واختر أدوات «أذكار إسلامية».",
        groups: ["الصلاة والوقت", "الأذكار", "القرآن والحكمة", "المسبحة"],
        dayAzkar: "أذكار اليوم", nightAzkar: "أذكار الليل",
        widgetNames: ["prayer": "مواقيت الصلاة", "hijri": "التاريخ الهجري", "clock": "الساعة الإسلامية", "digital": "الساعة الرقمية والتاريخ",
                      "progress": "مضيّ اليوم والشهر والسنة", "timeline": "أذكار حسب الوقت", "morning": "أذكار الصباح", "day": "أذكار اليوم",
                      "night": "أذكار الليل", "dhikr": "ذكر اليوم", "quick": "أذكار سريعة", "scripture": "قرآن وحديث وشعر وحِكَم",
                      "tasbih": "المسبحة", "counter": "العدّاد وإحصاء الأسبوع"],
        widgetDescriptions: ["prayer": "الصلاة القادمة مع العدّ التنازلي ومواقيت اليوم.", "hijri": "اليوم والشهر الهجري بخط عربي أصيل.",
                             "clock": "ساعة بعقارب ونجمة ثمانية وأرقام عربية.", "digital": "الوقت مع التاريخين الهجري والميلادي.",
                             "progress": "حلقات لمضيّ اليوم والشهر والسنتين.", "timeline": "تتبدّل الأذكار مع الوقت، صباحًا ونهارًا ومساءً وليلًا.",
                             "morning": "أذكار الصباح ذكرًا بعد ذكر.", "day": "أذكار النهار ذكرًا بعد ذكر.", "night": "أذكار المساء والنوم.",
                             "dhikr": "ذكر قصير يتجدّد كل ساعة.", "quick": "انتقل مباشرة إلى أذكار الصباح أو المساء أو النوم.",
                             "scripture": "آية أو حديث أو بيت شعر أو حكمة.", "tasbih": "سبّح مباشرة من الشاشة الرئيسية.",
                             "counter": "العدّاد مع تسبيحك خلال الأسبوع."],
        counter: "العدّاد", weeklyStats: "إحصاء الأسبوع", roundsDone: { "الدورات المكتملة: \($0)" }, in_: "بعد"
    )

    static let en = More(
        prayerTimes: "Prayer times",
        prayers: ["Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"],
        nextPrayer: "Next prayer",
        location: "Location", notSet: "Not set", useMyLocation: "Use my current location", locating: "Finding your location…",
        locationFailed: "Couldn’t find your location. Choose a city instead.", chooseCity: "Or choose a city", myLocation: "My location",
        method: "Calculation method",
        methods: ["Muslim World League", "Umm al-Qura (Makkah)", "Egyptian General Authority", "North America (ISNA)", "University of Islamic Sciences, Karachi", "Gulf region", "Dubai · Dulook DXB (IACAD)", "France (12°)", "Turkey (Diyanet)"],
        asrHanafi: "Hanafi Asr",
        asrHint: "Asr usually begins when an object’s shadow equals its length; the Hanafi school uses twice its length.",
        setLocationWidget: "Set your location in the app to see prayer times",
        widgetStyle: "Widget style", palette: "Colours", calligraphy: "Calligraphy", hijriOffset: "Hijri date adjustment",
        hijriOffsetHint: "Shift the Hijri date by a day or two to match the moon sighting where you live.",
        days: { n in n == 0 ? "None" : (n > 0 ? "+" : "−") + "\(abs(n))" + (abs(n) == 1 ? " day" : " days") },
        sources: ["Mixed", "Quran", "Hadith", "Azkar", "Poetry", "Wisdom"],
        day: "Day", month: "Month", year: "Year", hijriYear: "Hijri year",
        ourApps: "Our apps", mushafTitle: "Mushaf app",
        mushafText: "Read the Holy Quran in the script of the printed Mushaf, a sadaqa jariyah from our team. Download it free from our website.",
        mushafCta: "Download from mushafapp.com", today: "Today",
        congratsTitle: "May Allah accept from you", congratsDone: { "You completed \($0)" },
        repetitions: { $0 == 1 ? "1 repetition" : "\($0) repetitions" },
        prayerLabel: "A prayer for you",
        prayerMeaning: "May Allah make you among those who remember Him much, and keep your heart firm upon His remembrance.",
        streak: { $0 <= 1 ? "Day one — keep going" : "\($0) days in a row" },
        seeYouEvening: "See you for the Evening Azkar", seeYouMorning: "See you tomorrow for the Morning Azkar", alhamdulillah: "Alhamdulillah",
        tabWidgets: "Widgets", widgetsTitle: "Widgets",
        widgetsHint: "Pick the widget you like, then add it to your Home Screen or Lock Screen.",
        add: "Add", howToAdd: "Add a widget",
        addSteps: ["Touch and hold an empty spot on your Home Screen until the apps jiggle.",
                   "Tap Edit at the top, then Add Widget.",
                   "Search for “Islamic Azkar”, choose the widget and size, then tap Add Widget."],
        lockScreenHint: "For the Lock Screen: touch and hold the Lock Screen, tap Customize → Lock Screen, and pick the Islamic Azkar widgets.",
        groups: ["Prayer & time", "Azkar", "Quran & wisdom", "Tasbih"],
        dayAzkar: "Day Azkar", nightAzkar: "Night Azkar",
        widgetNames: ["prayer": "Prayer times", "hijri": "Hijri date", "clock": "Islamic clock", "digital": "Digital clock & date",
                      "progress": "Day, month & year progress", "timeline": "Azkar through the day", "morning": "Morning Azkar", "day": "Day Azkar",
                      "night": "Night Azkar", "dhikr": "Dhikr of the day", "quick": "Quick azkar", "scripture": "Quran, Hadith, Poetry & Wisdom",
                      "tasbih": "Tasbih", "counter": "Counter & weekly stats"],
        widgetDescriptions: ["prayer": "The next prayer with a countdown, and today’s times.", "hijri": "The Hijri day and month in classical calligraphy.",
                             "clock": "An analog clock with an eight-pointed star and Arabic numerals.", "digital": "The time with the Hijri and Gregorian dates.",
                             "progress": "Rings for how much of the day, month and years has passed.", "timeline": "Azkar that change with the time of day.",
                             "morning": "The morning azkar, one after another.", "day": "Azkar for the day, one after another.", "night": "Evening and bedtime azkar.",
                             "dhikr": "A short dhikr, refreshed every hour.", "quick": "Jump straight into morning, evening or sleep azkar.",
                             "scripture": "A verse, hadith, line of poetry or proverb.", "tasbih": "Count your tasbih right on the Home Screen.",
                             "counter": "The counter with your tasbih over the week."],
        counter: "Counter", weeklyStats: "Weekly stats", roundsDone: { "Rounds completed: \($0)" }, in_: "in"
    )

    static let fr = More(
        prayerTimes: "Horaires de prière",
        prayers: ["Fajr", "Lever du soleil", "Dhohr", "Asr", "Maghrib", "Icha"],
        nextPrayer: "Prochaine prière",
        location: "Lieu", notSet: "Non défini", useMyLocation: "Utiliser ma position actuelle", locating: "Recherche de votre position…",
        locationFailed: "Position introuvable. Choisissez plutôt une ville.", chooseCity: "Ou choisissez une ville", myLocation: "Ma position",
        method: "Méthode de calcul",
        methods: ["Ligue islamique mondiale", "Umm al-Qura (La Mecque)", "Autorité générale égyptienne", "Amérique du Nord (ISNA)", "Université des sciences islamiques, Karachi", "Région du Golfe", "Dubaï · Dulook DXB (IACAD)", "France (12°)", "Turquie (Diyanet)"],
        asrHanafi: "Asr selon l’école hanafite",
        asrHint: "L’Asr commence en général quand l’ombre d’un objet égale sa longueur ; l’école hanafite retient le double.",
        setLocationWidget: "Indiquez votre lieu dans l’application pour voir les horaires",
        widgetStyle: "Style des widgets", palette: "Couleurs", calligraphy: "Calligraphie", hijriOffset: "Ajustement de la date hégirienne",
        hijriOffsetHint: "Décalez la date hégirienne d’un ou deux jours selon l’observation du croissant chez vous.",
        days: { n in n == 0 ? "Aucun" : (n > 0 ? "+" : "−") + "\(abs(n))" + (abs(n) == 1 ? " jour" : " jours") },
        sources: ["Varié", "Coran", "Hadith", "Adhkar", "Poésie", "Sagesse"],
        day: "Jour", month: "Mois", year: "Année", hijriYear: "Année hégirienne",
        ourApps: "Nos applications", mushafTitle: "Application Mushaf",
        mushafText: "Lisez le Saint Coran dans l’écriture du Mushaf imprimé, une sadaqa jariya de notre équipe. Téléchargez-la gratuitement sur notre site.",
        mushafCta: "Télécharger sur mushafapp.com", today: "Aujourd’hui",
        congratsTitle: "Qu'Allah accepte de toi", congratsDone: { "Tu as terminé : \($0)" },
        repetitions: { $0 == 1 ? "1 répétition" : "\($0) répétitions" },
        prayerLabel: "Une invocation pour toi",
        prayerMeaning: "Qu'Allah fasse de toi l'un de ceux qui L'évoquent beaucoup, et qu'Il affermisse ton cœur dans Son rappel.",
        streak: { $0 <= 1 ? "Premier jour — continue" : "\($0) jours d'affilée" },
        seeYouEvening: "À ce soir pour les adhkar du soir", seeYouMorning: "À demain pour les adhkar du matin", alhamdulillah: "Al-hamdu lillâh",
        tabWidgets: "Widgets", widgetsTitle: "Widgets",
        widgetsHint: "Choisissez le widget qui vous plaît, puis ajoutez-le à l’écran d’accueil ou à l’écran verrouillé.",
        add: "Ajouter", howToAdd: "Ajouter un widget",
        addSteps: ["Touchez longuement un espace vide de l’écran d’accueil jusqu’à ce que les apps bougent.",
                   "Touchez Modifier en haut, puis Ajouter un widget.",
                   "Cherchez « Islamic Azkar », choisissez le widget et sa taille, puis Ajouter le widget."],
        lockScreenHint: "Pour l’écran verrouillé : touchez-le longuement, puis Personnaliser → Écran verrouillé, et choisissez les widgets Islamic Azkar.",
        groups: ["Prière & heure", "Adhkar", "Coran & sagesse", "Tasbih"],
        dayAzkar: "Adhkar de la journée", nightAzkar: "Adhkar de la nuit",
        widgetNames: ["prayer": "Horaires de prière", "hijri": "Date hégirienne", "clock": "Horloge islamique", "digital": "Horloge numérique & date",
                      "progress": "Progression du jour, du mois & de l’année", "timeline": "Adhkar au fil du jour", "morning": "Adhkar du matin",
                      "day": "Adhkar de la journée", "night": "Adhkar de la nuit", "dhikr": "Dhikr du jour", "quick": "Adhkar rapides",
                      "scripture": "Coran, hadith, poésie & sagesse", "tasbih": "Tasbih", "counter": "Compteur & statistiques"],
        widgetDescriptions: ["prayer": "La prochaine prière avec compte à rebours, et les horaires du jour.", "hijri": "Le jour et le mois hégiriens en calligraphie.",
                             "clock": "Une horloge à aiguilles, étoile à huit branches et chiffres arabes.", "digital": "L’heure avec les dates hégirienne et grégorienne.",
                             "progress": "Des anneaux pour l’avancée du jour, du mois et des années.", "timeline": "Des adhkar qui changent selon le moment de la journée.",
                             "morning": "Les adhkar du matin, l’un après l’autre.", "day": "Les adhkar de la journée, l’un après l’autre.", "night": "Adhkar du soir et du coucher.",
                             "dhikr": "Un court dhikr, renouvelé chaque heure.", "quick": "Accès direct aux adhkar du matin, du soir ou du coucher.",
                             "scripture": "Un verset, un hadith, un vers ou un proverbe.", "tasbih": "Comptez votre tasbih depuis l’écran d’accueil.",
                             "counter": "Le compteur avec votre tasbih de la semaine."],
        counter: "Compteur", weeklyStats: "Statistiques", roundsDone: { "Tours terminés : \($0)" }, in_: "dans"
    )
}

/// The prayer offered to the reader on completing a set of azkar.
let completionPrayer = "جَعَلَكَ اللَّهُ مِنَ الذَّاكِرِينَ اللَّهَ كَثِيرًا وَالذَّاكِرَاتِ، وَثَبَّتَ قَلْبَكَ عَلَى ذِكْرِهِ"

/// The dedication, shown in Arabic in every language.
let dedicationAr = "صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم"
let dedicationNamesAr = "عن محمد الكوسا وديمة غنوم وعائلاتهم"

func dedicationTranslation(_ lang: Lang) -> String? {
    switch lang {
    case .ar: nil
    case .en: "On behalf of Mohamad Al-Koussa, Dima Ghannoum and their families"
    case .fr: "Au nom de Mohamad Al-Koussa, Dima Ghannoum et leurs familles"
    }
}

let mushafURL = URL(string: "https://www.mushafapp.com")!
