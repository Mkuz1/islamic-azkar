import Foundation
import SwiftUI

// Prayer times, the Hijri calendar, calligraphy, widget palettes and the quote library —
// shared by the app and its widgets (mirrors the Android app's data package).

// MARK: - Settings shared with the widgets

extension SharedStore.Key {
    static let palette = "widget_palette"
    static let script = "calligraphy_script"
    static let hijriOffset = "hijri_offset"
    static let method = "prayer_method"
    static let asrHanafi = "asr_hanafi"
    static let timelineIndex = "timeline_index"
}

extension SharedStore {
    static var palette: Palette { Palette.all[safe: defaults.integer(forKey: Key.palette)] ?? .mushaf }
    static var script: Script { Script(rawValue: defaults.integer(forKey: Key.script)) ?? .ruqaa }
    static var hijriOffset: Int { defaults.integer(forKey: Key.hijriOffset) }
    /// Dubai (Dulook DXB) unless the user picked another method.
    static var method: CalcMethod {
        defaults.object(forKey: Key.method) == nil ? .dubai : (CalcMethod.allCases[safe: defaults.integer(forKey: Key.method)] ?? .dubai)
    }
    static var asrHanafi: Bool { defaults.bool(forKey: Key.asrHanafi) }
    static var place: Place? { Place.read(defaults) }
}

extension Array {
    subscript(safe i: Int) -> Element? { indices.contains(i) ? self[i] : nil }
}

// MARK: - Hijri (Umm al-Qura), shifted by the user's offset to match the local moon sighting

enum Hijri {
    struct HDate: Equatable { let day: Int; let month: Int; let year: Int }   // month 0…11

    static func of(_ date: Date = .now, offset: Int = SharedStore.hijriOffset) -> HDate {
        var cal = Calendar(identifier: .islamicUmmAlQura)
        cal.timeZone = .current
        let d = date.addingTimeInterval(Double(offset) * 86_400)
        let c = cal.dateComponents([.day, .month, .year], from: d)
        return HDate(day: c.day ?? 1, month: (c.month ?? 1) - 1, year: c.year ?? 1448)
    }

    /// Fraction of the Hijri year that has passed (for the progress widget).
    static func yearProgress(_ date: Date = .now, offset: Int = SharedStore.hijriOffset) -> Double {
        var cal = Calendar(identifier: .islamicUmmAlQura)
        cal.timeZone = .current
        let d = date.addingTimeInterval(Double(offset) * 86_400)
        guard let year = cal.dateInterval(of: .year, for: d) else { return 0 }
        return d.timeIntervalSince(year.start) / year.duration
    }

    static let monthsAr = ["مُحَرَّم", "صَفَر", "رَبِيع الأوَّل", "رَبِيع الآخِر", "جُمادَى الأُولى", "جُمادَى الآخِرة", "رَجَب", "شَعْبان", "رَمَضان", "شَوَّال", "ذُو القَعْدة", "ذُو الحِجَّة"]
    static let monthsEn = ["Muharram", "Safar", "Rabiʿ al-Awwal", "Rabiʿ al-Akhir", "Jumada al-Ula", "Jumada al-Akhira", "Rajab", "Shaʿban", "Ramadan", "Shawwal", "Dhu al-Qaʿda", "Dhu al-Hijja"]
    static let monthsFr = ["Mouharram", "Safar", "Rabiʿ al-Awwal", "Rabiʿ ath-Thani", "Joumada al-Oula", "Joumada ath-Thania", "Rajab", "Chaʿbane", "Ramadan", "Chawwal", "Dhou al-Qiʿda", "Dhou al-Hijja"]
    static let weekdaysAr = ["الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"]
    static let weekdaysEn = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    static let weekdaysFr = ["Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"]

    static func month(_ lang: Lang, _ m: Int) -> String {
        switch lang { case .ar: monthsAr[m]; case .en: monthsEn[m]; case .fr: monthsFr[m] }
    }

    /// Sunday = 0.
    static func weekday(_ lang: Lang, _ dow: Int) -> String {
        switch lang { case .ar: weekdaysAr[dow]; case .en: weekdaysEn[dow]; case .fr: weekdaysFr[dow] }
    }

