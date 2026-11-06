import SwiftUI

/// Texts for the Record page.
struct HistoryTexts {
    let title, subtitle, activeDays, finished, streak, nothing, readings, azkar, today, open: String
    let days: (Int) -> String

    static func of(_ lang: Lang) -> HistoryTexts {
        switch lang {
        case .ar: return HistoryTexts(
            title: "سجلّ الأذكار", subtitle: "ما أتممته من أذكار كل يوم",
            activeDays: "أيام الذكر هذا الشهر", finished: "أذكار أتممتها هذا الشهر", streak: "أيام متتالية",
            nothing: "لم تُتمّ أذكارًا في هذا اليوم.", readings: "أتممت", azkar: "الأذكار المكتملة", today: "اليوم",
            open: "سجلّي", days: { Lang.ar.num($0) })
        case .en: return HistoryTexts(
            title: "Azkar record", subtitle: "The azkar you finished each day",
            activeDays: "Days with azkar this month", finished: "Azkar finished this month", streak: "Days in a row",
            nothing: "No azkar finished on this day.", readings: "Completed", azkar: "Azkar finished", today: "Today",
            open: "My record", days: { String($0) })
        case .fr: return HistoryTexts(
            title: "Historique des adhkar", subtitle: "Les adhkar terminés chaque jour",
            activeDays: "Jours avec des adhkar ce mois-ci", finished: "Adhkar terminés ce mois-ci", streak: "Jours d'affilée",
            nothing: "Aucun dhikr terminé ce jour-là.", readings: "Terminé", azkar: "Adhkar terminés", today: "Aujourd'hui",
            open: "Mon historique", days: { String($0) })
        }
    }
}

/// Row that opens the Record page (iPhone Home screen).
struct HistoryLinkRow: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        let t = HistoryTexts.of(model.lang)
        NavigationLink(value: Route.history) {
            GlassRow(divider: false) {
                SymbolSquare(symbol: "calendar", color: .teal, size: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(t.open).foregroundStyle(.primary)
                    Text(t.subtitle).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer()
                Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
            }
        }
        .buttonStyle(.plain)
    }
}

/// The Record page: a month calendar of finished azkar, a summary, and the selected day's list.
struct HistoryView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
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
            Text(label).font(.caption).foregroundStyle(.secondary).lineLimit(2).fixedSize(horizontal: false, vertical: true)
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
        let shade: Double = day.count == 0 ? 0 : day.count < 6 ? 0.25 : day.count < 20 ? 0.5 : 0.8
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
        let data = AzkarData.shared
        let day = days[AzkarHistory.dayKey(selected)] ?? AzkarHistory.Day()
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(cal.isDateInToday(selected) ? t.today : fullDate(selected)).font(.headline)
                Spacer()
                Text(Hijri.format(lang, Hijri.of(selected))).font(.caption).foregroundStyle(.secondary)
            }
            if day.count == 0 && day.readings.isEmpty {
                Text(t.nothing).foregroundStyle(.secondary).frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 6)
            } else {
                if !day.readings.isEmpty {
                    Text(t.readings).font(.footnote.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary)
                    ForEach(day.readings, id: \.self) { key in
                        if let r = Reading(key: key) {
                            Button { model.show(r, split: split) } label: {
                                Label(data.title(r, model.s, lang), systemImage: "checkmark.seal.fill")
                                    .frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
                            }
                            .buttonStyle(.plain).foregroundStyle(Theme.accent)
                        }
                    }
                }
                Text("\(t.azkar) · \(lang.num(day.count))").font(.footnote.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary)
                    .padding(.top, 4)
                VStack(spacing: 0) {
                    ForEach(Array(day.zikr.enumerated()), id: \.element) { i, id in
                        if let z = data.zikrById[id], let r = data.reading(ofZikr: id) {
                            Button { model.show(r, focus: id, split: split) } label: {
                                HStack(alignment: .top, spacing: 10) {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.accent).padding(.top, 4)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(z.ar).font(.amiri(17)).lineLimit(2)
                                            .environment(\.layoutDirection, .rightToLeft)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        Text(data.title(r, model.s, lang)).font(.caption).foregroundStyle(.secondary)
                                    }
                                }
                                .padding(.vertical, 8).contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            if i < day.zikr.count - 1 { Divider() }
                        }
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassCard(cornerRadius: 22)
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
