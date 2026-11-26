import Foundation

/// Texts of the Apple TV screens that the shared L10n / More don't have (same wording as the
/// iPhone app's Record and Sync pages).
struct TVTexts {
    let record, recordSubtitle, streak, activeDays, finished, today, nothing: String
    let pressToCount, swipeHint, readAgain, backHome, round, now: String
    let city, phrase, syncTitle, syncIntro, turnOn, enterCode, link, yourCode, codeHint, syncNow, stop,
        badCode, offline, linked: String

    static func of(_ lang: Lang) -> TVTexts {
        switch lang {
        case .ar: return TVTexts(
            record: "سجلّي", recordSubtitle: "الأذكار التي أتممتها كل يوم", streak: "أيام متتالية",
            activeDays: "أيام الذكر هذا الشهر", finished: "أذكار أتممتها هذا الشهر", today: "اليوم",
            nothing: "لم تُتمّ أذكارًا اليوم بعد.",
            pressToCount: "اضغط للعدّ", swipeHint: "اسحب للذكر التالي أو السابق", readAgain: "اقرأ مرة أخرى",
            backHome: "الرئيسية", round: "دورة", now: "الآن",
            city: "المدينة", phrase: "الذكر",
            syncTitle: "المزامنة بين الأجهزة",
            syncIntro: "اعرض سجلّ أذكارك على كل أجهزتك: آيفون وآيباد وماك وأبل TV وأندرويد والموقع. بلا حساب ولا بريد: رمز خاص يربط أجهزتك.",
            turnOn: "تفعيل المزامنة", enterCode: "أدخل رمزًا من جهاز آخر", link: "ربط", yourCode: "رمز المزامنة",
            codeHint: "أدخل هذا الرمز على أجهزتك الأخرى: الإعدادات ← المزامنة. احتفظ به لنفسك.",
            syncNow: "زامن الآن", stop: "إيقاف المزامنة على هذا الجهاز",
            badCode: "الرمز غير صحيح. تأكد منه وحاول مرة أخرى.", offline: "تعذّر الاتصال. تحقق من الإنترنت وحاول مرة أخرى.",
            linked: "تم ربط هذا الجهاز")
        case .en: return TVTexts(
            record: "Record", recordSubtitle: "The azkar you completed each day", streak: "Days in a row",
            activeDays: "Days with azkar this month", finished: "Azkar completed this month", today: "Today",
            nothing: "No azkar completed today yet.",
            pressToCount: "Press to count", swipeHint: "Swipe for the next or previous dhikr", readAgain: "Read again",
            backHome: "Home", round: "Round", now: "Now",
            city: "City", phrase: "Dhikr",
            syncTitle: "Sync between devices",
            syncIntro: "See your azkar record on all your devices: iPhone, iPad, Mac, Apple TV, Android and the website. No account or email: a private code links your devices.",
            turnOn: "Turn on sync", enterCode: "Enter a code from another device", link: "Link", yourCode: "Sync code",
            codeHint: "Enter this code on your other devices in Settings → Sync. Keep it to yourself.",
            syncNow: "Sync now", stop: "Stop syncing on this device",
            badCode: "That code isn't right. Check it and try again.", offline: "Couldn't connect. Check your internet and try again.",
            linked: "This device is linked")
        case .fr: return TVTexts(
            record: "Historique", recordSubtitle: "Les adhkar terminés chaque jour", streak: "Jours d'affilée",
            activeDays: "Jours avec des adhkar ce mois-ci", finished: "Adhkar terminés ce mois-ci", today: "Aujourd'hui",
            nothing: "Aucun dhikr terminé aujourd'hui.",
            pressToCount: "Appuyez pour compter", swipeHint: "Balayez pour le dhikr suivant ou précédent", readAgain: "Relire",
            backHome: "Accueil", round: "Tour", now: "Maintenant",
            city: "Ville", phrase: "Dhikr",
            syncTitle: "Synchronisation entre appareils",
            syncIntro: "Retrouvez votre historique d'adhkar sur tous vos appareils : iPhone, iPad, Mac, Apple TV, Android et le site. Sans compte ni e-mail : un code privé relie vos appareils.",
            turnOn: "Activer la synchronisation", enterCode: "Saisir le code d'un autre appareil", link: "Relier", yourCode: "Code de synchronisation",
            codeHint: "Saisissez ce code sur vos autres appareils dans Réglages → Synchronisation. Gardez-le pour vous.",
            syncNow: "Synchroniser", stop: "Arrêter la synchronisation sur cet appareil",
            badCode: "Ce code n'est pas valide. Vérifiez-le et réessayez.", offline: "Connexion impossible. Vérifiez Internet et réessayez.",
            linked: "Cet appareil est relié")
        }
    }
}