    static func weekdayIndex(_ date: Date = .now) -> Int { Calendar.current.component(.weekday, from: date) - 1 }

    static func format(_ lang: Lang, _ d: HDate) -> String {
        switch lang {
        case .ar: "\(Lang.ar.num(d.day)) \(monthsAr[d.month]) \(Lang.ar.num(d.year)) هـ"
        case .en: "\(d.day) \(monthsEn[d.month]) \(d.year) AH"
        case .fr: "\(d.day) \(monthsFr[d.month]) \(d.year) H"
        }
    }
}

// MARK: - Calligraphy and palettes

/// Classical scripts for weekdays, months, prayer names and quotes on widgets.
enum Script: Int, CaseIterable, Identifiable {
    case ruqaa, naskh, kufi, nastaliq
    var id: Int { rawValue }

    func label(_ lang: Lang) -> String {
        switch (self, lang) {
        case (.ruqaa, .ar): "الرقعة"; case (.ruqaa, .en): "Ruqʿah"; case (.ruqaa, .fr): "Roqʿa"
        case (.naskh, .ar): "النسخ"; case (.naskh, _): "Naskh"
        case (.kufi, .ar): "الكوفي"; case (.kufi, .en): "Kufic"; case (.kufi, .fr): "Coufique"
        case (.nastaliq, .ar): "النستعليق"; case (.nastaliq, .en): "Nastaʿlīq"; case (.nastaliq, .fr): "Nastaʿlîq"
        }
    }

    func font(_ size: CGFloat) -> Font {
        switch self {
        case .ruqaa: .custom("ArefRuqaa-Bold", size: size)
        case .naskh: .custom("Amiri-Bold", size: size)
        case .kufi: .custom("ReemKufi", size: size).weight(.bold)
        case .nastaliq: .custom("NotoNastaliqUrdu-Regular", size: size * 0.82).weight(.bold)
        }
    }
}

/// Calm widget colour palettes: the glass tint behind every widget and the matching clock face.
struct Palette: Identifiable, Equatable {
    let id: Int
    let ar: String, en: String, fr: String
    let top: Color, bottom: Color

    func label(_ lang: Lang) -> String { lang == .ar ? ar : lang == .en ? en : fr }

    var gradient: LinearGradient { LinearGradient(colors: [top, bottom], startPoint: .topLeading, endPoint: .bottomTrailing) }

    static let mushaf = Palette(id: 0, ar: "أخضر المصحف", en: "Mushaf green", fr: "Vert Mushaf", top: Color(hex: 0x295245), bottom: Color(hex: 0x0B1F19))
    static let all: [Palette] = [
        mushaf,
        Palette(id: 1, ar: "ليل", en: "Midnight", fr: "Minuit", top: Color(hex: 0x26355A), bottom: Color(hex: 0x0B1229)),
        Palette(id: 2, ar: "رمل", en: "Desert sand", fr: "Sable", top: Color(hex: 0x6B5233), bottom: Color(hex: 0x2A1E10)),
        Palette(id: 3, ar: "وردي", en: "Rose", fr: "Rose", top: Color(hex: 0x6A2F3E), bottom: Color(hex: 0x2A0F17)),
        Palette(id: 4, ar: "فيروزي", en: "Turquoise", fr: "Turquoise", top: Color(hex: 0x1F5A5E), bottom: Color(hex: 0x082626)),
        Palette(id: 5, ar: "خزامى", en: "Lavender", fr: "Lavande", top: Color(hex: 0x4A3B6B), bottom: Color(hex: 0x1C1530)),
        Palette(id: 6, ar: "فحمي", en: "Graphite", fr: "Graphite", top: Color(hex: 0x3A3D42), bottom: Color(hex: 0x121315)),
    ]
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255, opacity: alpha)
    }
}

// MARK: - Prayer times

/// A place for prayer times: coordinates and the time zone its clocks follow.
struct Place: Equatable, Hashable {
    let nameAr: String, name: String
    let lat: Double, lng: Double
    let tz: String

    func label(_ lang: Lang) -> String { lang == .ar ? nameAr : name }
    var timeZone: TimeZone { TimeZone(identifier: tz) ?? .current }

