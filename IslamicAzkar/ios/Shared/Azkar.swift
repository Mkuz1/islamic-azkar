import Foundation

// MARK: - Language

enum Lang: String, CaseIterable, Identifiable, Codable {
    case ar, en, fr
    var id: String { rawValue }
    var isRTL: Bool { self == .ar }
    var locale: Locale { Locale(identifier: rawValue) }

    /// Arabic-Indic digits in Arabic, Western digits otherwise.
    func num(_ n: Int) -> String {
        guard self == .ar else { return String(n) }
        let digits: [Character] = ["٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"]
        return String(String(n).map { c in c.wholeNumberValue.map { digits[$0] } ?? c })
    }

    func clock(_ minutes: Int) -> String {
        let h = num(minutes / 60), m = num(minutes % 60)
        let zero = self == .ar ? "٠" : "0"
        return (h.count < 2 ? zero + h : h) + ":" + (m.count < 2 ? zero + m : m)
    }
}

// MARK: - Model

struct Zikr: Codable, Identifiable, Hashable {
    let id: Int
    let ar: String
    let tr: String
    let en: String
    let fr: String
    let n: Int
    /// Hadith reference (book and number) on the duas added from the hadith collections.
    var ref: LocalizedText? = nil
    /// Arabic explanation, shown as the meaning in Arabic (e.g. for the 99 Names).
    var arm: String? = nil

    var repeatCount: Int { max(1, n) }

    func meaning(_ lang: Lang) -> String {
        switch lang {
        case .ar: return arm ?? ""
        case .en: return en
        case .fr: return fr
        }
    }
}

/// A text in the three languages.
struct LocalizedText: Codable, Hashable {
    let ar: String
    let en: String
    let fr: String

    func callAsFunction(_ lang: Lang) -> String {
        switch lang {
        case .ar: return ar
        case .en: return en
        case .fr: return fr
        }
    }
}

/// A step-by-step guide (the ʿUmrah guide): each step links duas and cites its hadiths.
struct Guide: Codable, Hashable, Identifiable {
    struct Step: Codable, Hashable {
        let icon: String
        let title: LocalizedText
        let text: LocalizedText
        let zikr: [Int]
        let ref: LocalizedText
    }
    let key: String
    let title: LocalizedText
    let intro: LocalizedText
    let note: LocalizedText
    let chapter: Int
    let steps: [Step]
    var id: String { key }
}

struct Chapter: Codable, Identifiable, Hashable {
    let id: Int
    let ar: String
    let en: String
    let fr: String
    let items: [Zikr]

    func title(_ lang: Lang) -> String {
        switch lang {
        case .ar: return ar
        case .en: return en
        case .fr: return fr
        }
    }
}

struct Category: Codable, Identifiable, Hashable {
    let key: String
    let chapters: [Int]
    var id: String { key }
}

/// Something the reader can open: a book chapter, or the curated morning / evening lists.
enum Reading: Hashable, Identifiable {
    case chapter(Int)
    case morning
    case evening

    var id: String { key }

    var key: String {
        switch self {
        case .chapter(let id): return "c\(id)"
        case .morning: return "morning"
        case .evening: return "evening"
        }
    }

    init?(key: String) {
        switch key {
        case "morning": self = .morning
        case "evening": self = .evening
        default:
            guard key.hasPrefix("c"), let id = Int(key.dropFirst()) else { return nil }
            self = .chapter(id)
        }
    }

    /// The reading suggested for the current time of day.
    static func recommended(at date: Date = .now) -> Reading {
        switch Calendar.current.component(.hour, from: date) {
        case 4...11: return .morning
        case 12...14: return .chapter(25)
        case 15...19: return .evening
        default: return .chapter(28)
        }
    }
}

/// Hisn Al Muslim content, decoded once from the bundled azkar.json.
final class AzkarData {
    static let shared = AzkarData()

    let categories: [Category]
    let chapters: [Chapter]
    let morning: [Zikr]
    let evening: [Zikr]
    let chapterById: [Int: Chapter]
    let zikrById: [Int: Zikr]
    /// Chapter id of each dhikr (-1 morning list, -2 evening list).
    let chapterOfZikr: [Int: Int]
    let guides: [Guide]
    var umrahGuide: Guide? { guides.first { $0.key == "umrah" } }

