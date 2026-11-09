import AppKit
import SwiftUI

/// The Mac's own reminder: a card that slides down at the top centre of the screen, over every app
/// and in every Space (full screen too), whatever the system's notification style is. It stays until
/// the user reads or dismisses it (or for 10 minutes).
@MainActor
enum ReminderBanner {
    private static var panel: NSPanel?
    private static var hideTask: Task<Void, Never>?
    /// Opens the main window (set by the menu bar item, which always exists).
    static var openMain: (() -> Void)?
    /// For CI screenshots.
    static var window: NSWindow? { panel }

    /// What the card shows.
    struct Content {
        var title: String
        var body: String
        var preview: String?   // a short dhikr in Arabic
        var morning: Bool
        var link: URL
    }

    /// The morning or evening azkar reminder.
    static func show(_ reading: Reading) {
        let s = L10n.of(SharedStore.lang)
        let morning = reading == .morning
        let preview = AzkarData.shared.items(reading).map(\.ar).first { (12...140).contains($0.count) && !$0.contains("﴿") }
        show(Content(title: morning ? s.notifMorningTitle : s.notifEveningTitle,
                     body: morning ? s.notifMorningBody : s.notifEveningBody,
                     preview: preview, morning: morning, link: DeepLink.url(reading)))
    }

    /// Any of the app's reminders, as it arrives while the app runs (morning, evening, the dhikr
    /// reminders through the day, the test): shown on screen instead of only in Notification Center.
    static func show(notification id: String, title: String, body: String, link: String?) {
        let day = Calendar.current.ordinality(of: .day, in: .era, for: .now) ?? 0
        if id == "azkar.morning" || id == "azkar.evening" {
            // The 30-second check may have shown it already.
            let key = "mac_banner_\(id)"
            if SharedStore.defaults.integer(forKey: key) == day, panel != nil { return }
            SharedStore.defaults.set(day, forKey: key)
        }
        let url = link.flatMap(URL.init(string:)) ?? DeepLink.url(Reading.recommended())
        // The dhikr reminders carry the dhikr itself: Arabic on the first line, the meaning after it.
        let lines = body.components(separatedBy: "\n")
        let general = id.hasPrefix("azkar.general.")
        show(Content(title: title,
                     body: general ? (lines.dropFirst().joined(separator: " ")) : body,
                     preview: general ? lines.first : nil,
                     morning: id != "azkar.evening" && (id != "azkar.test" || Reading.recommended() != .evening),
                     link: url))
    }

    static func show(_ content: Content) {
        dismiss(animated: false)
        let lang = SharedStore.lang
        let view = BannerCard(content: content, lang: lang,
                              onRead: { open(content.link) },
                              onClose: { dismiss() })
            .environment(\.layoutDirection, lang.isRTL ? .rightToLeft : .leftToRight)
            .environment(\.locale, lang.locale)
        let host = NSHostingView(rootView: view)
        let size = NSSize(width: 460, height: host.fittingSize.height)

        let p = BannerPanel(contentRect: NSRect(origin: .zero, size: size),
                        styleMask: [.borderless, .nonactivatingPanel], backing: .buffered, defer: false)
        p.isFloatingPanel = true
        p.level = .statusBar
        p.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary, .ignoresCycle]
        p.backgroundColor = .clear
        p.isOpaque = false
        p.hasShadow = false   // the card draws its own
        p.hidesOnDeactivate = false
        p.isMovableByWindowBackground = false
        p.contentView = host

        let screen = NSScreen.main ?? NSScreen.screens.first
        let area = screen?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1440, height: 900)
        let x = area.midX - size.width / 2
        let end = NSRect(x: x, y: area.maxY - size.height - 8, width: size.width, height: size.height)
        p.setFrame(end.offsetBy(dx: 0, dy: 40), display: false)
        p.alphaValue = 0
        p.orderFrontRegardless()
        NSAnimationContext.runAnimationGroup { ctx in
            ctx.duration = 0.45
            ctx.timingFunction = CAMediaTimingFunction(controlPoints: 0.2, 0.9, 0.25, 1)
            p.animator().setFrame(end, display: true)
            p.animator().alphaValue = 1
        }
        NSSound(named: "Glass")?.play()
        panel = p

        hideTask = Task {
            try? await Task.sleep(for: .seconds(600))
            if !Task.isCancelled { dismiss() }
        }
    }

    static func dismiss(animated: Bool = true) {
        hideTask?.cancel(); hideTask = nil
        guard let p = panel else { return }
        panel = nil
        guard animated else { p.orderOut(nil); return }
        NSAnimationContext.runAnimationGroup({ ctx in
            ctx.duration = 0.25
            p.animator().setFrame(p.frame.offsetBy(dx: 0, dy: 30), display: true)
            p.animator().alphaValue = 0
        }, completionHandler: { p.orderOut(nil) })
    }

    private static func open(_ link: URL) {
        dismiss()
        MacNotifications.shared.open?(link)
        if let w = NSApp.windows.first(where: { $0.identifier?.rawValue.hasPrefix("main") == true && $0.canBecomeMain }) {
            w.deminiaturize(nil)
            w.makeKeyAndOrderFront(nil)
        } else {
            openMain?()
        }
        NSApp.activate(ignoringOtherApps: true)
    }
}