    func write(_ d: UserDefaults) {
        d.set(nameAr, forKey: "place_ar"); d.set(name, forKey: "place_name")
        d.set(lat, forKey: "place_lat"); d.set(lng, forKey: "place_lng"); d.set(tz, forKey: "place_tz")
    }

    static func read(_ d: UserDefaults) -> Place? {
        guard d.object(forKey: "place_lat") != nil else { return nil }
        return Place(nameAr: d.string(forKey: "place_ar") ?? "", name: d.string(forKey: "place_name") ?? "",
                     lat: d.double(forKey: "place_lat"), lng: d.double(forKey: "place_lng"),
                     tz: d.string(forKey: "place_tz") ?? TimeZone.current.identifier)
    }

    static func clear(_ d: UserDefaults) {
        for k in ["place_ar", "place_name", "place_lat", "place_lng", "place_tz"] { d.removeObject(forKey: k) }
    }
}

/// Cities offered when the phone's location isn't used.
enum Cities {
    static let all: [Place] = [
        Place(nameAr: "مكة المكرمة", name: "Makkah", lat: 21.4225, lng: 39.8262, tz: "Asia/Riyadh"),
        Place(nameAr: "المدينة المنورة", name: "Madinah", lat: 24.4672, lng: 39.6112, tz: "Asia/Riyadh"),
        Place(nameAr: "القدس", name: "Jerusalem", lat: 31.7780, lng: 35.2354, tz: "Asia/Jerusalem"),
        Place(nameAr: "بيروت", name: "Beirut", lat: 33.8938, lng: 35.5018, tz: "Asia/Beirut"),
        Place(nameAr: "طرابلس (لبنان)", name: "Tripoli (Lebanon)", lat: 34.4367, lng: 35.8497, tz: "Asia/Beirut"),
        Place(nameAr: "صيدا", name: "Saida", lat: 33.5571, lng: 35.3729, tz: "Asia/Beirut"),
        Place(nameAr: "دمشق", name: "Damascus", lat: 33.5138, lng: 36.2765, tz: "Asia/Damascus"),
        Place(nameAr: "عمّان", name: "Amman", lat: 31.9539, lng: 35.9106, tz: "Asia/Amman"),
        Place(nameAr: "القاهرة", name: "Cairo", lat: 30.0444, lng: 31.2357, tz: "Africa/Cairo"),
        Place(nameAr: "الرياض", name: "Riyadh", lat: 24.7136, lng: 46.6753, tz: "Asia/Riyadh"),
        Place(nameAr: "جدة", name: "Jeddah", lat: 21.5433, lng: 39.1728, tz: "Asia/Riyadh"),
        Place(nameAr: "دبي", name: "Dubai", lat: 25.2048, lng: 55.2708, tz: "Asia/Dubai"),
        Place(nameAr: "أبوظبي", name: "Abu Dhabi", lat: 24.4539, lng: 54.3773, tz: "Asia/Dubai"),
        Place(nameAr: "الدوحة", name: "Doha", lat: 25.2854, lng: 51.5310, tz: "Asia/Qatar"),
        Place(nameAr: "الكويت", name: "Kuwait City", lat: 29.3759, lng: 47.9774, tz: "Asia/Kuwait"),
        Place(nameAr: "بغداد", name: "Baghdad", lat: 33.3152, lng: 44.3661, tz: "Asia/Baghdad"),
        Place(nameAr: "إسطنبول", name: "Istanbul", lat: 41.0082, lng: 28.9784, tz: "Europe/Istanbul"),
        Place(nameAr: "الدار البيضاء", name: "Casablanca", lat: 33.5731, lng: -7.5898, tz: "Africa/Casablanca"),
        Place(nameAr: "تونس", name: "Tunis", lat: 36.8065, lng: 10.1815, tz: "Africa/Tunis"),
        Place(nameAr: "الجزائر", name: "Algiers", lat: 36.7538, lng: 3.0588, tz: "Africa/Algiers"),
        Place(nameAr: "باريس", name: "Paris", lat: 48.8566, lng: 2.3522, tz: "Europe/Paris"),
        Place(nameAr: "ليون", name: "Lyon", lat: 45.7640, lng: 4.8357, tz: "Europe/Paris"),
        Place(nameAr: "مرسيليا", name: "Marseille", lat: 43.2965, lng: 5.3698, tz: "Europe/Paris"),
        Place(nameAr: "بروكسل", name: "Brussels", lat: 50.8503, lng: 4.3517, tz: "Europe/Brussels"),
        Place(nameAr: "لندن", name: "London", lat: 51.5074, lng: -0.1278, tz: "Europe/London"),
        Place(nameAr: "برلين", name: "Berlin", lat: 52.5200, lng: 13.4050, tz: "Europe/Berlin"),
        Place(nameAr: "مونتريال", name: "Montreal", lat: 45.5019, lng: -73.5674, tz: "America/Toronto"),
        Place(nameAr: "نيويورك", name: "New York", lat: 40.7128, lng: -74.0060, tz: "America/New_York"),
        Place(nameAr: "سيدني", name: "Sydney", lat: -33.8688, lng: 151.2093, tz: "Australia/Sydney"),
        Place(nameAr: "كوالالمبور", name: "Kuala Lumpur", lat: 3.1390, lng: 101.6869, tz: "Asia/Kuala_Lumpur"),
        Place(nameAr: "جاكرتا", name: "Jakarta", lat: -6.2088, lng: 106.8456, tz: "Asia/Jakarta"),
    ]
}

