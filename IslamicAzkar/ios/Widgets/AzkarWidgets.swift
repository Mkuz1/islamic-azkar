import SwiftUI
import WidgetKit
import AppIntents

@main
struct AzkarWidgets: WidgetBundle {
    var body: some Widget {
        PrayerWidget()
        HijriWidget()
        ClockWidget()
        DigitalClockWidget()
        ProgressRingsWidget()
        TimelineAzkarWidget()
        MorningAzkarWidget()
        DayAzkarWidget()
        NightAzkarWidget()
        DhikrWidget()
        QuickAzkarWidget()
        ScriptureWidget()
        TasbihWidget()
        CounterWidget()
    }
}

// MARK: - Timelines

struct DateEntry: TimelineEntry {
    let date: Date
    let lang: Lang
}

/// Entries every `step` minutes for the next `count` steps, starting on a step boundary.
struct SteppedProvider: TimelineProvider {
    var step: Int = 15
    var count: Int = 16

    func placeholder(in context: Context) -> DateEntry { DateEntry(date: .now, lang: SharedStore.lang) }
    func getSnapshot(in context: Context, completion: @escaping (DateEntry) -> Void) { completion(placeholder(in: context)) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<DateEntry>) -> Void) {
        let lang = SharedStore.lang
        let s = Double(step * 60)
        let start = Date(timeIntervalSince1970: (Date.now.timeIntervalSince1970 / s).rounded(.down) * s)
        var entries = [DateEntry(date: .now, lang: lang)]
        entries += (1...count).map { DateEntry(date: start.addingTimeInterval(Double($0) * s), lang: lang) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

/// Prayer widgets refresh at every prayer time, and every 15 minutes for the countdown ring.
struct PrayerProvider: TimelineProvider {
    func placeholder(in context: Context) -> DateEntry { DateEntry(date: .now, lang: SharedStore.lang) }
    func getSnapshot(in context: Context, completion: @escaping (DateEntry) -> Void) { completion(placeholder(in: context)) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<DateEntry>) -> Void) {
        let lang = SharedStore.lang
        var dates: [Date] = [.now]
        for i in 1...24 { dates.append(Date.now.addingTimeInterval(Double(i) * 900)) }
        if let place = SharedStore.place {
            dates += (Prayers.times(place) + Prayers.times(place, day: .now.addingTimeInterval(86_400))).filter { $0 > .now }
        }
        let entries = Array(Set(dates)).sorted().prefix(40).map { DateEntry(date: $0, lang: lang) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

private extension View {
    func widgetGlass() -> some View {
        containerBackground(for: .widget) { WidgetGlass() }
    }
}

private var homeFamilies: [WidgetFamily] { [.systemSmall, .systemMedium, .systemLarge] }

private func families(_ home: [WidgetFamily], lock: Bool) -> [WidgetFamily] {
    #if os(iOS)
    lock ? home + [.accessoryCircular, .accessoryRectangular, .accessoryInline] : home
    #else
    home
    #endif
}

// MARK: - Prayer & time

struct PrayerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "PrayerWidget", provider: PrayerProvider()) { e in
            PrayerEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("مواقيت الصلاة · Prayer times")
        .description("The next prayer with a countdown, and today’s times.")
        .supportedFamilies(families(homeFamilies, lock: true))
    }
}

private struct PrayerEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DateEntry
    var body: some View {
        PrayerWidgetView(size: WidgetSize(family), place: SharedStore.place, now: entry.date, lang: entry.lang)
    }
}

struct HijriWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "HijriWidget", provider: SteppedProvider(step: 60, count: 24)) { e in
            HijriEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("التاريخ الهجري · Hijri date")
        .description("The Hijri day and month in classical calligraphy.")
        .supportedFamilies(families([.systemSmall, .systemMedium], lock: true))
    }
}

private struct HijriEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DateEntry
    var body: some View { HijriWidgetView(size: WidgetSize(family), now: entry.date, lang: entry.lang) }
}

struct ClockWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "ClockWidget", provider: SteppedProvider(step: 1, count: 60)) { e in
            IslamicClockFace(date: e.date).padding(-6).containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName("الساعة الإسلامية · Islamic clock")
        .description("An analog clock with an eight-pointed star and Arabic numerals.")
        .supportedFamilies([.systemSmall])
        .contentMarginsDisabled()
    }
}

