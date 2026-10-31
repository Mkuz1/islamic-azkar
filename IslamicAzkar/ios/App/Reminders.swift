import Foundation
import UserNotifications

/// Local notifications: daily morning / evening reminders and dhikr reminders through the day.
enum Reminders {
    private static let morningID = "azkar.morning"
    private static let eveningID = "azkar.evening"
    private static let generalPrefix = "azkar.general."

    /// Asks for permission the first time a reminder is switched on.
    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    static func reschedule(_ m: AppModel) {
        let center = UNUserNotificationCenter.current()
        let s = m.s
        let lang = m.lang
        let morning = (m.morningOn, m.morningAt)
        let evening = (m.eveningOn, m.eveningAt)
        let general = (m.generalOn, m.generalEvery, m.generalFrom, m.generalTo)

        center.getPendingNotificationRequests { pending in
            center.removePendingNotificationRequests(withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix("azkar.") })

            if morning.0 {
                add(center, id: morningID, at: morning.1, title: s.notifMorningTitle, body: s.notifMorningBody,
                    link: DeepLink.url(.morning))
            }
            if evening.0 {
                add(center, id: eveningID, at: evening.1, title: s.notifEveningTitle, body: s.notifEveningBody,
                    link: DeepLink.url(.evening))
            }
            if general.0 {
                // One repeating request per slot; each slot gets its own dhikr (refreshed whenever the app opens).
                let pool = AzkarData.shared.shortAzkar.shuffled()
                var slot = general.2
                var i = 0
                let end = max(general.3, general.2)
                while slot <= end && i < 48 {
                    let z = pool[i % pool.count]
                    let meaning = z.meaning(lang)
                    add(center, id: generalPrefix + "\(slot)", at: slot, title: s.notifGeneralTitle,
                        body: meaning.isEmpty ? z.ar : "\(z.ar)\n\(meaning)",
                        link: DeepLink.url(AzkarData.shared.reading(ofZikr: z.id) ?? .morning, focus: z.id))
                    slot += general.1 * 60
                    i += 1
                }
            }
        }
    }

    private static func add(_ center: UNUserNotificationCenter, id: String, at minutes: Int, title: String, body: String, link: URL) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = ["link": link.absoluteString]
        content.threadIdentifier = "azkar"
        var when = DateComponents()
        when.hour = minutes / 60
        when.minute = minutes % 60
        let trigger = UNCalendarNotificationTrigger(dateMatching: when, repeats: true)
        center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }
}
