import SwiftUI

/// Apple TV app: the same azkar, prayer times, tasbih and record as the iPhone app (the data and
/// logic in ../ios/Shared), with screens made for the Siri Remote and the living-room distance.
@main
struct IslamicAzkarTVApp: App {
    @State private var model = TVModel()

    var body: some Scene {
        WindowGroup {
            TVRootView()
                .environment(model)
                .environment(\.layoutDirection, model.lang.isRTL ? .rightToLeft : .leftToRight)
                .environment(\.locale, model.lang.locale)
                .preferredColorScheme(.dark)
                .tint(Brand.mushafGold)
        }
    }
}

enum TVTab: Hashable { case home, tasbih, record, settings }

enum TVRoute: Hashable {
    case reader(Reading)
    case category(String)
}

/// Settings and state, kept in the same defaults keys as the iPhone app.
@Observable
final class TVModel {
    @ObservationIgnored private let d = SharedStore.defaults

    var lang: Lang { didSet { d.set(lang.rawValue, forKey: SharedStore.Key.lang) } }
    var place: Place { didSet { place.write(d) } }
    var method: Int { didSet { d.set(method, forKey: SharedStore.Key.method) } }
    var tasbihCount: Int { didSet { d.set(tasbihCount, forKey: SharedStore.Key.tasbihCount) } }
    var tasbihTarget: Int { didSet { d.set(tasbihTarget, forKey: SharedStore.Key.tasbihTarget) } }
    var tasbihPhrase: Int { didSet { d.set(tasbihPhrase, forKey: SharedStore.Key.tasbihPhrase) } }
    var tasbihTotal: Int { didSet { d.set(tasbihTotal, forKey: SharedStore.Key.tasbihTotal) } }

    var tab: TVTab = .home
    var homePath: [TVRoute] = []
    /// Bumped when the record changes (a reading completed here, or synced from another device).
    var historyVersion = 0

    var s: L10n { L10n.of(lang) }
    var m: More { More.of(lang) }
    var t: TVTexts { TVTexts.of(lang) }

    init() {
        let d = SharedStore.defaults
        lang = SharedStore.lang
        // A place is needed for prayer times; Makkah until the user picks their city.
        place = SharedStore.place ?? Cities.all[0]
        method = d.object(forKey: SharedStore.Key.method) == nil ? CalcMethod.dubai.rawValue : d.integer(forKey: SharedStore.Key.method)
        tasbihCount = d.integer(forKey: SharedStore.Key.tasbihCount)
        tasbihTarget = d.object(forKey: SharedStore.Key.tasbihTarget) == nil ? 33 : d.integer(forKey: SharedStore.Key.tasbihTarget)
        tasbihPhrase = d.integer(forKey: SharedStore.Key.tasbihPhrase)
        tasbihTotal = d.integer(forKey: SharedStore.Key.tasbihTotal)
        applyCI()
    }

    /// CI screenshots: `-ciScreen home|reader|done|tasbih|record|settings -ciLang ar|en|fr`.
    private func applyCI() {
        let args = UserDefaults.standard
        guard let screen = args.string(forKey: "ciScreen") else { return }
        if let l = args.string(forKey: "ciLang").flatMap(Lang.init(rawValue:)) { lang = l }
        place = Cities.all[0]
        switch screen {
        case "reader": homePath = [.reader(.morning)]
        case "category": homePath = [.category("hardship")]
        case "tasbih": tab = .tasbih; tasbihCount = 21; tasbihTarget = 33; tasbihTotal = 1250
        case "record":
            tab = .record
            let plan: [[String]] = [["morning", "evening", "c28"], ["morning", "evening"], ["morning", "c28", "c25"],
                                    ["morning", "evening", "c28", "c25"], ["morning", "evening"], ["morning", "evening", "c28"],
                                    ["morning", "c25"], ["morning", "evening"], ["evening", "c28"], ["morning", "evening", "c28"],
                                    ["morning"], ["morning", "evening", "c25"], ["morning", "evening"], ["morning", "evening", "c28"]]
            for (i, keys) in plan.enumerated() {
                let day = Calendar.current.date(byAdding: .day, value: -i, to: .now) ?? .now
                keys.forEach { AzkarHistory.markReading($0, at: day) }
            }
        case "settings": tab = .settings
        default: tab = .home
        }
    }
}

/// The deep green backdrop of the app, with a soft gold glow.
struct TVBackdrop: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(hex: 0x173F31), Brand.mushafNight], startPoint: .top, endPoint: .bottom)
            RadialGradient(colors: [Brand.mushafGold.opacity(0.14), .clear], center: .topLeading, startRadius: 0, endRadius: 1100)
        }
        .ignoresSafeArea()
    }
}

struct TVRootView: View {
    @Environment(TVModel.self) private var model

    var body: some View {
        @Bindable var m = model
        let s = model.s
        TabView(selection: $m.tab) {
            NavigationStack(path: $m.homePath) {
                TVHomeView()
                    .navigationDestination(for: TVRoute.self) { route in
                        switch route {
                        case .reader(let r): TVReaderView(reading: r)
                        case .category(let key): TVCategoryView(key: key)
                        }
                    }
            }
            .tabItem { Label(s.tabHome, systemImage: "house.fill") }
            .tag(TVTab.home)

            NavigationStack { TVTasbihView() }
                .tabItem { Label(s.tabTasbih, systemImage: "circle.hexagongrid.fill") }
                .tag(TVTab.tasbih)

            NavigationStack { TVRecordView() }
                .tabItem { Label(model.t.record, systemImage: "calendar") }
                .tag(TVTab.record)

            NavigationStack { TVSettingsView() }
                .tabItem { Label(s.tabSettings, systemImage: "gearshape.fill") }
                .tag(TVTab.settings)
        }
        .background(TVBackdrop())
        .task { await AzkarSync.syncNow() }
        .onReceive(NotificationCenter.default.publisher(for: AzkarSync.changed)) { _ in model.historyVersion += 1 }
    }
}