/// Calculation authorities: twilight angles for Fajr and Isha (or a fixed delay after Maghrib).
enum CalcMethod: Int, CaseIterable, Identifiable {
    case mwl, ummAlQura, egypt, isna, karachi, gulf, dubai, france, turkey
    var id: Int { rawValue }

    var fajr: Double { [18, 18.5, 19.5, 15, 18, 19.5, 18.2, 12, 18][rawValue] }
    var isha: Double { [17, 0, 17.5, 15, 18, 0, 18.2, 12, 17][rawValue] }
    var ishaMinutes: Int { self == .ummAlQura || self == .gulf ? 90 : 0 }
    /// Precaution minutes added to Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha. Dubai follows Dubai's
    /// Islamic Affairs department (IACAD, the Dulook DXB app): 18.2°, Dhuhr and Maghrib +3 min (aladhan method 16).
    var offsets: [Double] { self == .dubai ? [0, 0, 3, 0, 3, 0] : [0, 0, 0, 0, 0, 0] }
}

/// Prayer times from the sun's position, with the angle-based rule for high latitudes.
enum PrayerCalc {
    private static func rad(_ d: Double) -> Double { d * .pi / 180 }
    private static func deg(_ r: Double) -> Double { r * 180 / .pi }
    private static func dsin(_ d: Double) -> Double { sin(rad(d)) }
    private static func dcos(_ d: Double) -> Double { cos(rad(d)) }
    private static func dtan(_ d: Double) -> Double { tan(rad(d)) }
    private static func fix(_ a: Double, _ b: Double) -> Double { a - b * floor(a / b) }

    private static func julian(_ year: Int, _ month: Int, _ day: Int) -> Double {
        var y = year, m = month
        if m <= 2 { y -= 1; m += 12 }
        let a = floor(Double(y) / 100)
        let b = 2 - a + floor(a / 4)
        return floor(365.25 * Double(y + 4716)) + floor(30.6001 * Double(m + 1)) + Double(day) + b - 1524.5
    }

