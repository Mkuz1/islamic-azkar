import SwiftUI
import UserNotifications
import WidgetKit
#if os(iOS)
import UIKit
#endif

enum ThemeMode: String, CaseIterable { case system, light, dark }

enum AppTab: Hashable { case home, favorites, tasbih, widgets, settings }

/// Navigation destinations inside the Home tab.
enum Route: Hashable {
    case category(String)
    case reader(Reading, focus: Int?)
    case guide(String)
}

/// App-wide state: preferences (persisted in the shared App Group so widgets agree) and navigation.
@Observable
final class AppModel {
    private let d = SharedStore.defaults

    var lang: Lang { didSet { d.set(lang.rawValue, forKey: SharedStore.Key.lang); refreshExtensions() } }
    var theme: ThemeMode { didSet { d.set(theme.rawValue, forKey: "theme") } }
    var textScale: Double { didSet { d.set(textScale, forKey: "text_scale") } }
    var showTranslit: Bool { didSet { d.set(showTranslit, forKey: "translit") } }
    var showMeaning: Bool { didSet { d.set(showMeaning, forKey: "meaning") } }
    var haptics: Bool { didSet { d.set(haptics, forKey: "haptics") } }
    var favorites: Set<Int> { didSet { d.set(Array(favorites), forKey: "favorites") } }

    // Reminders (minutes after midnight).
    var morningOn: Bool { didSet { save("morning_on", morningOn) } }
    var morningAt: Int { didSet { save("morning_at", morningAt) } }
    var eveningOn: Bool { didSet { save("evening_on", eveningOn) } }
    var eveningAt: Int { didSet { save("evening_at", eveningAt) } }
    var generalOn: Bool { didSet { save("general_on", generalOn) } }
    var generalEvery: Int { didSet { save("general_every", generalEvery) } }
    var generalFrom: Int { didSet { save("general_from", generalFrom) } }
    var generalTo: Int { didSet { save("general_to", generalTo) } }

    // Tasbih, shared with the interactive widget.
    var tasbihCount: Int { didSet { d.set(tasbihCount, forKey: SharedStore.Key.tasbihCount); reloadTasbihWidget() } }
    var tasbihTarget: Int { didSet { d.set(tasbihTarget, forKey: SharedStore.Key.tasbihTarget); reloadTasbihWidget() } }
    var tasbihPhrase: Int { didSet { d.set(tasbihPhrase, forKey: SharedStore.Key.tasbihPhrase); reloadTasbihWidget() } }
    var tasbihTotal: Int { didSet { d.set(tasbihTotal, forKey: SharedStore.Key.tasbihTotal) } }

