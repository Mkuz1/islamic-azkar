import SwiftUI

/// Texts for the Record page.
struct HistoryTexts {
    let title, subtitle, activeDays, finished, streak, nothing, completed, today, open, tab: String
    let days: (Int) -> String

    static func of(_ lang: Lang) -> HistoryTexts {
        switch lang {
        case .ar: return HistoryTexts(
            title: "سجلّ الأذكار", subtitle: "الأذكار التي أتممتها كل يوم",
            activeDays: "أيام الذكر هذا الشهر", finished: "أذكار أتممتها هذا الشهر", streak: "أيام متتالية",
            nothing: "لم تُتمّ أذكارًا في هذا اليوم.", completed: "الأذكار المكتملة", today: "اليوم",
            open: "سجلّي", tab: "سجلّي", days: { Lang.ar.num($0) })
        case .en: return HistoryTexts(
            title: "Azkar record", subtitle: "The azkar you completed each day",
            activeDays: "Days with azkar this month", finished: "Azkar completed this month", streak: "Days in a row",
            nothing: "No azkar completed on this day.", completed: "Azkar completed", today: "Today",
            open: "My record", tab: "Record", days: { String($0) })
        case .fr: return HistoryTexts(
            title: "Historique des adhkar", subtitle: "Les adhkar terminés chaque jour",
            activeDays: "Jours avec des adhkar ce mois-ci", finished: "Adhkar terminés ce mois-ci", streak: "Jours d'affilée",
            nothing: "Aucun dhikr terminé ce jour-là.", completed: "Adhkar terminés", today: "Aujourd'hui",
            open: "Mon historique", tab: "Historique", days: { String($0) })
        }
    }
}

