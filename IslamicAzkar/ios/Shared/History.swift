import Foundation

/// Daily record of finished azkar, kept on the device: for each day ("2026-11-06"), the dhikr ids
/// finished that day and the readings completed in full (e.g. "morning", "evening", "c28").
enum AzkarHistory {
    struct Day: Codable, Equatable {
        var zikr: [Int] = []
        var readings: [String] = []
        var count: Int { zikr.count }
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

    /// A dhikr reached its last repetition.
    static func markZikr(_ id: Int, at date: Date = .now) {
        update(date) { if !$0.zikr.contains(id) { $0.zikr.append(id) } }
    }

    /// A whole reading (morning, evening, a chapter) was completed.
    static func markReading(_ key: String, at date: Date = .now) {
        update(date) { if !$0.readings.contains(key) { $0.readings.append(key) } }
    }

    /// Consecutive days with at least one finished dhikr, ending today (or yesterday).
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