    /// Declination and equation of time.
    private static func sun(_ jd: Double) -> (Double, Double) {
        let d = jd - 2451545.0
        let g = fix(357.529 + 0.98560028 * d, 360)
        let q = fix(280.459 + 0.98564736 * d, 360)
        let l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360)
        let e = 23.439 - 0.00000036 * d
        let ra = fix(deg(atan2(dcos(e) * dsin(l), dcos(l))) / 15, 24)
        return (deg(asin(dsin(e) * dsin(l))), q / 15 - fix(ra, 24))
    }

    /// Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha in local hours.
    static func hours(year: Int, month: Int, day: Int, lat: Double, lng: Double, tz: Double, method: CalcMethod, hanafi: Bool) -> [Double] {
        let jd = julian(year, month, day) - lng / (15 * 24)
        func midDay(_ t: Double) -> Double { fix(12 - sun(jd + t).1, 24) }
        func angleTime(_ angle: Double, _ t: Double, ccw: Bool) -> Double {
            let decl = sun(jd + t).0
            let x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            let span = deg(acos(x)) / 15
            return midDay(t) + (ccw ? -span : span)
        }
        func asr(_ factor: Double, _ t: Double) -> Double {
            let decl = sun(jd + t).0
            return angleTime(-deg(atan(1 / (factor + dtan(abs(lat - decl))))), t, ccw: false)
        }
        var fajr = angleTime(method.fajr, 5 / 24, ccw: true)
        let sunrise = angleTime(0.833, 6 / 24, ccw: true)
        let dhuhr = midDay(12 / 24)
        let asrT = asr(hanafi ? 2 : 1, 13 / 24)
        let sunset = angleTime(0.833, 18 / 24, ccw: false)
        var isha = method.ishaMinutes > 0 ? .nan : angleTime(method.isha, 18 / 24, ccw: false)

        let night = 24 - (sunset - sunrise)
        let fajrLimit = night * method.fajr / 60
        if fajr.isNaN || sunrise - fajr > fajrLimit { fajr = sunrise - fajrLimit }
        if method.ishaMinutes > 0 {
            isha = sunset + Double(method.ishaMinutes) / 60
        } else {
            let ishaLimit = night * method.isha / 60
            if isha.isNaN || isha - sunset > ishaLimit { isha = sunset + ishaLimit }
        }
        let shift = tz - lng / 15
        return zip([fajr, sunrise, dhuhr, asrT, sunset, isha], method.offsets).map { $0 + shift + $1 / 60 }
    }
}

/// Today's prayer times for a place.
enum Prayers {
    static let fajr = 0, sunrise = 1, isha = 5

    /// The six times (Fajr…Isha) of the day containing `day`, in the place's time zone.
    static func times(_ place: Place, day: Date = .now, method: CalcMethod = SharedStore.method, hanafi: Bool = SharedStore.asrHanafi) -> [Date] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.timeZone
        let c = cal.dateComponents([.year, .month, .day], from: day)
        let midnight = cal.startOfDay(for: day)
        let tz = Double(place.timeZone.secondsFromGMT(for: midnight.addingTimeInterval(12 * 3600))) / 3600
        let h = PrayerCalc.hours(year: c.year!, month: c.month!, day: c.day!, lat: place.lat, lng: place.lng, tz: tz, method: method, hanafi: hanafi)
        return h.map { midnight.addingTimeInterval((($0 * 60).rounded()) * 60) }
    }

    /// The next prayer (sunrise excluded) after `now`, rolling over to tomorrow's Fajr.
    static func next(_ place: Place, now: Date = .now) -> (index: Int, time: Date) {
        let t = times(place, day: now)
        for i in 0...5 where i != sunrise && t[i] > now { return (i, t[i]) }
        return (fajr, times(place, day: now.addingTimeInterval(86_400))[fajr])
    }

    /// The prayer whose time it currently is, with its start.
    static func previous(_ place: Place, now: Date = .now) -> (index: Int, time: Date) {
        let t = times(place, day: now)
        for i in stride(from: 5, through: 0, by: -1) where i != sunrise && t[i] <= now { return (i, t[i]) }
        return (isha, times(place, day: now.addingTimeInterval(-86_400))[isha])
    }

    static func clock(_ date: Date, _ place: Place, _ lang: Lang) -> String {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.timeZone
        let c = cal.dateComponents([.hour, .minute], from: date)
        return lang.clock((c.hour ?? 0) * 60 + (c.minute ?? 0))
    }
}

// MARK: - Quotes: Quran, hadith, poetry and wisdom

struct Quote: Codable, Hashable {
    let ar: String, en: String, fr: String
    let srcAr: String, srcEn: String, srcFr: String

    func meaning(_ lang: Lang) -> String { lang == .ar ? "" : lang == .en ? en : fr }
    func source(_ lang: Lang) -> String { lang == .ar ? srcAr : lang == .en ? srcEn : srcFr }
}

enum QuoteSource: Int, CaseIterable, Identifiable {
    case mixed, quran, hadith, azkar, poetry, wisdom
    var id: Int { rawValue }
}

/// Verses (from the Mushaf app's Uthmani text), hadith on the virtues of dhikr, poetry, wisdom
/// and the encouragements shown after completing a set of azkar.
final class Library {
    static let shared = Library()

