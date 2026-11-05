import AppKit
import SwiftUI

/// What the Mac sidebar can show at the root of the detail column.
enum SidebarItem: Hashable {
    case tab(AppTab)
    case category(String)
    case guide
}

/// Mac navigation: a sidebar (app header, sections, categories, ʿUmrah guide) and one navigation
/// stack in the detail column with a toolbar back button, ⌘[ and Esc to go back.
struct MacRootView: View {
    @Environment(AppModel.self) private var model
    @State private var extra: SidebarItem?

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
        NavigationSplitView {
            sidebar
                .navigationSplitViewColumnWidth(min: 220, ideal: 250, max: 320)
        } detail: {
            NavigationStack(path: $m.homePath) {
                root
                    .navigationDestination(for: Route.self) { destination($0) }
            }
            .toolbar {
                ToolbarItem(placement: .navigation) {
                    Button(action: back) {
                        Label(backTitle, systemImage: model.lang.isRTL ? "chevron.right" : "chevron.left")
                    }
                    .keyboardShortcut("[", modifiers: .command)
                    .disabled(model.homePath.isEmpty)
                    .help(backTitle)
                }
            }
            .onExitCommand(perform: back)
        }
        .frame(minWidth: 860, minHeight: 600)
        .onChange(of: model.tab) { _, _ in if extra != nil, model.tab != .home { extra = nil } }
        .task { await MacNavTest.runIfRequested(model: model, select: { selection.wrappedValue = $0 }) }
    }

    private var backTitle: String {
        switch model.lang {
        case .ar: return "رجوع"
        case .en: return "Back"
        case .fr: return "Retour"
        }
    }

    private func back() {
        guard !model.homePath.isEmpty else { return }
        model.homePath.removeLast()
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
        default: HomeView()
        }
    }

    @ViewBuilder
    private func destination(_ route: Route) -> some View {
        switch route {
        case .category(let key): CategoryView(key: key)
        case .reader(let r, let focus): ReaderView(reading: r, focus: focus)
        case .guide(let key): GuideView(key: key)
        }
    }
}

/// CI smoke test of the Mac navigation (`-ciNav 1`): walks through the sidebar, opens and closes
/// readings, logs how long the main thread took to respond to each step, saves a screenshot after
/// each step in the `-ciShotDir` folder, then quits. A step that leaves the wrong screen fails it.
enum MacNavTest {
    @MainActor
    static func runIfRequested(model: AppModel, select: @escaping (SidebarItem) -> Void) async {
        let d = UserDefaults.standard
        guard d.bool(forKey: "ciNav"), let dir = d.string(forKey: "ciShotDir") else { return }
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
        await step("03-back", { model.homePath.removeLast() }, expect: { model.homePath.isEmpty })
        await step("04-category", { select(.category("prayer")) }, expect: { model.homePath.isEmpty })
        await step("05-open-chapter", { model.homePath.append(.reader(.chapter(25), focus: nil)) }, expect: { model.homePath.count == 1 })
        await step("06-back", { model.homePath.removeLast() }, expect: { model.homePath.isEmpty })
        await step("07-favorites", { select(.tab(.favorites)) }, expect: { model.tab == .favorites })
        await step("08-tasbih", { select(.tab(.tasbih)) }, expect: { model.tab == .tasbih })
        await step("09-settings", { select(.tab(.settings)) }, expect: { model.tab == .settings })
        await step("10-guide", { select(.guide) }, expect: { model.tab == .home })
        await step("11-home-open-evening", { select(.tab(.home)); model.show(.evening, split: false) }, expect: { model.homePath.count == 1 })
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
