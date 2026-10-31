import SwiftUI
import WidgetKit
import AppIntents

@main
struct AzkarWidgets: WidgetBundle {
    var body: some Widget {
        DhikrWidget()
        QuickAzkarWidget()
        TasbihWidget()
    }
}

// MARK: - Dhikr of the day

struct DhikrEntry: TimelineEntry {
    let date: Date
    let zikr: Zikr
    let lang: Lang
}

struct DhikrProvider: TimelineProvider {
    private func zikr(for date: Date) -> Zikr {
        let pool = AzkarData.shared.shortAzkar
        let hours = Int(date.timeIntervalSince1970 / 3600)
        return pool[hours % pool.count]
    }

    func placeholder(in context: Context) -> DhikrEntry {
        DhikrEntry(date: .now, zikr: AzkarData.shared.morning[13], lang: SharedStore.lang)
    }

    func getSnapshot(in context: Context, completion: @escaping (DhikrEntry) -> Void) {
        completion(DhikrEntry(date: .now, zikr: zikr(for: .now), lang: SharedStore.lang))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DhikrEntry>) -> Void) {
        // A new dhikr every hour for the next 12 hours.
        let start = Calendar.current.dateInterval(of: .hour, for: .now)?.start ?? .now
        let entries = (0..<12).map { h -> DhikrEntry in
            let d = start.addingTimeInterval(Double(h) * 3600)
            return DhikrEntry(date: d, zikr: zikr(for: d), lang: SharedStore.lang)
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

struct DhikrWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DhikrEntry

    var body: some View {
        let s = L10n.of(entry.lang)
        let link = AzkarData.shared.reading(ofZikr: entry.zikr.id).map { DeepLink.url($0, focus: entry.zikr.id) }
        switch family {
        #if os(iOS)
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 2) {
                Label(s.widgetDhikr, systemImage: "sparkles").font(.caption2.weight(.semibold)).widgetAccentable()
                Text(entry.zikr.ar).font(.amiri(14)).lineLimit(3)
            }
            .widgetURL(link)
        case .accessoryInline:
            Text(entry.zikr.ar).widgetURL(link)
        #endif
        default:
            VStack(spacing: 6) {
                HStack {
                    EightPointStar().fill(Brand.mushafGold).frame(width: 12, height: 12)
                    Text(s.widgetDhikr).font(.caption.weight(.semibold)).foregroundStyle(.white.opacity(0.85))
                    Spacer()
                }
                Text(entry.zikr.ar)
                    .font(.amiri(family == .systemSmall ? 17 : 21))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.5)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                let meaning = entry.zikr.meaning(entry.lang)
                if family != .systemSmall, !meaning.isEmpty {
                    Text(meaning).font(.caption).foregroundStyle(.white.opacity(0.75))
                        .lineLimit(family == .systemLarge ? 6 : 2).multilineTextAlignment(.center)
                }
                Text("ISLAMIC AZKAR · أذكار إسلامية").font(.system(size: 9, weight: .semibold)).tracking(0.8).foregroundStyle(Brand.mushafGold)
            }
            .widgetURL(link)
        }
    }
}

struct DhikrWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DhikrWidget", provider: DhikrProvider()) { entry in
            DhikrWidgetView(entry: entry)
                .containerBackground(for: .widget) { Brand.widgetGradient }
        }
        .configurationDisplayName("Dhikr of the day · ذكر اليوم")
        .description("A short dhikr from Hisn Al Muslim, refreshed every hour.")
        .supportedFamilies(Self.families)
    }

    private static var families: [WidgetFamily] {
        #if os(iOS)
        [.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular, .accessoryInline]
        #else
        [.systemSmall, .systemMedium, .systemLarge]
        #endif
    }
}

// MARK: - Quick azkar

struct SimpleEntry: TimelineEntry {
    let date: Date
    let lang: Lang
}

struct SimpleProvider: TimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry { SimpleEntry(date: .now, lang: SharedStore.lang) }
    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> Void) { completion(placeholder(in: context)) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<SimpleEntry>) -> Void) {
        completion(Timeline(entries: [placeholder(in: context)], policy: .after(.now.addingTimeInterval(3600))))
    }
}