    private struct File: Codable {
        let categories: [Category]
        let chapters: [Chapter]
        let morning: [Zikr]
        let evening: [Zikr]
        let guides: [Guide]?
    }

    private init() {
        let url = Bundle.main.url(forResource: "azkar", withExtension: "json")!
        let file = try! JSONDecoder().decode(File.self, from: Data(contentsOf: url))
        categories = file.categories
        chapters = file.chapters
        morning = file.morning
        evening = file.evening
        guides = file.guides ?? []
        chapterById = Dictionary(uniqueKeysWithValues: chapters.map { ($0.id, $0) })
        var byId: [Int: Zikr] = [:]
        var of: [Int: Int] = [:]
        for c in chapters { for z in c.items { byId[z.id] = z; of[z.id] = c.id } }
        for z in morning { byId[z.id] = z; of[z.id] = -1 }
        for z in evening { byId[z.id] = z; of[z.id] = -2 }
        zikrById = byId
        chapterOfZikr = of
    }

    func items(_ r: Reading) -> [Zikr] {
        switch r {
        case .morning: return morning
        case .evening: return evening
        case .chapter(let id): return chapterById[id]?.items ?? []
        }
    }

    func title(_ r: Reading, _ s: L10n, _ lang: Lang) -> String {
        switch r {
        case .morning: return s.morningAzkar
        case .evening: return s.eveningAzkar
        case .chapter(let id): return chapterById[id]?.title(lang) ?? s.book
        }
    }

    func reading(ofZikr id: Int) -> Reading? {
        switch chapterOfZikr[id] {
        case -1: return .morning
        case -2: return .evening
        case let c?: return .chapter(c)
        case nil: return nil
        }
    }

    /// Short, self-contained azkar for widgets and reminders.
    lazy var shortAzkar: [Zikr] = {
        let pool = morning + evening + [129, 130, 25, 34, 85, 107].flatMap { chapterById[$0]?.items ?? [] }
        var seen = Set<String>()
        return pool.filter { z in
            (12...170).contains(z.ar.count) && !z.ar.contains("﴿") && seen.insert(z.ar).inserted
        }
    }()
}

/// Strips Arabic diacritics and unifies letter forms for search.
func normalizeArabic(_ s: String) -> String {
    var out = ""
    for scalar in s.unicodeScalars {
        let v = scalar.value
        if (0x064B...0x065F).contains(v) || v == 0x0670 || (0x06D6...0x06ED).contains(v) || v == 0x0640 { continue }
        switch scalar {
        case "أ", "إ", "آ", "ٱ": out.append("ا")
        case "ى": out.append("ي")
        case "ة": out.append("ه")
        default: out.unicodeScalars.append(scalar)
        }
    }
    return out.lowercased()
}

// MARK: - Shared store (App Group, so widgets see the same values)