struct DigitalClockWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DigitalWidget", provider: SteppedProvider(step: 60, count: 24)) { e in
            DigitalEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("الساعة الرقمية · Digital clock")
        .description("The time with the Hijri and Gregorian dates.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

private struct DigitalEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DateEntry
    var body: some View { DigitalClockWidgetView(size: WidgetSize(family), now: entry.date, lang: entry.lang) }
}

struct ProgressRingsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "ProgressWidget", provider: SteppedProvider(step: 15, count: 16)) { e in
            ProgressEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("مضيّ الوقت · Day & year progress")
        .description("Rings for how much of the day, month and years has passed.")
        .supportedFamilies(families([.systemSmall, .systemMedium], lock: false) + lockCircular)
    }

    private var lockCircular: [WidgetFamily] {
        #if os(iOS)
        [.accessoryCircular]
        #else
        []
        #endif
    }
}

private struct ProgressEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DateEntry
    var body: some View { ProgressWidgetView(size: WidgetSize(family), now: entry.date, lang: entry.lang) }
}

// MARK: - Azkar

private struct AzkarListEntryView: View {
    @Environment(\.widgetFamily) private var family
    let set: AzkarSet
    let entry: DateEntry
    var body: some View { AzkarListWidgetView(size: WidgetSize(family), set: set, now: entry.date, lang: entry.lang) }
}

private func azkarListConfiguration(_ kind: String, _ set: AzkarSet, _ name: String, _ description: String) -> some WidgetConfiguration {
    StaticConfiguration(kind: kind, provider: SteppedProvider(step: AzkarSet.slotMinutes, count: 18)) { e in
        AzkarListEntryView(set: set, entry: e).widgetGlass()
    }
    .configurationDisplayName(name)
    .description(description)
    .supportedFamilies(families([.systemSmall, .systemMedium, .systemLarge], lock: false) + rectangular)
}

private var rectangular: [WidgetFamily] {
    #if os(iOS)
    [.accessoryRectangular]
    #else
    []
    #endif
}

struct TimelineAzkarWidget: Widget {
    var body: some WidgetConfiguration {
        azkarListConfiguration("TimelineAzkarWidget", .timeline, "أذكار حسب الوقت · Azkar through the day",
                               "Azkar that change with the time of day: morning, day, evening and night.")
    }
}

struct MorningAzkarWidget: Widget {
    var body: some WidgetConfiguration {
        azkarListConfiguration("MorningAzkarWidget", .morning, "أذكار الصباح · Morning Azkar", "The morning azkar, one after another.")
    }
}

struct DayAzkarWidget: Widget {
    var body: some WidgetConfiguration {
        azkarListConfiguration("DayAzkarWidget", .day, "أذكار اليوم · Day Azkar", "Azkar for the day, one after another.")
    }
}

struct NightAzkarWidget: Widget {
    var body: some WidgetConfiguration {
        azkarListConfiguration("NightAzkarWidget", .night, "أذكار الليل · Night Azkar", "Evening and bedtime azkar.")
    }
}

struct DhikrWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DhikrWidget", provider: SteppedProvider(step: 60, count: 12)) { e in
            DhikrEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("ذكر اليوم · Dhikr of the day")
        .description("A short dhikr from Hisn Al Muslim, refreshed every hour.")
        .supportedFamilies(families(homeFamilies, lock: true))
    }
}

private struct DhikrEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DateEntry
    var body: some View { DhikrWidgetView(size: WidgetSize(family), zikr: DhikrWidgetView.zikr(at: entry.date), lang: entry.lang) }
}

struct QuickAzkarWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "QuickAzkarWidget", provider: SteppedProvider(step: 60, count: 12)) { e in
            QuickEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("أذكار سريعة · Quick azkar")
        .description("Jump straight into morning, evening or sleep azkar.")
        .supportedFamilies(homeFamilies)
    }
}

private struct QuickEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DateEntry
    var body: some View { QuickAzkarWidgetView(size: WidgetSize(family), lang: entry.lang, now: entry.date) }
}

// MARK: - Quran, hadith, poetry and wisdom (configurable)