    let verses: [Quote]
    let poetry: [Quote]
    let wisdom: [Quote]
    let encouragements: [Quote]

    private struct VerseFile: Codable { let ar, en, fr, refAr, ref: String }
    private struct LibraryFile: Codable { let poetry, wisdom, encouragements: [Quote] }

    private init() {
        let vurl = Bundle.main.url(forResource: "verses", withExtension: "json")!
        let v = (try? JSONDecoder().decode([VerseFile].self, from: Data(contentsOf: vurl))) ?? []
        verses = v.map { Quote(ar: $0.ar, en: $0.en, fr: $0.fr, srcAr: $0.refAr, srcEn: $0.ref, srcFr: $0.ref) }
        let lurl = Bundle.main.url(forResource: "library", withExtension: "json")!
        let l = try! JSONDecoder().decode(LibraryFile.self, from: Data(contentsOf: lurl))
        poetry = l.poetry
        wisdom = l.wisdom
        encouragements = l.encouragements
    }

    /// Hadith from the book's chapters on the virtues of dhikr, salawat, salam and repentance.
    lazy var hadiths: [Quote] = {
        let data = AzkarData.shared
        return [130, 107, 129, 108, 50].flatMap { data.chapterById[$0]?.items ?? [] }
            .filter { (30...360).contains($0.ar.count) }
            .map { Quote(ar: $0.ar, en: $0.en, fr: $0.fr, srcAr: "حصن المسلم", srcEn: "Hisn al-Muslim", srcFr: "La Citadelle du musulman") }
    }()

    lazy var azkar: [Quote] = AzkarData.shared.shortAzkar.map {
        Quote(ar: $0.ar, en: $0.en, fr: $0.fr, srcAr: "حصن المسلم", srcEn: "Hisn al-Muslim", srcFr: "La Citadelle du musulman")
    }

    func pool(_ source: QuoteSource) -> [Quote] {
        switch source {
        case .quran: verses
        case .hadith: hadiths
        case .azkar: azkar
        case .poetry: poetry
        case .wisdom: wisdom
        case .mixed: verses + hadiths + poetry + wisdom
        }
    }

    /// A quote that changes every `hours` hours, the same everywhere at the same time.
    func quote(_ source: QuoteSource, at date: Date = .now, every hours: Int = 2) -> Quote {
        let p = pool(source)
        let slot = Int(date.timeIntervalSince1970 / Double(hours * 3600))
        return p[(slot * 7 + source.rawValue) % max(p.count, 1)]
    }
}

// MARK: - Tasbih history and reading streaks

enum TasbihHistory {
    private static func day(_ date: Date = .now) -> Int {
        Int((date.timeIntervalSince1970 + Double(TimeZone.current.secondsFromGMT(for: date))) / 86_400)
    }

    static func add(_ n: Int = 1) {
        let d = SharedStore.defaults, k = "tasbih_day_\(day())"
        d.set(d.integer(forKey: k) + n, forKey: k)
    }

    /// The last seven days, oldest first, ending today: (weekday index Sunday = 0, count).
    static func lastWeek(_ now: Date = .now) -> [(weekday: Int, count: Int)] {
        let d = SharedStore.defaults, t = day(now)
        return (0..<7).reversed().map { back in
            let date = now.addingTimeInterval(-Double(back) * 86_400)
            return (Hijri.weekdayIndex(date), d.integer(forKey: "tasbih_day_\(t - back)"))
        }
    }
}

enum Streaks {
    /// Records that `key` (e.g. "morning") was completed today; returns the consecutive-day count.
    static func record(_ key: String) -> Int {
        let d = SharedStore.defaults
        let today = Int((Date.now.timeIntervalSince1970 + Double(TimeZone.current.secondsFromGMT())) / 86_400)
        let last = d.object(forKey: "streak_day_\(key)") == nil ? Int.min / 2 : d.integer(forKey: "streak_day_\(key)")
        let count = d.integer(forKey: "streak_\(key)")
        let next = today - last == 0 ? max(count, 1) : today - last == 1 ? count + 1 : 1
        d.set(today, forKey: "streak_day_\(key)")
        d.set(next, forKey: "streak_\(key)")
        return next
    }
}
