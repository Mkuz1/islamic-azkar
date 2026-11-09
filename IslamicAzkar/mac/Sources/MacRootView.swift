import AppKit
import SwiftUI

/// What the Mac sidebar can show at the root of the detail column.
enum SidebarItem: Hashable {
    case tab(AppTab)
    case category(String)
    case guide
    case history
}

/// Mac navigation: a sidebar (app header, sections, categories, ʿUmrah guide) and one navigation
/// stack in the detail column. The Back button sits at the top of the page itself (⌘[ and Esc too);
/// the show / hide sidebar button sits in the sidebar. Clicking any sidebar item, even the one already
/// selected, shows that section's main page.
struct MacRootView: View {
    @Environment(AppModel.self) private var model
    @State private var extra: SidebarItem?
    @State private var columns: NavigationSplitViewVisibility = .all

    private var selection: Binding<SidebarItem?> {
        Binding(
            get: { extra ?? .tab(model.tab) },
            set: { item in
                guard let item else { return }
                if case .tab(let t) = item {
                    extra = nil
                    model.tab = t
                } else {
                    extra = item
                    model.tab = .home
                }
                model.homePath = []
            }
        )
    }

    var body: some View {
        @Bindable var m = model
        NavigationSplitView(columnVisibility: $columns) {
            sidebar
                .toolbar(removing: .sidebarToggle)
                .navigationSplitViewColumnWidth(min: 220, ideal: 250, max: 320)
        } detail: {
            NavigationStack(path: $m.homePath) {
                // The bar is attached to every page: pages pushed on the stack do not inherit
                // an inset set on the stack itself.
                root
                    .safeAreaInset(edge: .top, spacing: 0) { pageBar }
                    .navigationDestination(for: Route.self) { route in
                        destination(route)
                            .navigationBarBackButtonHidden(true)   // one Back control: the page bar's
                            .safeAreaInset(edge: .top, spacing: 0) { pageBar }
                    }
            }
            .onExitCommand(perform: back)
        }
        .frame(minWidth: 860, minHeight: 600)
        .onChange(of: model.tab) { _, _ in if extra != nil, model.tab != .home { extra = nil } }
        .task { await MacNavTest.runIfRequested(model: model, select: { selection.wrappedValue = $0 }, back: back) }
    }

    /// Top of the page: Back (and, when the sidebar is hidden, the button to show it again).
    @ViewBuilder
    private var pageBar: some View {
        if !atHome || columns == .detailOnly {
            pageBarContent
        }
    }

    private var pageBarContent: some View {
        HStack(spacing: 8) {
            if columns == .detailOnly {
                Button { withAnimation { columns = .all } } label: {
                    Image(systemName: "sidebar.leading").frame(height: 18)
                }
                .help(sidebarTitle)
            }
            if !atHome {   // Home's first page has nowhere to go back to
                Button(action: back) {
                    Label(backTitle, systemImage: model.lang.isRTL ? "chevron.right" : "chevron.left")
                        .frame(height: 18)
                }
                .keyboardShortcut("[", modifiers: .command)
                .help(backTitle + " (⌘[)")
            }
            Spacer()
        }
        .buttonStyle(.bordered)
        .padding(.horizontal, 16).padding(.vertical, 8)
        .background(.bar)
        .overlay(alignment: .bottom) { Divider() }
    }

    private var sidebarTitle: String {
        switch model.lang {
        case .ar: return "إظهار الشريط الجانبي أو إخفاؤه"
        case .en: return "Show or hide the sidebar"
        case .fr: return "Afficher ou masquer la barre latérale"
        }
    }

    private var backTitle: String {
        switch model.lang {
        case .ar: return "رجوع"
        case .en: return "Back"
        case .fr: return "Retour"
        }
    }

    /// Home's first page: the only place with nowhere to go back to.
    private var atHome: Bool { model.homePath.isEmpty && extra == nil && model.tab == .home }

    /// Back one page; from the first page of any other section, back to Home.
    private func back() {
        if !model.homePath.isEmpty {
            model.homePath.removeLast()
        } else if !atHome {
            selection.wrappedValue = .tab(.home)
        }
    }

    // MARK: Sidebar