enum SharedStore {
    static let baseGroupID = "group.com.koussa.IslamicAzkar"
    #if os(macOS) || os(tvOS)
    static let groupID = baseGroupID
    /// The Mac and Apple TV apps have no widget extension, so they keep their settings in their own
    /// defaults (on macOS 15, reading another team's group container is a protected access that can
    /// stall launch).
    static let defaults: UserDefaults = .standard
    #else
    /// The App Group the app and its widgets really share. App Store builds use the base name, but
    /// an app re-signed for one device (Xcode, Sideloadly, AltStore…) gets a renamed group, and a
    /// group the app isn't entitled to gives each process its own private store: the widgets then
    /// never see the location or the settings. So the name is taken from the signing profile.
    static let groupID: String = {
        var candidates = provisionedGroups()
        var bid = Bundle.main.bundleIdentifier ?? ""
        if bid.hasSuffix(".Widgets") { bid = String(bid.dropLast(".Widgets".count)) }
        candidates += [baseGroupID, "group." + bid]
        if bid.hasPrefix("com.koussa.IslamicAzkar."), let tail = bid.split(separator: ".").last { candidates.append("\(baseGroupID).\(tail)") }
        return candidates.first { FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: $0) != nil } ?? baseGroupID
    }()

    static let defaults: UserDefaults = {
        let d = UserDefaults(suiteName: groupID) ?? .standard
        migrate(into: d)
        return d
    }()

    /// App groups in this bundle's embedded profile (present when the app was signed for a device).
    private static func provisionedGroups() -> [String] {
        guard let url = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision"),
              let data = try? Data(contentsOf: url),
              let start = data.range(of: Data("<?xml".utf8)),
              let end = data.range(of: Data("</plist>".utf8), in: start.lowerBound..<data.endIndex),
              let plist = try? PropertyListSerialization.propertyList(from: data[start.lowerBound..<end.upperBound], format: nil) as? [String: Any],
              let ent = plist["Entitlements"] as? [String: Any],
              let groups = ent["com.apple.security.application-groups"] as? [String] else { return [] }
        return groups.filter { $0.contains("IslamicAzkar") } + groups.filter { !$0.contains("IslamicAzkar") }
    }

    /// Settings saved before the shared group was found (in the app's own copy) move into it once.
    private static func migrate(into d: UserDefaults) {
        guard groupID != baseGroupID, Bundle.main.bundlePath.hasSuffix(".app"),
              d.object(forKey: "migrated_from_base_group") == nil else { return }
        if let old = UserDefaults.standard.persistentDomain(forName: baseGroupID) {
            for (k, v) in old where d.object(forKey: k) == nil { d.set(v, forKey: k) }
        }
        d.set(true, forKey: "migrated_from_base_group")
    }
    #endif

    enum Key {
        static let lang = "lang"
        static let tasbihCount = "tasbih_count"
        static let tasbihTarget = "tasbih_target"
        static let tasbihPhrase = "tasbih_phrase"
        static let tasbihTotal = "tasbih_total"
    }

    static var lang: Lang { Lang(rawValue: defaults.string(forKey: Key.lang) ?? "") ?? .ar }

    static func incrementTasbih() {
        defaults.set(defaults.integer(forKey: Key.tasbihCount) + 1, forKey: Key.tasbihCount)
        defaults.set(defaults.integer(forKey: Key.tasbihTotal) + 1, forKey: Key.tasbihTotal)
    }

    static func resetTasbih() { defaults.set(0, forKey: Key.tasbihCount) }

    static var tasbihTarget: Int {
        defaults.object(forKey: Key.tasbihTarget) == nil ? 33 : defaults.integer(forKey: Key.tasbihTarget)
    }
}

enum TasbihPhrases {
    static let ar = ["سُبْحَانَ اللَّهِ", "الْحَمْدُ لِلَّهِ", "اللَّهُ أَكْبَرُ", "لَا إِلَهَ إِلَّا اللَّهُ", "أَسْتَغْفِرُ اللَّهَ", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"]
    static let en = ["Glory be to Allah", "All praise is for Allah", "Allah is the Greatest", "None is worthy of worship but Allah", "I seek Allah's forgiveness", "Glory and praise be to Allah", "No might nor power except with Allah", "O Allah, bless Muhammad"]
    static let fr = ["Gloire à Allah", "Louange à Allah", "Allah est le Plus Grand", "Nul ne mérite d'être adoré sauf Allah", "Je demande pardon à Allah", "Gloire et louange à Allah", "Nulle force ni puissance sauf par Allah", "Ô Allah, prie sur Muhammad"]

    static func meaning(_ i: Int, _ lang: Lang) -> String {
        switch lang {
        case .ar: return ""
        case .en: return en[i]
        case .fr: return fr[i]
        }
    }
}

// MARK: - Deep links (widgets, notifications)

enum DeepLink {
    static let scheme = "islamicazkar"

    static func url(_ reading: Reading, focus: Int? = nil) -> URL {
        var s = "\(scheme)://read/\(reading.key)"
        if let focus { s += "?focus=\(focus)" }
        return URL(string: s)!
    }

    static var tasbih: URL { URL(string: "\(scheme)://tasbih")! }

    static func parse(_ url: URL) -> (Reading, Int?)? {
        guard url.scheme == scheme, url.host == "read", let r = Reading(key: url.lastPathComponent) else { return nil }
        let focus = URLComponents(url: url, resolvingAgainstBaseURL: false)?
            .queryItems?.first { $0.name == "focus" }?.value.flatMap(Int.init)
        return (r, focus)
    }
}
