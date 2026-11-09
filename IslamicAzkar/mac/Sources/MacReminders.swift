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
        let id = notification.request.identifier
        guard id.hasPrefix("azkar.") else { return [.banner, .list, .sound] }
        // The app runs: show its own card at the top of the screen (macOS may keep a running menu-bar
        // app's banners in Notification Center), and keep the notification in the list.
        let c = notification.request.content
        let link = c.userInfo["link"] as? String
        await MainActor.run { ReminderBanner.show(notification: id, title: c.title, body: c.body, link: link) }
        return [.list]
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
        for due in configured where m >= due.at && m - due.at < 30 {
            guard ReminderBanner.claim(due.id) else { continue }
            if !DailyAzkar.doneToday(due.reading) { ReminderBanner.show(due.reading) }
        }
        // The dhikr reminders through the day: the app shows them itself, at their times, since macOS
        // does not tell a background app when its notifications arrive.
        for slot in generalSlots where m >= slot && m - slot < 10 {
            let id = "azkar.general.\(slot)"
            guard ReminderBanner.claim(id) else { continue }
            showGeneral(id)
        }
    }

    /// Minutes after midnight of the dhikr reminders through the day (as Reminders schedules them).
    private static var generalSlots: [Int] {
        guard d.bool(forKey: "general_on") else { return [] }
        let every = max(1, d.object(forKey: "general_every") == nil ? 3 : d.integer(forKey: "general_every"))
        let from = d.object(forKey: "general_from") == nil ? 9 * 60 : d.integer(forKey: "general_from")
        let to = max(d.object(forKey: "general_to") == nil ? 21 * 60 : d.integer(forKey: "general_to"), from)
        return Array(stride(from: from, through: to, by: every * 60).prefix(48))
    }

    /// The card for a dhikr reminder, with the same dhikr as its scheduled notification.
    private static func showGeneral(_ id: String) {
        UNUserNotificationCenter.current().getPendingNotificationRequests { pending in
            let c = pending.first { $0.identifier == id }?.content
            let title = c?.title ?? L10n.of(SharedStore.lang).notifGeneralTitle
            let body = c?.body ?? {
                let z = AzkarData.shared.shortAzkar.randomElement()!
                let meaning = z.meaning(SharedStore.lang)
                return meaning.isEmpty ? z.ar : "\(z.ar)\n\(meaning)"
            }()
            let link = c?.userInfo["link"] as? String
            DispatchQueue.main.async {
                ReminderBanner.show(ReminderBanner.content(id: id, title: title, body: body, link: link))
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
            // The on-screen card does not need the notification permission.
            let shown = Reading.recommended() == .evening ? Reading.evening : .morning
            Task { try? await Task.sleep(for: .seconds(3)); ReminderBanner.show(shown) }
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