    private var sidebar: some View {
        let s = model.s
        let lang = model.lang
        let data = AzkarData.shared
        return List(selection: selection) {
            Section {
                row(s.tabHome, "house.fill", .green, .tab(.home))
                row(s.tabFavorites, "heart.fill", .pink, .tab(.favorites))
                row(s.tabTasbih, "circle.hexagongrid.fill", .teal, .tab(.tasbih))
                row(s.tabSettings, "gearshape.fill", .gray, .tab(.settings))
                row(HistoryTexts.of(lang).title, "calendar", .teal, .history)
            } header: {
                header
            }
            Section(s.categories) {
                ForEach(data.categories) { cat in
                    row(s.category(cat.key), categorySymbol(cat.key), categoryHue(cat.key), .category(cat.key))
                }
            }
            if let guide = data.umrahGuide {
                Section(GuideTexts.of(lang).guide) {
                    row(guide.title(lang), "location.north.circle.fill", .brown, .guide)
                }
            }
        }
        .listStyle(.sidebar)
    }

    private var header: some View {
        HStack(spacing: 10) {
            Image(nsImage: NSApp.applicationIconImage)
                .resizable().interpolation(.high).frame(width: 34, height: 34)
            VStack(alignment: .leading, spacing: 1) {
                Text(model.s.appName).font(.headline).foregroundStyle(.primary)
                Text(Hijri.format(model.lang, Hijri.of())).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 4)
            Button { withAnimation { columns = .detailOnly } } label: {
                Image(systemName: "sidebar.leading").font(.system(size: 14, weight: .medium))
            }
            .buttonStyle(.borderless)
            .help(sidebarTitle)
        }
        .textCase(nil)
        .padding(.vertical, 8)
    }

    private func row(_ title: String, _ symbol: String, _ color: Color, _ item: SidebarItem) -> some View {
        Label {
            Text(title).lineLimit(1)
        } icon: {
            Image(systemName: symbol)
                .font(.system(size: 11, weight: .semibold)).foregroundStyle(.white)
                .frame(width: 22, height: 22)
                .background(color, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
        }
        .tag(item)
        // The list only reports a *change* of selection: clicking the section you are already in
        // (e.g. Home while reading inside it) must still go back to that section's main page.
        .contentShape(Rectangle())
        .onTapGesture { selection.wrappedValue = item }
    }

    // MARK: Detail

    @ViewBuilder
    private var root: some View {
        switch extra ?? .tab(model.tab) {
        case .tab(.favorites): FavoritesView()
        case .tab(.tasbih): TasbihView()
        case .tab(.settings): SettingsView()
        case .category(let key): CategoryView(key: key)
        case .guide: GuideView(key: "umrah")
        case .history: HistoryView()
        default: HomeView()
        }
    }

    @ViewBuilder
    private func destination(_ route: Route) -> some View {
        switch route {
        case .category(let key): CategoryView(key: key)
        case .reader(let r, let focus): ReaderView(reading: r, focus: focus)
        case .guide(let key): GuideView(key: key)
        case .history: HistoryView()
        }
    }
}

/// CI smoke test of the Mac navigation (`-ciNav 1`): walks through the sidebar, opens and closes
/// readings, logs how long the main thread took to respond to each step, saves a screenshot after
/// each step in the `-ciShotDir` folder, then quits. A step that leaves the wrong screen fails it.
enum MacNavTest {
    /// The window content is rebuilt on a language change; the test must only start once.
    @MainActor private static var started = false

