import AppKit
import ServiceManagement
import SwiftUI

/// macOS only: keep running in the menu bar when the last window closes.
final class MacAppDelegate: NSObject, NSApplicationDelegate {
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }

    func applicationDidFinishLaunching(_ notification: Notification) {
        // CI screenshots of the menu bar panel: `-ciMenu 1` shows it in a plain window.
        if UserDefaults.standard.bool(forKey: "ciMenu") { MenuPanelPreviewWindow.show() }
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
        NSApp.activate(ignoringOtherApps: true)
    }
}