enum QuoteSourceOption: Int, AppEnum {
    case mixed, quran, hadith, azkar, poetry, wisdom

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Source"
    static var caseDisplayRepresentations: [QuoteSourceOption: DisplayRepresentation] = [
        .mixed: "Mixed · منوّع", .quran: "Quran · القرآن", .hadith: "Hadith · الحديث",
        .azkar: "Azkar · الأذكار", .poetry: "Poetry · الشعر", .wisdom: "Wisdom · الحكم",
    ]
}

struct ScriptureIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Quran, Hadith, Poetry & Wisdom"
    @Parameter(title: "Source", default: .mixed) var source: QuoteSourceOption
}

struct ScriptureEntry: TimelineEntry {
    let date: Date
    let lang: Lang
    let source: QuoteSource
}

struct ScriptureProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> ScriptureEntry { ScriptureEntry(date: .now, lang: SharedStore.lang, source: .quran) }
    func snapshot(for configuration: ScriptureIntent, in context: Context) async -> ScriptureEntry {
        ScriptureEntry(date: .now, lang: SharedStore.lang, source: QuoteSource(rawValue: configuration.source.rawValue) ?? .mixed)
    }
    func timeline(for configuration: ScriptureIntent, in context: Context) async -> Timeline<ScriptureEntry> {
        let source = QuoteSource(rawValue: configuration.source.rawValue) ?? .mixed
        let start = Date(timeIntervalSince1970: (Date.now.timeIntervalSince1970 / 7200).rounded(.down) * 7200)
        let entries = (0..<12).map { ScriptureEntry(date: $0 == 0 ? .now : start.addingTimeInterval(Double($0) * 7200), lang: SharedStore.lang, source: source) }
        return Timeline(entries: entries, policy: .atEnd)
    }
}

struct ScriptureWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "ScriptureWidget", intent: ScriptureIntent.self, provider: ScriptureProvider()) { e in
            ScriptureEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("قرآن وحديث وشعر · Quran & wisdom")
        .description("A verse, hadith, line of poetry or proverb. Touch and hold the widget to choose the source.")
        .supportedFamilies(families(homeFamilies, lock: false) + rectangular)
    }
}

private struct ScriptureEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: ScriptureEntry
    var body: some View { ScriptureWidgetView(size: WidgetSize(family), source: entry.source, now: entry.date, lang: entry.lang) }
}

// MARK: - Tasbih (interactive)

struct TasbihEntry: TimelineEntry {
    let date: Date
    let count: Int
    let target: Int
    let phrase: Int
    let lang: Lang
}

struct TasbihProvider: TimelineProvider {
    private func current() -> TasbihEntry {
        let d = SharedStore.defaults
        return TasbihEntry(date: .now, count: d.integer(forKey: SharedStore.Key.tasbihCount), target: SharedStore.tasbihTarget,
                           phrase: min(max(d.integer(forKey: SharedStore.Key.tasbihPhrase), 0), TasbihPhrases.ar.count - 1),
                           lang: SharedStore.lang)
    }
    func placeholder(in context: Context) -> TasbihEntry { current() }
    func getSnapshot(in context: Context, completion: @escaping (TasbihEntry) -> Void) { completion(current()) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<TasbihEntry>) -> Void) {
        // Refresh after midnight so the weekly chart moves on to a new day.
        let midnight = Calendar.current.startOfDay(for: .now.addingTimeInterval(86_400))
        completion(Timeline(entries: [current()], policy: .after(midnight)))
    }
}

private struct TasbihEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: TasbihEntry
    var body: some View {
        TasbihWidgetView(size: WidgetSize(family), count: entry.count, target: entry.target, phrase: entry.phrase, lang: entry.lang)
    }
}

struct TasbihWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TasbihWidget", provider: TasbihProvider()) { e in
            TasbihEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("المسبحة · Tasbih")
        .description("Count your tasbih right on the Home Screen.")
        .supportedFamilies(families([.systemSmall], lock: false) + tasbihLock)
    }

    private var tasbihLock: [WidgetFamily] {
        #if os(iOS)
        [.accessoryCircular, .accessoryRectangular]
        #else
        []
        #endif
    }
}

struct CounterWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "CounterWidget", provider: TasbihProvider()) { e in
            TasbihEntryView(entry: e).widgetGlass()
        }
        .configurationDisplayName("العدّاد والإحصاء · Counter & weekly stats")
        .description("The counter with your tasbih over the week.")
        .supportedFamilies([.systemMedium])
    }
}
