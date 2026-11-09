import AppKit
import ServiceManagement
import SwiftUI

/// macOS only: keep running in the menu bar when the last window closes.
final class MacAppDelegate: NSObject, NSApplicationDelegate {
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }

    func applicationDidFinishLaunching(_ notification: Notification) {
        launchTrace("didFinishLaunching")
        MacCI.start()
    }
}

/// CI and diagnostics: `-ciMenu 1` shows the menu bar panel in a plain window; `-ciBanner morning`
/// shows the on-screen reminder; `-ciShot <file.png>`
/// saves the front window as a PNG after a few seconds, then quits. Started once, from whichever
/// comes first: the app delegate or the main window appearing.
enum MacCI {
    @MainActor private static var started = false

    @MainActor static func start() {
        guard !started else { return }
        started = true
        if UserDefaults.standard.bool(forKey: "ciMenu") { MenuPanelPreviewWindow.show() }
        if let key = UserDefaults.standard.string(forKey: "ciBanner") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                if let r = Reading(key: key) { ReminderBanner.show(r) }
                else {   // a dhikr reminder through the day, as the notification delegate receives it
                    let s = L10n.of(SharedStore.lang)
                    let z = AzkarData.shared.shortAzkar[3]
                    ReminderBanner.show(notification: "azkar.general.540", title: s.notifGeneralTitle,
                                        body: z.meaning(SharedStore.lang).isEmpty ? z.ar : "\(z.ar)\n\(z.meaning(SharedStore.lang))", link: nil)
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 4) {
                let w = ReminderBanner.window
                launchTrace("banner on screen: visible=\(w?.isVisible ?? false) occluded=\(!(w?.occlusionState.contains(.visible) ?? false)) alpha=\(w?.alphaValue ?? 0) frame=\(w?.frame.debugDescription ?? "none") screen=\(NSScreen.main?.visibleFrame.debugDescription ?? "?")")
            }
        }
        guard let path = UserDefaults.standard.string(forKey: "ciShot") else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 6) {
            let w = ReminderBanner.window ?? MenuPanelPreviewWindow.window ?? NSApp.windows.first { $0.isVisible && $0.frame.width > 300 }
            launchTrace("capture: \(NSApp.windows.count) windows, chosen \(w?.frame.size.debugDescription ?? "none")")
            if let v = w?.contentView, let rep = v.bitmapImageRepForCachingDisplay(in: v.bounds) {
                v.cacheDisplay(in: v.bounds, to: rep)
                try? rep.representation(using: .png, properties: [:])?.write(to: URL(fileURLWithPath: path))
            }
            NSApp.terminate(nil)
        }
    }
}

/// "Open at login", through the system's login items.
enum LaunchAtLogin {
    static var isOn: Bool { SMAppService.mainApp.status == .enabled }

    static func set(_ on: Bool) {
        do {
            if on { try SMAppService.mainApp.register() } else { try SMAppService.mainApp.unregister() }
        } catch {
            NSLog("Launch at login: \(error.localizedDescription)")
        }
    }
}

/// Brings the main window forward (or opens it if it was closed).
@MainActor
func showMainWindow(_ openWindow: OpenWindowAction) {
    if let w = NSApp.windows.first(where: { $0.identifier?.rawValue.hasPrefix("main") == true && $0.canBecomeMain }) {
        w.deminiaturize(nil)
        w.makeKeyAndOrderFront(nil)
    } else {
        openWindow(id: "main")
    }
    NSApp.activate(ignoringOtherApps: true)
}

/// CI only: the menu bar panel in an ordinary window so it can be screenshotted.
enum MenuPanelPreviewWindow {
    @MainActor static var window: NSWindow?

    @MainActor static func show() {
        let model = AppModel()
        let view = MenuBarPanel()
            .environment(model)
            .environment(\.layoutDirection, model.lang.isRTL ? .rightToLeft : .leftToRight)
            .environment(\.locale, model.lang.locale)
            .tint(Theme.accent)
        let w = NSWindow(contentViewController: NSHostingController(rootView: view))
        w.title = "Islamic Azkar"
        w.setFrameTopLeftPoint(NSPoint(x: 40, y: (NSScreen.main?.frame.height ?? 900) - 40))
        w.makeKeyAndOrderFront(nil)
        window = w
        NSApp.activate(ignoringOtherApps: true)
    }
}