/// A borderless panel that still takes clicks and the Return key without activating the app.
private final class BannerPanel: NSPanel {
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { false }
}

/// The card: app icon and the reminder, a short dhikr from the reading, and Read now / Later.
private struct BannerCard: View {
    let content: ReminderBanner.Content
    let lang: Lang
    let onRead: () -> Void
    let onClose: () -> Void
    @State private var hover = false

    var body: some View {
        let s = L10n.of(lang)
        let morning = content.morning
        let preview = content.preview
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                ZStack(alignment: .bottomTrailing) {
                    Image(nsImage: NSApp.applicationIconImage).resizable().interpolation(.high)
                        .frame(width: 46, height: 46)
                    Image(systemName: morning ? "sun.max.fill" : "moon.stars.fill")
                        .font(.system(size: 11, weight: .bold)).foregroundStyle(.white)
                        .frame(width: 20, height: 20)
                        .background(morning ? Color.orange : Color.indigo, in: Circle())
                        .overlay(Circle().strokeBorder(.white.opacity(0.9), lineWidth: 1.5))
                        .offset(x: 4, y: 4)
                }
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(s.appName).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                        Text("·").foregroundStyle(.tertiary)
                        Text(Date.now, style: .time).font(.caption).foregroundStyle(.secondary)
                    }
                    Text(content.title)
                        .font(.system(size: 17, weight: .bold))
                    if !content.body.isEmpty {
                        Text(content.body)
                            .font(.callout).foregroundStyle(.secondary).lineLimit(3).fixedSize(horizontal: false, vertical: true)
                    }
                }
                Spacer(minLength: 0)
                Button(action: onClose) {
                    Image(systemName: "xmark").font(.system(size: 10, weight: .bold)).foregroundStyle(.secondary)
                        .frame(width: 22, height: 22).background(Color.primary.opacity(0.08), in: Circle())
                }
                .buttonStyle(.plain)
                .opacity(hover ? 1 : 0.6)
            }
            if let preview {
                Text(preview)
                    .font(.amiri(17)).lineLimit(3).multilineTextAlignment(.center)
                    .environment(\.layoutDirection, .rightToLeft)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 12).padding(.vertical, 10)
                    .background(Theme.accent.opacity(0.10), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            HStack(spacing: 10) {
                Button(action: onClose) { Text(laterTitle) }
                    .buttonStyle(BannerButtonStyle(prominent: false))
                Button(action: onRead) { Text(MacTexts.of(lang).readNow) }
                    .buttonStyle(BannerButtonStyle(prominent: true))
                    .keyboardShortcut(.defaultAction)
            }
        }
        .padding(16)
        .frame(width: 440)
        .background {
            RoundedRectangle(cornerRadius: 24, style: .continuous).fill(.regularMaterial)
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(LinearGradient(colors: [Theme.accent.opacity(0.10), .clear], startPoint: .top, endPoint: .bottom))
        }
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(Color.primary.opacity(0.10), lineWidth: 1))
        .shadow(color: .black.opacity(0.25), radius: 18, y: 8)
        .padding(10)   // room for the shadow inside the panel
        .onHover { hover = $0 }
    }

    private var laterTitle: String {
        switch lang {
        case .ar: return "لاحقًا"
        case .en: return "Later"
        case .fr: return "Plus tard"
        }
    }
}

/// The card's buttons keep their colours even though the app is not the active one (the system
/// styles turn grey then): green with white text for Read now, a soft fill for Later.
private struct BannerButtonStyle: ButtonStyle {
    let prominent: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(prominent ? Color.white : Color.primary)
            .frame(maxWidth: .infinity, minHeight: 34)
            .background(prominent ? AnyShapeStyle(Theme.accent) : AnyShapeStyle(Color.primary.opacity(0.08)),
                        in: Capsule())
            .contentShape(Capsule())
            .opacity(configuration.isPressed ? 0.75 : 1)
    }
}
