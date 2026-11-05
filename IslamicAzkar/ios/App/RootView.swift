import SwiftUI

private struct SplitKey: EnvironmentKey { static let defaultValue = false }

extension EnvironmentValues {
    /// True inside the iPad / landscape split view, where readings open in the detail column.
    var inSplitView: Bool {
        get { self[SplitKey.self] }
        set { self[SplitKey.self] = newValue }
    }
}

extension AppModel {
    /// Opens a reading in the detail column (split view) or pushes it (iPhone).
    func show(_ reading: Reading, focus: Int? = nil, split: Bool) {
        if split {
            detail = reading
            detailFocus = focus
        } else {
            homePath.append(.reader(reading, focus: focus))
        }
    }
}

struct RootView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var m = model
        let s = model.s
        TabView(selection: $m.tab) {
            Tab(s.tabHome, systemImage: "house.fill", value: AppTab.home) { HomeTab() }
            Tab(s.tabFavorites, systemImage: "heart.fill", value: AppTab.favorites) {
                NavigationStack { FavoritesView() }
            }
            Tab(s.tabTasbih, systemImage: "circle.hexagongrid.fill", value: AppTab.tasbih) {
                NavigationStack { TasbihView() }
            }
            #if os(iOS)
            Tab(model.m.tabWidgets, systemImage: "square.grid.2x2.fill", value: AppTab.widgets) {
                NavigationStack { WidgetsView() }
            }
            #endif
            Tab(s.tabSettings, systemImage: "gearshape.fill", value: AppTab.settings) {
                NavigationStack { SettingsView() }
            }
        }
        .tabViewStyle(.sidebarAdaptable)
        .modifier(MinimizeTabBarOnScroll())
        .sheet(item: $m.shareTarget) { ShareSheetView(target: $0) }
        .overlay(alignment: .bottom) {
            if let t = model.toast {
                ToastView(text: t)
                    .padding(.bottom, 96)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .task {
                        try? await Task.sleep(for: .seconds(1.6))
                        withAnimation { model.toast = nil }
                    }
            }
        }
        .animation(.spring(duration: 0.35), value: model.toast)
    }
}

/// iOS 26: the floating Liquid Glass tab bar tucks away while reading.
private struct MinimizeTabBarOnScroll: ViewModifier {
    func body(content: Content) -> some View {
        #if os(iOS)
        if #available(iOS 26.0, *) {
            content.tabBarMinimizeBehavior(.onScrollDown)
        } else {
            content
        }
        #else
        content
        #endif
    }
}

/// Home: one full-width navigation stack on every device (no sidebar), content centred on wide screens.
struct HomeTab: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var m = model
        NavigationStack(path: $m.homePath) {
            HomeView()
                .navigationDestination(for: Route.self) { destination($0) }
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
