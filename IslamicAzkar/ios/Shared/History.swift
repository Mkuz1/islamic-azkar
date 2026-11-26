import Foundation

/// Daily record of completed azkar, kept on the device: for each day ("2026-11-06"), the readings
/// completed in full (e.g. "morning", "evening", "c28" — any chapter of any category).
enum AzkarHistory {
    struct Day: Codable, Equatable {
        var zikr: [Int] = []          // kept for data saved by v1.0.12; no longer shown
        var readings: [String] = []
        var count: Int { readings.count }
    }

    private static let key = "azkar_history"

    static func dayKey(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    static func all() -> [String: Day] {
        guard let data = SharedStore.defaults.data(forKey: key),
              let days = try? JSONDecoder().decode([String: Day].self, from: data) else { return [:] }
        return days
    }

    static func day(_ date: Date) -> Day { all()[dayKey(date)] ?? Day() }

    private static func update(_ date: Date, _ change: (inout Day) -> Void) {
        var days = all()
        var day = days[dayKey(date)] ?? Day()
        change(&day)
        days[dayKey(date)] = day
        if let data = try? JSONEncoder().encode(days) { SharedStore.defaults.set(data, forKey: key) }
    }

    /// A whole reading (morning, evening, a chapter) was completed.
    static func markReading(_ key: String, at date: Date = .now) {
        let isNew = !day(date).readings.contains(key)
        update(date) { if !$0.readings.contains(key) { $0.readings.append(key) } }
        if isNew { Task { await AzkarSync.syncNow() } }   // show it on the other devices
    }

    /// Day → completed readings, as sent to the other devices.
    static func readingsByDay() -> [String: [String]] {
        all().compactMapValues { $0.readings.isEmpty ? nil : $0.readings }
    }

    /// Adds readings completed on the other devices.
    static func merge(_ remote: [String: [String]]) {
        var days = all()
        var changed = false
        for (key, list) in remote {
            var day = days[key] ?? Day()
            for r in list where !day.readings.contains(r) { day.readings.append(r); changed = true }
            days[key] = day
        }
        if changed, let data = try? JSONEncoder().encode(days) { SharedStore.defaults.set(data, forKey: Self.key) }
    }

    /// Consecutive days on which `key` (e.g. "morning", "c28") was completed, ending today (or
    /// yesterday, if it is not done yet today). Counts every device's completions (the record syncs).
    static func streak(_ key: String, now: Date = .now) -> Int {
        let days = all(), cal = Calendar.current
        var date = now
        if days[dayKey(date)]?.readings.contains(key) != true { date = cal.date(byAdding: .day, value: -1, to: date) ?? date }
        var n = 0
        while days[dayKey(date)]?.readings.contains(key) == true {
            n += 1
            date = cal.date(byAdding: .day, value: -1, to: date) ?? date
        }
        return n
    }

    /// Consecutive days with at least one completed reading, ending today (or yesterday).
    static func currentStreak(_ days: [String: Day], now: Date = .now) -> Int {
        let cal = Calendar.current
        var date = now
        if (days[dayKey(date)]?.count ?? 0) == 0 { date = cal.date(byAdding: .day, value: -1, to: date) ?? date }
        var n = 0
        while (days[dayKey(date)]?.count ?? 0) > 0 {
            n += 1
            date = cal.date(byAdding: .day, value: -1, to: date) ?? date
        }
        return n
    }
}