/// The Record page: a month calendar of completed azkar, a summary, and the selected day's list.
struct HistoryView: View {
    @Environment(AppModel.self) private var model
    @State private var month = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: .now)) ?? .now
    @State private var selected = Calendar.current.startOfDay(for: .now)
    @State private var days: [String: AzkarHistory.Day] = [:]

    private let cal = Calendar.current

    var body: some View {
        let t = HistoryTexts.of(model.lang)
        ScrollView {
            VStack(spacing: 14) {
                Text(t.subtitle).font(.subheadline).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 4)
                summary(t)
                calendar
                    .padding(14)
                    .glassCard(cornerRadius: 22)
                details(t)
            }
            .padding(.horizontal, 16).padding(.bottom, 40)
            .frame(maxWidth: 760).frame(maxWidth: .infinity)
        }
        .background(Backdrop())
        .navigationTitle(t.title)
        .navigationBarTitleDisplayModeLarge()
        .onAppear { days = AzkarHistory.all() }
        .onChange(of: model.progress) { _, _ in days = AzkarHistory.all() }
    }

    // MARK: Summary

    private func summary(_ t: HistoryTexts) -> some View {
        let inMonth = days.filter { key, _ in key.hasPrefix(monthPrefix(month)) }
        let active = inMonth.values.filter { $0.count > 0 }.count
        let finished = inMonth.values.reduce(0) { $0 + $1.count }
        return HStack(spacing: 10) {
            stat(t.days(active), t.activeDays, "calendar")
            stat(t.days(finished), t.finished, "checkmark.circle")
            stat(t.days(AzkarHistory.currentStreak(days)), t.streak, "flame")
        }
    }

    private func stat(_ value: String, _ label: String, _ symbol: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Image(systemName: symbol).foregroundStyle(Theme.accent)
            Text(value).font(.title2.weight(.semibold)).monospacedDigit()
            Text(label).font(.caption).foregroundStyle(.secondary).lineLimit(3).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, minHeight: 92, alignment: .topLeading)
        .padding(12)
        .glassCard(cornerRadius: 18)
    }

    // MARK: Calendar

    private var calendar: some View {
        let lang = model.lang
        let first = month
        let count = cal.range(of: .day, in: .month, for: first)?.count ?? 30
        let lead = (cal.component(.weekday, from: first) - cal.firstWeekday + 7) % 7
        let cells: [Date?] = Array(repeating: nil, count: lead) + (0..<count).map { cal.date(byAdding: .day, value: $0, to: first) }
        let columns = Array(repeating: GridItem(.flexible(), spacing: 6), count: 7)
        return VStack(spacing: 10) {
            HStack {
                Button { shiftMonth(-1) } label: { Image(systemName: lang.isRTL ? "chevron.right" : "chevron.left") }
                Spacer()
                VStack(spacing: 2) {
                    Text(monthTitle(first)).font(.headline)
                    Text(Hijri.format(lang, Hijri.of(first))).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Button { shiftMonth(1) } label: { Image(systemName: lang.isRTL ? "chevron.left" : "chevron.right") }
                    .disabled(cal.isDate(first, equalTo: .now, toGranularity: .month))
            }
            .buttonStyle(.borderless)
            .font(.headline)

            LazyVGrid(columns: columns, spacing: 6) {
                ForEach(weekdaySymbols(), id: \.self) { d in
                    Text(d).font(.caption2.weight(.semibold)).foregroundStyle(.secondary)
                }
                ForEach(Array(cells.enumerated()), id: \.offset) { _, date in
                    if let date { dayCell(date) } else { Color.clear.frame(height: 46) }
                }
            }
        }
    }

    private func dayCell(_ date: Date) -> some View {
        let day = days[AzkarHistory.dayKey(date)] ?? AzkarHistory.Day()
        let isToday = cal.isDateInToday(date)
        let isSelected = cal.isDate(date, inSameDayAs: selected)
        let future = date > .now
        let shade: Double = day.count == 0 ? 0 : day.count < 3 ? 0.25 : day.count < 6 ? 0.5 : 0.8
        return Button { selected = cal.startOfDay(for: date) } label: {
            VStack(spacing: 2) {
                Text(model.lang.num(cal.component(.day, from: date)))
                    .font(.callout.weight(isToday ? .bold : .regular)).monospacedDigit()
                    .foregroundStyle(shade >= 0.5 ? Color.white : (future ? Color.secondary : Color.primary))
                HStack(spacing: 1) {
                    if day.readings.contains("morning") { Image(systemName: "sun.max.fill").foregroundStyle(.orange) }
                    if day.readings.contains("evening") { Image(systemName: "moon.fill").foregroundStyle(.indigo) }
                }
                .font(.system(size: 8))
                .frame(height: 9)
            }
            .frame(maxWidth: .infinity, minHeight: 46)
            .background(Theme.accent.opacity(shade), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(isSelected ? Theme.gold : (isToday ? Theme.accent : .clear), lineWidth: isSelected ? 2 : 1.5)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(future)
    }

    // MARK: Selected day

    @ViewBuilder
    private func details(_ t: HistoryTexts) -> some View {
        let lang = model.lang
        let day = days[AzkarHistory.dayKey(selected)] ?? AzkarHistory.Day()
        let readings = day.readings.compactMap(Reading.init(key:))
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(cal.isDateInToday(selected) ? t.today : fullDate(selected)).font(.headline)
                Spacer()
                Text(Hijri.format(lang, Hijri.of(selected))).font(.caption).foregroundStyle(.secondary)
            }
            if readings.isEmpty {
                Text(t.nothing).foregroundStyle(.secondary).frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 6)
            } else {
                Text("\(t.completed) (\(lang.num(readings.count)))").font(.footnote.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary)
                VStack(spacing: 0) {
                    ForEach(Array(readings.enumerated()), id: \.element) { i, r in
                        readingRow(r)
                        if i < readings.count - 1 { Divider().padding(.leading, 44) }
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassCard(cornerRadius: 22)
    }

    /// One completed reading: its category's icon, its title, and the category it belongs to.
    private func readingRow(_ r: Reading) -> some View {
        let data = AzkarData.shared
        let s = model.s
        let look: (symbol: String, color: Color, category: String?) = {
            switch r {
            case .morning: return ("sun.max.fill", .orange, nil)
            case .evening: return ("moon.fill", .indigo, nil)
            case .chapter(let id):
                guard let cat = data.categories.first(where: { $0.chapters.contains(id) }) else { return ("sparkles", .teal, nil) }
                return (categorySymbol(cat.key), categoryHue(cat.key), s.category(cat.key))
            }
        }()
        return NavigationLink(value: Route.reader(r, focus: nil)) {
            HStack(spacing: 12) {
                SymbolSquare(symbol: look.symbol, color: look.color, size: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(data.title(r, s, model.lang)).foregroundStyle(.primary).lineLimit(2)
                    if let c = look.category { Text(c).font(.caption).foregroundStyle(.secondary) }
                }
                Spacer(minLength: 8)
                Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.accent)
            }
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: Helpers

    private func shiftMonth(_ n: Int) {
        if let m = cal.date(byAdding: .month, value: n, to: month) { withAnimation(.snappy) { month = m } }
    }

    private func monthPrefix(_ date: Date) -> String {
        let c = cal.dateComponents([.year, .month], from: date)
        return String(format: "%04d-%02d-", c.year ?? 0, c.month ?? 0)
    }

    private var locale: Locale { model.lang == .ar ? Locale(identifier: "ar@numbers=arab") : model.lang.locale }

    private func monthTitle(_ date: Date) -> String {
        let f = DateFormatter(); f.locale = locale; f.setLocalizedDateFormatFromTemplate("MMMM y")
        return f.string(from: date)
    }

    private func fullDate(_ date: Date) -> String {
        let f = DateFormatter(); f.locale = locale; f.setLocalizedDateFormatFromTemplate("EEEE d MMMM y")
        return f.string(from: date)
    }

    /// Short weekday names starting at the calendar's first weekday.
    private func weekdaySymbols() -> [String] {
        let f = DateFormatter(); f.locale = locale
        let s = f.veryShortStandaloneWeekdaySymbols ?? ["S", "M", "T", "W", "T", "F", "S"]
        let start = cal.firstWeekday - 1
        return (0..<7).map { "\(s[(start + $0) % 7])\(String(repeating: "\u{200B}", count: $0))" }
    }
}
