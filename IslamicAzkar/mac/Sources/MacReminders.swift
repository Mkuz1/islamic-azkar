import AppKit
import SwiftUI
import UserNotifications

/// Mac reminders.
///
/// - On screen: while the app runs (it stays in the menu bar), it checks every 30 seconds whether a
///   morning or evening reminder is due and shows its own card at the top centre of the screen
///   (ReminderBanner), whatever the system's notification style or Focus is.
/// - The scheduled system notification still goes to Notification Center (it is the reminder when
///   the app is not running); while the app runs it is not shown again as a banner.
/// - The menu bar icon shows a bell while the azkar of a reminder that has come due are unread.
final class MacNotifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = MacNotifications()
    /// Opens a notification's link (set by the app once its model exists).
    var open: ((URL) -> Void)?

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        // The app's own card is on screen already for azkar reminders.
        notification.request.identifier.hasPrefix("azkar.") ? [.list] : [.banner, .list, .sound]
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        guard let link = response.notification.request.content.userInfo["link"] as? String, let url = URL(string: link) else { return }
        await MainActor.run {
            open?(url)
            NSApp.activate(ignoringOtherApps: true)
        }
    }
}

enum MacReminders {
    struct Due { let reading: Reading; let id: String; let at: Int }

    private static var d: UserDefaults { SharedStore.defaults }

    /// Morning / evening reminders that are switched on, with their time (minutes after midnight).
    private static var configured: [Due] {
        var out: [Due] = []
        if d.bool(forKey: "morning_on") { out.append(Due(reading: .morning, id: "azkar.morning", at: d.object(forKey: "morning_at") == nil ? 360 : d.integer(forKey: "morning_at"))) }
        if d.bool(forKey: "evening_on") { out.append(Due(reading: .evening, id: "azkar.evening", at: d.object(forKey: "evening_at") == nil ? 1020 : d.integer(forKey: "evening_at"))) }
        return out
    }

    private static func minutes(_ date: Date) -> Int {
        let c = Calendar.current.dateComponents([.hour, .minute], from: date)
        return (c.hour ?? 0) * 60 + (c.minute ?? 0)
    }

    /// A reminder whose time has come today (within 6 hours) and whose azkar are not read yet.
    static func pending(at now: Date = .now) -> Due? {
        let m = minutes(now)
        return configured.first { $0.at <= m && m - $0.at < 6 * 60 && !DailyAzkar.doneToday($0.reading) }
    }

    /// Called every 30 seconds: once a reminder's time has come (up to 30 minutes late, e.g. after
    /// sleep), show the on-screen card once that day, unless those azkar were already read.
    @MainActor
    static func check(at now: Date = .now) {
        let m = minutes(now)
        let day = Calendar.current.ordinality(of: .day, in: .era, for: now) ?? 0
        for due in configured where m >= due.at && m - due.at < 30 {
            let key = "mac_banner_\(due.id)"
            guard d.integer(forKey: key) != day else { continue }
            d.set(day, forKey: key)
            if !DailyAzkar.doneToday(due.reading) { ReminderBanner.show(due.reading) }
        }
    }

    /// Posts the reminder for `reading` now.
    static func post(_ reading: Reading, id: String) {
        let s = L10n.of(SharedStore.lang)
        let content = UNMutableNotificationContent()
        content.title = reading == .morning ? s.notifMorningTitle : s.notifEveningTitle
        content.body = reading == .morning ? s.notifMorningBody : s.notifEveningBody
        content.sound = .default
        content.userInfo = ["link": DeepLink.url(reading).absoluteString]
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: id, content: content, trigger: nil))
    }

    /// Settings → "Send a test reminder": asks for permission if needed, then posts one in 3 seconds.
    /// Returns false when notifications are off for the app (then System Settings is opened).
    @MainActor
    static func sendTest() async -> Bool {
        // The on-screen card does not need the notification permission: it always shows.
        let shown = Reading.recommended() == .evening ? Reading.evening : .morning
        Task { try? await Task.sleep(for: .seconds(3)); ReminderBanner.show(shown) }
        let center = UNUserNotificationCenter.current()
        _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
        let settings = await center.notificationSettings()
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional,
              settings.alertSetting != .disabled else {
            if let url = URL(string: "x-apple.systempreferences:com.apple.Notifications-Settings.extension?id=\(Bundle.main.bundleIdentifier ?? "")") {
                NSWorkspace.shared.open(url)
            }
            return false
        }
        let reading = Reading.recommended() == .evening ? Reading.evening : .morning
        let s = L10n.of(SharedStore.lang)
        let content = UNMutableNotificationContent()
        content.title = reading == .morning ? s.notifMorningTitle : s.notifEveningTitle
        content.body = reading == .morning ? s.notifMorningBody : s.notifEveningBody
        content.sound = .default
        content.userInfo = ["link": DeepLink.url(reading).absoluteString]
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3, repeats: false)
        try? await center.add(UNNotificationRequest(identifier: "azkar.test", content: content, trigger: trigger))
        return true
    }
}

/// Settings row (Mac): sends a test reminder, or explains how to allow notifications.
struct MacTestReminderRow: View {
    @Environment(AppModel.self) private var model
    @State private var status: String?

    var body: some View {
        let t = texts
        VStack(alignment: .leading, spacing: 4) {
            Button {
                Task {
                    let ok = await MacReminders.sendTest()
                    status = ok ? t.sent : t.off
                }
            } label: { Label(t.button, systemImage: "bell.badge") }
            if let status { Text(status).font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true) }
        }
    }

    private var texts: (button: String, sent: String, off: String) {
        switch model.lang {
        case .ar: return ("إرسال تذكير تجريبي", "سيظهر التذكير أعلى الشاشة خلال ثوانٍ.",
                          "سيظهر التذكير أعلى الشاشة خلال ثوانٍ. ولتصلك التذكيرات والتطبيق مغلق أيضًا، فعّل إشعاراته في إعدادات النظام ← الإشعارات ← Islamic Azkar.")
        case .en: return ("Send a test reminder", "The reminder will appear at the top of the screen in a few seconds.",
                          "The reminder will appear at the top of the screen in a few seconds. To also get reminders while the app is closed, turn on its notifications in System Settings → Notifications → Islamic Azkar.")
        case .fr: return ("Envoyer un rappel de test", "Le rappel va apparaître en haut de l'écran dans quelques secondes.",
                          "Le rappel va apparaître en haut de l'écran dans quelques secondes. Pour recevoir aussi les rappels quand l'application est fermée, activez ses notifications dans Réglages Système → Notifications → Islamic Azkar.")
        }
    }
}