    // Widgets, calendar and prayer times (read by the widget extension too).
    var palette: Int { didSet { d.set(palette, forKey: SharedStore.Key.palette); WidgetCenter.shared.reloadAllTimelines() } }
    var script: Int { didSet { d.set(script, forKey: SharedStore.Key.script); WidgetCenter.shared.reloadAllTimelines() } }
    var hijriOffset: Int { didSet { d.set(hijriOffset, forKey: SharedStore.Key.hijriOffset); WidgetCenter.shared.reloadAllTimelines() } }
    var place: Place? {
        didSet {
            if let place { place.write(d) } else { Place.clear(d) }
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
    var method: Int { didSet { d.set(method, forKey: SharedStore.Key.method); WidgetCenter.shared.reloadAllTimelines() } }
    var asrHanafi: Bool { didSet { d.set(asrHanafi, forKey: SharedStore.Key.asrHanafi); WidgetCenter.shared.reloadAllTimelines() } }

    // Navigation
    var tab: AppTab = .home
    var homePath: [Route] = []
    /// Split view (iPad, landscape Plus/Max): the reading shown in the detail column.
    var detail: Reading?
    var detailFocus: Int?
    var showDedication = true
    var shareTarget: ShareTarget?
    var toast: String?

    var s: L10n { L10n.of(lang) }
    var m: More { More.of(lang) }

    init() {
        launchTrace("AppModel.init start")
        let d = SharedStore.defaults
        func bool(_ k: String, _ def: Bool) -> Bool { d.object(forKey: k) == nil ? def : d.bool(forKey: k) }
        func int(_ k: String, _ def: Int) -> Int { d.object(forKey: k) == nil ? def : d.integer(forKey: k) }
        lang = SharedStore.lang
        theme = ThemeMode(rawValue: d.string(forKey: "theme") ?? "") ?? .system
        textScale = d.object(forKey: "text_scale") == nil ? 1 : d.double(forKey: "text_scale")
        showTranslit = bool("translit", true)
        showMeaning = bool("meaning", true)
        haptics = bool("haptics", true)
        favorites = Set((d.array(forKey: "favorites") as? [Int]) ?? [])
        morningOn = bool("morning_on", false); morningAt = int("morning_at", 6 * 60)
        eveningOn = bool("evening_on", false); eveningAt = int("evening_at", 17 * 60)
        generalOn = bool("general_on", false); generalEvery = int("general_every", 3)
        generalFrom = int("general_from", 9 * 60); generalTo = int("general_to", 21 * 60)
        tasbihCount = int(SharedStore.Key.tasbihCount, 0)
        tasbihTarget = int(SharedStore.Key.tasbihTarget, 33)
        tasbihPhrase = int(SharedStore.Key.tasbihPhrase, 0)
        tasbihTotal = int(SharedStore.Key.tasbihTotal, 0)
        palette = int(SharedStore.Key.palette, 0)
        script = int(SharedStore.Key.script, 0)
        hijriOffset = int(SharedStore.Key.hijriOffset, 0)
        place = Place.read(d)
        method = int(SharedStore.Key.method, CalcMethod.dubai.rawValue)
        asrHanafi = bool(SharedStore.Key.asrHanafi, false)
        launchTrace("AppModel settings read")
        applyLaunchArguments()
        launchTrace("AppModel.init done")
    }

    /// CI screenshots: `-ciScreen home|reader|celebration|tasbih|widgets|settings|favorites|dedication -ciLang ar -ciDark 1`.
    var ciScreen: String?
    private func applyLaunchArguments() {
        let args = UserDefaults.standard
        guard let screen = args.string(forKey: "ciScreen") else { return }
        ciScreen = screen
        if let l = args.string(forKey: "ciLang").flatMap(Lang.init(rawValue:)) { lang = l }
        theme = args.bool(forKey: "ciDark") ? .dark : .light
        if place == nil { place = Cities.all[3] }
        if favorites.isEmpty { favorites = [AzkarData.shared.morning[1].id, AzkarData.shared.evening[3].id, AzkarData.shared.morning[6].id] }
        if tasbihTotal == 0 { tasbihCount = 21; tasbihTotal = 722; for _ in 0..<39 { TasbihHistory.add() } }
        showDedication = screen == "dedication"
        switch screen {
        case "favorites": tab = .favorites
        case "tasbih": tab = .tasbih
        case "widgets": tab = .widgets
        case "settings": tab = .settings
        case "reader", "celebration": homePath = [.reader(.morning, focus: nil)]
        case "guide": homePath = [.guide("umrah")]
        case "sunnah": homePath = [.reader(.chapter(134), focus: nil)]
        case "quran": homePath = [.reader(.chapter(137), focus: nil)]
        default: tab = .home
        }
    }

    private func save(_ key: String, _ value: Any) {
        d.set(value, forKey: key)
        Reminders.reschedule(self)
    }

    func toggleFavorite(_ id: Int) {
        if favorites.contains(id) { favorites.remove(id) } else { favorites.insert(id) }
    }

    /// Picks up counts the interactive widget made while the app was in the background.
    func reloadTasbihFromStore() {
        let c = d.integer(forKey: SharedStore.Key.tasbihCount)
        let t = d.integer(forKey: SharedStore.Key.tasbihTotal)
        if c != tasbihCount { tasbihCount = c }
        if t != tasbihTotal { tasbihTotal = t }
    }

    func open(_ reading: Reading, focus: Int? = nil) {
        showDedication = false
        tab = .home
        detail = reading
        detailFocus = focus
        homePath = [.reader(reading, focus: focus)]
    }

    func handle(url: URL) {
        if url == DeepLink.tasbih { showDedication = false; tab = .tasbih; return }
        if let (r, focus) = DeepLink.parse(url) { open(r, focus: focus) }
    }

    private func reloadTasbihWidget() { WidgetCenter.shared.reloadTimelines(ofKind: "TasbihWidget") }

    private func refreshExtensions() {
        WidgetCenter.shared.reloadAllTimelines()
        Reminders.reschedule(self)
    }
}

struct ShareTarget: Identifiable {
    let zikr: Zikr
    let title: String
    var id: Int { zikr.id }
}

#if os(iOS)
/// Routes notification taps into the app.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    var model: AppModel?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        guard let link = response.notification.request.content.userInfo["link"] as? String, let url = URL(string: link) else { return }
        await MainActor.run { model?.handle(url: url) }
    }
}
#endif

/// `-ciTrace 1`: prints launch stages to stderr (diagnosing a launch that shows nothing).
func launchTrace(_ stage: String) {
    guard UserDefaults.standard.bool(forKey: "ciTrace") else { return }
    FileHandle.standardError.write(Data("trace: \(stage)\n".utf8))
}

@main
struct IslamicAzkarApp: App {
    #if os(iOS)
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    #else
    @NSApplicationDelegateAdaptor(MacAppDelegate.self) private var macDelegate
    #endif
    @State private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    init() { launchTrace("App.init") }

    var body: some Scene {
        WindowGroup(id: "main") {
            Group {
                if model.showDedication {
                    DedicationView(onContinue: { withAnimation(.easeInOut(duration: 0.6)) { model.showDedication = false } },
                                   onOpen: { r in withAnimation(.easeInOut(duration: 0.6)) { model.open(r) } })
                        .transition(.opacity)
                } else {
                    RootView().transition(.opacity)
                }
            }
            .environment(model)
            .environment(\.layoutDirection, model.lang.isRTL ? .rightToLeft : .leftToRight)
            .environment(\.locale, model.lang.locale)
            .preferredColorScheme(model.theme == .light ? .light : model.theme == .dark ? .dark : nil)
            .tint(Theme.accent)
            .onOpenURL { model.handle(url: $0) }
            .onAppear {
                launchTrace("main window appeared")
                #if os(macOS)
                MacCI.start()
                #endif
                #if os(iOS)
                delegate.model = model
                #endif
                Reminders.reschedule(model)
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { model.reloadTasbihFromStore() }
            }
        }
        #if os(macOS)
        .defaultSize(width: 1100, height: 760)
        #endif

        #if os(macOS)
        // The menu bar icon stays while the app runs, even with every window closed or minimized:
        // it reminds of the azkar due now and shows today's prayer times (MenuBar.swift in mac/).
        MenuBarExtra {
            MenuBarPanel()
                .environment(model)
                .environment(\.layoutDirection, model.lang.isRTL ? .rightToLeft : .leftToRight)
                .environment(\.locale, model.lang.locale)
                .tint(Theme.accent)
        } label: {
            MenuBarLabel().environment(model)
        }
        .menuBarExtraStyle(.window)
        #endif
    }
}