struct QuickAzkarView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SimpleEntry

    var body: some View {
        let s = L10n.of(entry.lang)
        if family == .systemSmall {
            let r = Reading.recommended()
            VStack(alignment: .leading, spacing: 6) {
                Image(systemName: symbol(r)).font(.title2).foregroundStyle(Brand.mushafGold)
                Spacer()
                Text(s.recommendedNow).font(.caption2).foregroundStyle(.white.opacity(0.7))
                Text(AzkarData.shared.title(r, s, entry.lang)).font(.headline).foregroundStyle(.white).lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .widgetURL(DeepLink.url(r))
        } else {
            HStack(spacing: 8) {
                tile(.morning, "sun.max.fill", s.morningAzkar, .orange)
                tile(.evening, "moon.stars.fill", s.eveningAzkar, .purple)
                tile(.chapter(28), "bed.double.fill", s.sleepAzkar, .cyan)
            }
        }
    }

    private func symbol(_ r: Reading) -> String {
        switch r {
        case .morning: return "sun.max.fill"
        case .evening: return "sun.horizon.fill"
        case .chapter(28): return "moon.stars.fill"
        default: return "hands.and.sparkles.fill"
        }
    }

    private func tile(_ r: Reading, _ symbol: String, _ title: String, _ hue: Color) -> some View {
        Link(destination: DeepLink.url(r)) {
            VStack(spacing: 6) {
                Image(systemName: symbol).font(.title2).foregroundStyle(hue).symbolRenderingMode(.hierarchical)
                Text(title).font(.caption.weight(.semibold)).foregroundStyle(.white).lineLimit(1).minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(.white.opacity(0.12), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(.white.opacity(0.25), lineWidth: 0.8))
        }
    }
}

struct QuickAzkarWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "QuickAzkarWidget", provider: SimpleProvider()) { entry in
            QuickAzkarView(entry: entry)
                .containerBackground(for: .widget) { Brand.widgetGradient }
        }
        .configurationDisplayName("Quick azkar · أذكار سريعة")
        .description("Jump straight into morning, evening or sleep azkar.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
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
        return TasbihEntry(date: .now, count: d.integer(forKey: SharedStore.Key.tasbihCount),
                           target: SharedStore.tasbihTarget,
                           phrase: min(max(d.integer(forKey: SharedStore.Key.tasbihPhrase), 0), TasbihPhrases.ar.count - 1),
                           lang: SharedStore.lang)
    }
    func placeholder(in context: Context) -> TasbihEntry { current() }
    func getSnapshot(in context: Context, completion: @escaping (TasbihEntry) -> Void) { completion(current()) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<TasbihEntry>) -> Void) {
        completion(Timeline(entries: [current()], policy: .never))
    }
}

struct TasbihWidgetView: View {
    let entry: TasbihEntry

    var body: some View {
        let inRound = entry.target > 0 ? entry.count % entry.target : entry.count
        ZStack(alignment: .topTrailing) {
            Button(intent: IncrementTasbihIntent()) {
                VStack(spacing: 2) {
                    Text(TasbihPhrases.ar[entry.phrase]).font(.amiri(15)).foregroundStyle(.white.opacity(0.9)).lineLimit(1)
                    Text(entry.lang.num(inRound))
                        .font(.system(size: 40, weight: .bold, design: .rounded)).foregroundStyle(.white)
                        .contentTransition(.numericText())
                    Text(entry.target > 0 ? "/ \(entry.lang.num(entry.target))" : "∞").font(.caption).foregroundStyle(Brand.mushafGold)
                    ProgressView(value: Double(inRound), total: Double(max(entry.target, 1)))
                        .tint(Brand.mushafGold).padding(.horizontal, 6).padding(.top, 4)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.plain)

            Button(intent: ResetTasbihIntent()) {
                Image(systemName: "arrow.counterclockwise").font(.caption.bold()).foregroundStyle(.white)
                    .frame(width: 26, height: 26)
                    .background(.white.opacity(0.18), in: Circle())
            }
            .buttonStyle(.plain)
        }
    }
}

struct TasbihWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TasbihWidget", provider: TasbihProvider()) { entry in
            TasbihWidgetView(entry: entry)
                .containerBackground(for: .widget) { Brand.widgetGradient }
        }
        .configurationDisplayName("Tasbih · المسبحة")
        .description("Count your tasbih right on the Home Screen.")
        .supportedFamilies([.systemSmall])
    }
}
