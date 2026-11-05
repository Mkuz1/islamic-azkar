import AppKit
import SwiftUI
import UserNotifications

/// Mac reminders.
///
/// - The app stays running in the menu bar, and macOS hides a running app's notifications unless
///   the app says how to show them: this delegate asks for a banner with sound.
/// - Backup: while the app runs, it checks every 30 seconds whether a morning or evening reminder
///   is due; if the scheduled notification did not arrive, it posts one itself.
/// - The menu bar icon shows a bell while the azkar of a reminder that has come due are unread.
final class MacNotifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = MacNotifications()
    /// Opens a notification's link (set by the app once its model exists).
    var open: ((URL) -> Void)?

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound]
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

    /// Called every 30 seconds: one minute after a reminder's time, post it if none was delivered.
    static func check(at now: Date = .now) {
        let m = minutes(now)
        let day = Calendar.current.ordinality(of: .day, in: .era, for: now) ?? 0
        for due in configured where m >= due.at + 1 && m - due.at < 10 {
            let key = "mac_reminder_\(due.id)"
            guard d.integer(forKey: key) != day else { continue }
            d.set(day, forKey: key)
            let center = UNUserNotificationCenter.current()
            center.getDeliveredNotifications { delivered in
                let recent = delivered.contains { $0.request.identifier == due.id && now.timeIntervalSince($0.date) < 15 * 60 }
                if !recent { post(due.reading, id: due.id + ".backup") }
            }
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
        case .ar: return ("إرسال تذكير تجريبي", "سيصلك تذكير خلال ثوانٍ. إن لم يظهر، فعّل إشعارات «أذكار إسلامية» في إعدادات النظام ← الإشعارات.",
                          "الإشعارات متوقفة لهذا التطبيق. فعّلها في إعدادات النظام ← الإشعارات ← Islamic Azkar.")
        case .en: return ("Send a test reminder", "A reminder will appear in a few seconds. If it doesn't, allow notifications for Islamic Azkar in System Settings → Notifications.",
                          "Notifications are off for this app. Turn them on in System Settings → Notifications → Islamic Azkar.")
        case .fr: return ("Envoyer un rappel de test", "Un rappel va apparaître dans quelques secondes. Sinon, autorisez les notifications d'Islamic Azkar dans Réglages Système → Notifications.",
                          "Les notifications sont désactivées pour cette application. Activez-les dans Réglages Système → Notifications → Islamic Azkar.")
        }
    }
}