    @MainActor
    static func runIfRequested(model: AppModel, select: @escaping (SidebarItem) -> Void, back: @escaping () -> Void) async {
        let d = UserDefaults.standard
        guard d.bool(forKey: "ciNav"), let dir = d.string(forKey: "ciShotDir"), !started else { return }
        started = true
        var failures = 0
        func step(_ name: String, _ action: () -> Void, expect: () -> Bool) async {
            let start = Date()
            action()
            try? await Task.sleep(for: .milliseconds(1200))
            let ok = expect()
            if !ok { failures += 1 }
            capture("\(dir)/\(name).png")
            launchTrace(String(format: "nav %@ %@ (%.2fs)", name, ok ? "ok" : "FAILED", Date().timeIntervalSince(start)))
        }
        try? await Task.sleep(for: .seconds(2))
        model.showDedication = false
        await step("01-home", { select(.tab(.home)) }, expect: { model.tab == .home && model.homePath.isEmpty })
        await step("02-open-morning", { model.show(.morning, split: false) }, expect: { model.homePath.count == 1 })
        await step("02b-click-home-while-reading", { select(.tab(.home)) }, expect: { model.homePath.isEmpty })
        await step("02c-open-morning-again", { model.show(.morning, split: false) }, expect: { model.homePath.count == 1 })
        await step("03-back", back, expect: { model.homePath.isEmpty })
        await step("04-category", { select(.category("prayer")) }, expect: { model.homePath.isEmpty })
        await step("05-open-chapter", { model.homePath.append(.reader(.chapter(25), focus: nil)) }, expect: { model.homePath.count == 1 })
        await step("06-back", back, expect: { model.homePath.isEmpty })
        await step("06b-back-to-home", back, expect: { model.tab == .home && model.homePath.isEmpty })
        await step("07-favorites", { select(.tab(.favorites)) }, expect: { model.tab == .favorites })
        await step("07b-back-to-home", back, expect: { model.tab == .home })
        await step("08-tasbih", { select(.tab(.tasbih)) }, expect: { model.tab == .tasbih })
        await step("08b-back-to-home", back, expect: { model.tab == .home })
        await step("09-settings", { select(.tab(.settings)) }, expect: { model.tab == .settings })
        await step("10-guide", { select(.guide) }, expect: { model.tab == .home })
        await step("10b-guide-open-dua", { model.show(.chapter(115), split: false) }, expect: { model.homePath.count == 1 })
        await step("10c-back", back, expect: { model.homePath.isEmpty })
        await step("10d-back-to-home", back, expect: { model.tab == .home && model.homePath.isEmpty })
        await step("10e-history", { select(.history) }, expect: { model.tab == .home && model.homePath.isEmpty })
        await step("10f-history-back", back, expect: { model.tab == .home && model.homePath.isEmpty })
        await step("09b-settings-back", { select(.tab(.settings)); back() }, expect: { model.tab == .home })
        await step("11-home-open-evening", { select(.tab(.home)); model.show(.evening, split: false) }, expect: { model.homePath.count == 1 })
        // BUG-001: changing the language, then opening a reading, crashed (layout loop). Only model
        // operations from here on: the window content is rebuilt, so `select` / `back` are stale.
        await step("12-language-ar", { model.lang = .ar }, expect: { model.lang == .ar && model.homePath.isEmpty })
        await step("13-open-morning-ar", { model.tab = .home; model.show(.morning, split: false) }, expect: { model.homePath.count == 1 })
        await step("14-language-en", { model.lang = .en }, expect: { model.homePath.isEmpty })
        await step("15-open-evening-en", { model.show(.evening, split: false) }, expect: { model.homePath.count == 1 })
        // BUG-004: a dhikr completed in a reading is completed everywhere.
        let first = AzkarData.shared.morning[0]
        await step("16-shared-progress", { model.setRemaining(first, 0) }, expect: { model.remaining(first) == 0 })
        await step("16b-history-with-record", {
            for z in AzkarData.shared.morning { model.setRemaining(z, 0) }
            for z in AzkarData.shared.items(.chapter(28)) { model.setRemaining(z, 0) }
            model.homePath = [.history]
        }, expect: { AzkarHistory.day(.now).readings.contains("morning") && AzkarHistory.day(.now).readings.contains("c28") })
        await step("16c-reader-counter", { model.homePath = []; model.show(.morning, split: false) }, expect: { model.homePath.count == 1 })
        // BUG-003: dark appearance (screenshot to review).
        await step("17-dark-home", { model.homePath = []; model.theme = .dark }, expect: { model.theme == .dark })
        await step("18-dark-reader", { model.show(.morning, split: false) }, expect: { model.homePath.count == 1 })
        // Sync between devices, against the live server: turn it on, then sync with the code.
        await step("19-sync-on", { Task { try? await AzkarSync.start() } }, expect: { true })
        try? await Task.sleep(for: .seconds(4))
        let synced = AzkarSync.code != nil && AzkarSync.lastSync != nil
        launchTrace("nav 19-sync \(synced ? "ok" : "FAILED") code \(AzkarSync.code.map(AzkarSync.display) ?? "none")")
        if !synced { failures += 1 }
        launchTrace("nav done, failures: \(failures)")
        exit(failures == 0 ? 0 : 3)
    }

    @MainActor
    static func capture(_ path: String) {
        guard let w = NSApp.windows.first(where: { $0.isVisible && $0.frame.width > 500 }),
              let v = w.contentView, let rep = v.bitmapImageRepForCachingDisplay(in: v.bounds) else { return }
        v.cacheDisplay(in: v.bounds, to: rep)
        try? rep.representation(using: .png, properties: [:])?.write(to: URL(fileURLWithPath: path))
    }
}
