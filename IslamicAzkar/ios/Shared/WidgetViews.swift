import SwiftUI
import WidgetKit
import AppIntents

// Widget faces, shared by the widget extension and the in-app widget gallery so the
// previews are the real thing. Each view takes its size explicitly (WidgetSize) because
// the widgetFamily environment value only exists inside the extension.

enum WidgetSize: String, CaseIterable {
    case small, medium, large, circular, rectangular, inline

    var isAccessory: Bool { self == .circular || self == .rectangular || self == .inline }

    /// Point size of the Home Screen widget on a 6.1″ iPhone (used by the gallery).
    var previewSize: CGSize {
        switch self {
        case .small: CGSize(width: 158, height: 158)
        case .medium: CGSize(width: 338, height: 158)
        case .large: CGSize(width: 338, height: 354)
        case .circular: CGSize(width: 72, height: 72)
        case .rectangular: CGSize(width: 172, height: 76)
        case .inline: CGSize(width: 240, height: 26)
        }
    }

    #if os(iOS)
    init(_ family: WidgetFamily) {
        switch family {
        case .systemSmall: self = .small
        case .systemMedium: self = .medium
        case .systemLarge, .systemExtraLarge: self = .large
        case .accessoryCircular: self = .circular
        case .accessoryRectangular: self = .rectangular
        case .accessoryInline: self = .inline
        default: self = .small
        }
    }
    #else
    init(_ family: WidgetFamily) {
        switch family {
        case .systemMedium: self = .medium
        case .systemLarge, .systemExtraLarge: self = .large
        default: self = .small
        }
    }
    #endif
}

/// The glass behind every Home Screen widget: the chosen palette, a soft sheen and a rim.
struct WidgetGlass: View {
    var palette: Palette = SharedStore.palette

    var body: some View {
        ZStack {
            palette.gradient
            LinearGradient(colors: [.white.opacity(0.16), .clear, .black.opacity(0.12)], startPoint: .top, endPoint: .bottom)
            RadialGradient(colors: [Brand.mushafGold.opacity(0.10), .clear], center: .bottomLeading, startRadius: 0, endRadius: 260)
        }
    }
}

/// A gallery tile: the widget face on its glass, clipped like the Home Screen does.
struct WidgetPreviewFrame<Content: View>: View {
    let size: WidgetSize
    var palette: Palette = SharedStore.palette
    @ViewBuilder let content: Content

    var body: some View {
        let s = size.previewSize
        if size.isAccessory {
            content
                .foregroundStyle(.white)
                .padding(size == .inline ? 4 : 8)
                .frame(width: s.width, height: s.height)
                .background(.white.opacity(0.14), in: RoundedRectangle(cornerRadius: size == .circular ? s.width / 2 : 16, style: .continuous))
        } else {
            content
                .padding(16)
                .frame(width: s.width, height: s.height)
                .background(WidgetGlass(palette: palette))
                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(.white.opacity(0.28), lineWidth: 0.8))
                .shadow(color: .black.opacity(0.18), radius: 10, y: 4)
        }
    }
}

private let gold = Brand.mushafGold
private let footer = "أذكار إسلامية"

// MARK: - Prayer times

struct PrayerWidgetView: View {
    let size: WidgetSize
    let place: Place?
    let now: Date
    let lang: Lang
    var script: Script = SharedStore.script

    var body: some View {
        let m = More.of(lang)
        if let place {
            content(place, m)
        } else {
            VStack(spacing: 6) {
                if !size.isAccessory { Text(m.prayerTimes).font(.caption.weight(.semibold)).foregroundStyle(gold) }
                Text(m.setLocationWidget).font(size.isAccessory ? .caption2 : .footnote).multilineTextAlignment(.center)
                    .foregroundStyle(.white.opacity(0.9)).minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    @ViewBuilder
    private func content(_ place: Place, _ m: More) -> some View {
        let times = Prayers.times(place, day: now)
        let next = Prayers.next(place, now: now)
        let prev = Prayers.previous(place, now: now)
        let name = m.prayers[next.index]
        switch size {
        case .inline:
            Text("\(name) \(Prayers.clock(next.time, place, lang))")
        case .circular:
            let span = next.time.timeIntervalSince(prev.time)
            let p = span > 0 ? min(max(now.timeIntervalSince(prev.time) / span, 0), 1) : 0
            Gauge(value: p) { Text(name) } currentValueLabel: {
                Text(Prayers.clock(next.time, place, lang)).font(.system(size: 11, weight: .semibold))
            }
            .gaugeStyle(.accessoryCircularCapacity)
        case .rectangular:
            VStack(alignment: .leading, spacing: 1) {
                Text(m.nextPrayer).font(.caption2).opacity(0.75)
                HStack(alignment: .firstTextBaseline) {
                    Text(name).font(.headline)
                    Text(Prayers.clock(next.time, place, lang)).font(.headline.monospacedDigit())
                }
                Text(next.time, style: .relative).font(.caption2).opacity(0.75)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        case .small:
            VStack(spacing: 2) {
                Text(m.nextPrayer).font(.caption2.weight(.semibold)).foregroundStyle(.white.opacity(0.7))
                Text(name).font(lang == .ar ? script.font(34) : .system(size: 26, weight: .semibold))
                    .foregroundStyle(gold).lineLimit(1).minimumScaleFactor(0.6)
                Text(Prayers.clock(next.time, place, lang)).font(.system(size: 34, weight: .light, design: .rounded)).foregroundStyle(.white)
                Text(next.time, style: .timer).font(.caption.monospacedDigit()).foregroundStyle(.white.opacity(0.75))
                    .multilineTextAlignment(.center)
                Text(place.label(lang)).font(.caption2).foregroundStyle(.white.opacity(0.55)).lineLimit(1)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .medium:
            VStack(spacing: 10) {
                HStack {
                    Text(m.prayerTimes).font(.caption.weight(.semibold)).foregroundStyle(gold)
                    Spacer()
                    Text(place.label(lang)).font(.caption).foregroundStyle(.white.opacity(0.6)).lineLimit(1)
                }
                HStack(spacing: 4) {
                    ForEach([0, 2, 3, 4, 5], id: \.self) { i in
                        let on = i == next.index
                        VStack(spacing: 4) {
                            Text(m.prayers[i]).font(lang == .ar ? script.font(17) : .caption.weight(.semibold))
                                .foregroundStyle(on ? gold : .white.opacity(0.75)).lineLimit(1).minimumScaleFactor(0.6)
                            Text(Prayers.clock(times[i], place, lang)).font(.system(size: 15, weight: on ? .bold : .regular).monospacedDigit())
                                .foregroundStyle(.white)
                        }
                        .frame(maxWidth: .infinity).padding(.vertical, 8)
                        .background(on ? .white.opacity(0.14) : .clear, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
                HStack(spacing: 4) {
                    Text(m.nextPrayer + " ·").foregroundStyle(.white.opacity(0.6))
                    Text(next.time, style: .relative).foregroundStyle(.white.opacity(0.85))
                }
                .font(.caption)
            }
        case .large:
            VStack(spacing: 8) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(m.prayerTimes).font(.headline).foregroundStyle(gold)
                        Text(Hijri.format(lang, Hijri.of(now))).font(.caption).foregroundStyle(.white.opacity(0.7))
                    }
                    Spacer()
                    Label(place.label(lang), systemImage: "location.fill").font(.caption).foregroundStyle(.white.opacity(0.7)).lineLimit(1)
                }
                Spacer(minLength: 2)
                ForEach(0..<6, id: \.self) { i in
                    let on = i == next.index
                    HStack {
                        Text(m.prayers[i]).font(lang == .ar ? script.font(22) : .body.weight(on ? .semibold : .regular))
                            .foregroundStyle(on ? gold : .white.opacity(i == Prayers.sunrise ? 0.55 : 0.9))
                        Spacer()
                        if on { Text(next.time, style: .relative).font(.caption).foregroundStyle(.white.opacity(0.7)) }
                        Text(Prayers.clock(times[i], place, lang)).font(.system(size: 17, weight: on ? .bold : .regular).monospacedDigit())
                            .foregroundStyle(.white)
                    }
                    .padding(.horizontal, 12).padding(.vertical, 7)
                    .background(on ? .white.opacity(0.14) : .clear, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                Spacer(minLength: 2)
            }
        }
    }
}

// MARK: - Hijri date

struct HijriWidgetView: View {
    let size: WidgetSize
    let now: Date
    let lang: Lang
    var script: Script = SharedStore.script
    var offset: Int = SharedStore.hijriOffset

    var body: some View {
        let h = Hijri.of(now, offset: offset)
        let dow = Hijri.weekdayIndex(now)
        switch size {
        case .inline:
            Text(Hijri.format(lang, h))
        case .circular:
            VStack(spacing: 0) {
                Text(Lang.ar.num(h.day)).font(.system(size: 22, weight: .bold))
                Text(Hijri.monthsAr[h.month]).font(.system(size: 9, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
            }
        case .rectangular:
            VStack(alignment: .leading, spacing: 0) {
                Text(Hijri.weekdaysAr[dow]).font(script.font(16))
                Text(Hijri.format(lang, h)).font(.headline).lineLimit(1).minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        case .small:
            VStack(spacing: 0) {
                Text(Hijri.weekdaysAr[dow]).font(script.font(24)).foregroundStyle(gold)
                Text(Lang.ar.num(h.day)).font(.system(size: 58, weight: .bold)).foregroundStyle(.white).minimumScaleFactor(0.6)
                Text(Hijri.monthsAr[h.month]).font(script.font(26)).foregroundStyle(gold).lineLimit(1).minimumScaleFactor(0.6)
                if lang != .ar {
                    Text("\(Hijri.month(lang, h.month)) \(h.year)").font(.caption2).foregroundStyle(.white.opacity(0.75)).lineLimit(1)
                } else {
                    Text("\(Lang.ar.num(h.year)) هـ").font(.caption).foregroundStyle(.white.opacity(0.75))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .medium, .large:
            HStack(spacing: 18) {
                VStack(spacing: 0) {
                    Text(Lang.ar.num(h.day)).font(.system(size: 64, weight: .bold)).foregroundStyle(.white)
                    Text(Hijri.monthsAr[h.month]).font(script.font(26)).foregroundStyle(gold).lineLimit(1).minimumScaleFactor(0.6)
                }
                .frame(width: 120)
                Rectangle().fill(.white.opacity(0.18)).frame(width: 1).padding(.vertical, 10)
                VStack(alignment: .leading, spacing: 6) {
                    Text(Hijri.weekdaysAr[dow]).font(script.font(28)).foregroundStyle(gold)
                    Text(Hijri.format(lang, h)).font(.headline).foregroundStyle(.white).lineLimit(1).minimumScaleFactor(0.7)
                    Text(now.formatted(.dateTime.weekday(.wide).day().month(.wide).year().locale(lang.locale)))
                        .font(.caption).foregroundStyle(.white.opacity(0.7)).lineLimit(1).minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// MARK: - Clocks

/// Analog clock: eight-pointed star, Arabic-Indic numerals and gold ticks.
struct IslamicClockFace: View {
    let date: Date
    var palette: Palette = SharedStore.palette
    var showSeconds = false

    var body: some View {
        GeometryReader { geo in
            let r = min(geo.size.width, geo.size.height) / 2
            let c = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
            let comps = Calendar.current.dateComponents([.hour, .minute, .second], from: date)
            let minutes = Double(comps.minute ?? 0) + Double(comps.second ?? 0) / 60
            let hours = Double((comps.hour ?? 0) % 12) + minutes / 60
            ZStack {
                Circle().fill(LinearGradient(colors: [palette.top, palette.bottom], startPoint: .top, endPoint: .bottom))
                Circle().strokeBorder(.white.opacity(0.25), lineWidth: r * 0.03)
                EightPointStar(inner: 0.62).stroke(gold.opacity(0.75), lineWidth: 1.2).frame(width: r * 0.95, height: r * 0.95)
                EightPointStar(inner: 0.62).fill(gold.opacity(0.12)).frame(width: r * 0.95, height: r * 0.95)
                EightPointStar(inner: 0.62).stroke(gold.opacity(0.35), lineWidth: 0.8).frame(width: r * 0.62, height: r * 0.62)
                Circle().stroke(gold.opacity(0.35), lineWidth: 0.8).frame(width: r * 0.36, height: r * 0.36)
                ForEach(0..<60, id: \.self) { i in
                    let major = i % 5 == 0
                    Capsule().fill(major ? gold : .white.opacity(0.5))
                        .frame(width: major ? r * 0.03 : r * 0.012, height: major ? r * 0.09 : r * 0.045)
                        .offset(y: -r * 0.86)
                        .rotationEffect(.degrees(Double(i) * 6))
                }
                ForEach(1...12, id: \.self) { n in
                    let a = Double(n) * .pi / 6
                    Text(Lang.ar.num(n))
                        .font(.system(size: r * 0.17, weight: .bold))
                        .foregroundStyle(n % 3 == 0 ? gold : .white)
                        .position(x: c.x + sin(a) * r * 0.66, y: c.y - cos(a) * r * 0.66)
                }
                Hand(length: r * 0.48, width: r * 0.075).rotationEffect(.degrees(hours * 30)).foregroundStyle(.white)
                Hand(length: r * 0.72, width: r * 0.05).rotationEffect(.degrees(minutes * 6)).foregroundStyle(.white)
                if showSeconds {
                    Hand(length: r * 0.8, width: r * 0.015).rotationEffect(.degrees(Double(comps.second ?? 0) * 6)).foregroundStyle(gold)
                }
                Circle().fill(gold).frame(width: r * 0.1, height: r * 0.1)
            }
            .frame(width: r * 2, height: r * 2)
            .position(c)
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    private struct Hand: View {
        let length: CGFloat
        let width: CGFloat
        var body: some View {
            Capsule().frame(width: width, height: length)
                .offset(y: -length / 2 + width / 2)
        }
    }
}

struct DigitalClockWidgetView: View {
    let size: WidgetSize
    let now: Date
    let lang: Lang
    var script: Script = SharedStore.script
    var offset: Int = SharedStore.hijriOffset

    var body: some View {
        let h = Hijri.of(now, offset: offset)
        let dow = Hijri.weekdayIndex(now)
        switch size {
        case .small:
            VStack(spacing: 2) {
                Text(Hijri.weekdaysAr[dow]).font(script.font(22)).foregroundStyle(gold)
                Text(now, style: .time).font(.system(size: 40, weight: .thin, design: .rounded)).foregroundStyle(.white)
                    .minimumScaleFactor(0.5).lineLimit(1)
                Text(Hijri.format(lang, h)).font(.caption2).foregroundStyle(.white.opacity(0.8)).lineLimit(1).minimumScaleFactor(0.7)
                Text(now.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).locale(lang.locale)))
                    .font(.caption2).foregroundStyle(.white.opacity(0.55))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            VStack(spacing: 6) {
                Text(now, style: .time).font(.system(size: 66, weight: .thin, design: .rounded)).foregroundStyle(.white)
                    .minimumScaleFactor(0.5).lineLimit(1)
                HStack(spacing: 10) {
                    Text(Hijri.weekdaysAr[dow]).font(script.font(22)).foregroundStyle(gold)
                    Text(Hijri.format(lang, h)).font(.subheadline).foregroundStyle(.white).lineLimit(1).minimumScaleFactor(0.7)
                    Text("· " + now.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).locale(lang.locale)))
                        .font(.caption).foregroundStyle(.white.opacity(0.6)).lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

// MARK: - Progress rings

struct ProgressWidgetView: View {
    let size: WidgetSize
    let now: Date
    let lang: Lang

    private func frac(_ c: Calendar.Component) -> Double {
        guard let i = Calendar.current.dateInterval(of: c, for: now) else { return 0 }
        return now.timeIntervalSince(i.start) / i.duration
    }

    var body: some View {
        let m = More.of(lang)
        let rings: [(String, Double)] = [(m.day, frac(.day)), (m.month, frac(.month)), (m.year, frac(.year)), (m.hijriYear, Hijri.yearProgress(now))]
        switch size {
        case .circular:
            Gauge(value: rings[0].1) { Text(m.day) } currentValueLabel: { Text(percent(rings[0].1)).font(.system(size: 12, weight: .semibold)) }
                .gaugeStyle(.accessoryCircularCapacity)
        case .small:
            Grid(horizontalSpacing: 10, verticalSpacing: 8) {
                GridRow { ring(rings[0], 50); ring(rings[1], 50) }
                GridRow { ring(rings[2], 50); ring(rings[3], 50) }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        default:
            HStack(spacing: 8) { ForEach(rings.indices, id: \.self) { ring(rings[$0], 62) } }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func percent(_ v: Double) -> String { "\(lang.num(Int((v * 100).rounded(.down))))%" }

    private func ring(_ r: (String, Double), _ d: CGFloat) -> some View {
        VStack(spacing: 4) {
            ZStack {
                Circle().stroke(.white.opacity(0.18), lineWidth: d * 0.1)
                Circle().trim(from: 0, to: r.1).stroke(gold, style: StrokeStyle(lineWidth: d * 0.1, lineCap: .round)).rotationEffect(.degrees(-90))
                Text(percent(r.1)).font(.system(size: d * 0.24, weight: .medium)).foregroundStyle(.white)
            }
            .frame(width: d, height: d)
            Text(r.0).font(.system(size: d * 0.19)).foregroundStyle(gold).lineLimit(1).minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Azkar

/// Morning / day / night azkar, and the timeline that follows the time of day.
enum AzkarSet: String, CaseIterable {
    case morning, day, night, timeline

    enum Period { case morning, day, evening, night }

    static let dayChapters = [25, 10, 11, 69, 70, 98, 85, 87, 106, 34, 35, 43, 82, 129]
    static let sleepChapters = [28, 29, 30]
    static let slotMinutes = 20

    static func period(_ date: Date) -> Period {
        switch Calendar.current.component(.hour, from: date) {
        case 4...10: .morning
        case 11...14: .day
        case 15...19: .evening
        default: .night
        }
    }

    private static func chapters(_ ids: [Int], maxLen: Int) -> [Zikr] {
        ids.flatMap { AzkarData.shared.chapterById[$0]?.items ?? [] }.filter { $0.ar.count <= maxLen }
    }

    func items(at date: Date) -> [Zikr] {
        let data = AzkarData.shared
        let sleep = Self.chapters(Self.sleepChapters, maxLen: 320)
        switch self {
        case .morning: return data.morning
        case .day: return Self.chapters(Self.dayChapters, maxLen: 320)
        case .night: return data.evening + sleep
        case .timeline:
            switch Self.period(date) {
            case .morning: return data.morning
            case .day: return Self.chapters(Self.dayChapters, maxLen: 320)
            case .evening: return data.evening
            case .night: return sleep
            }
        }
    }

    func title(at date: Date, _ lang: Lang) -> (String, String, Color) {
        let s = L10n.of(lang), m = More.of(lang)
        let p: Period = switch self { case .morning: .morning; case .day: .day; case .night: .night; case .timeline: Self.period(date) }
        switch p {
        case .morning: return (s.morningAzkar, "sun.max.fill", .orange)
        case .day: return (m.dayAzkar, "sun.haze.fill", .yellow)
        case .evening: return (s.eveningAzkar, "sun.horizon.fill", .orange)
        case .night: return (self == .night ? m.nightAzkar : s.sleepAzkar, "moon.stars.fill", Color(hex: 0xBFA6FF))
        }
    }

    /// The item shown at `date`: one every 20 minutes from the start of the period, shifted by the user's taps.
    func index(at date: Date, count: Int) -> Int {
        guard count > 0 else { return 0 }
        let c = Calendar.current.dateComponents([.hour, .minute], from: date)
        let minute = (c.hour ?? 0) * 60 + (c.minute ?? 0)
        let start: Int = switch (self == .timeline ? Self.period(date) : .morning) { case .morning: 240; case .day: 660; case .evening: 900; case .night: 1200 }
        let elapsed = ((minute - start) % 1440 + 1440) % 1440
        let shift = SharedStore.defaults.integer(forKey: SharedStore.Key.timelineIndex + "_" + rawValue)
        return ((elapsed / Self.slotMinutes + shift) % count + count) % count
    }
}

/// Steps an azkar widget forward or back (interactive widgets, iOS 17+).
struct StepAzkarIntent: AppIntent {
    static var title: LocalizedStringResource = "Next dhikr"
    static var isDiscoverable: Bool = false

    @Parameter(title: "Set") var set: String
    @Parameter(title: "Step") var step: Int

    init() {}
    init(set: AzkarSet, step: Int) { self.set = set.rawValue; self.step = step }

    func perform() async throws -> some IntentResult {
        let k = SharedStore.Key.timelineIndex + "_" + set
        SharedStore.defaults.set(SharedStore.defaults.integer(forKey: k) + step, forKey: k)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct AzkarListWidgetView: View {
    let size: WidgetSize
    let set: AzkarSet
    let now: Date
    let lang: Lang

    var body: some View {
        let items = set.items(at: now)
        let i = set.index(at: now, count: items.count)
        let z = items[safe: i] ?? AzkarData.shared.morning[0]
        let (title, symbol, hue) = set.title(at: now, lang)
        let link = AzkarData.shared.reading(ofZikr: z.id).map { DeepLink.url($0, focus: z.id) }
        let progress = lang == .ar ? "\(lang.num(i + 1)) من \(lang.num(items.count))" : lang == .fr ? "\(i + 1) sur \(items.count)" : "\(i + 1) of \(items.count)"

        switch size {
        case .rectangular, .inline, .circular:
            VStack(alignment: .leading, spacing: 1) {
                Label(title, systemImage: symbol).font(.caption2.weight(.semibold))
                Text(z.ar).font(.amiri(13)).lineLimit(3)
            }
            .widgetURL(link)
        case .small:
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Image(systemName: symbol).foregroundStyle(hue).font(.subheadline)
                    Spacer()
                    Text(progress).font(.caption2.weight(.semibold)).foregroundStyle(gold)
                }
                Text(z.ar).font(.amiri(16)).foregroundStyle(.white).multilineTextAlignment(.center)
                    .minimumScaleFactor(0.45).frame(maxWidth: .infinity, maxHeight: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
            }
            .widgetURL(link)
        case .medium, .large:
            VStack(spacing: 6) {
                HStack(spacing: 8) {
                    Image(systemName: symbol).foregroundStyle(hue)
                    Text(title).font(.subheadline.weight(.semibold)).foregroundStyle(.white).lineLimit(1)
                    Spacer()
                    Text(progress).font(.caption.weight(.semibold)).foregroundStyle(gold)
                        .padding(.horizontal, 9).padding(.vertical, 3).background(.white.opacity(0.12), in: Capsule())
                }
                Text(z.ar).font(.amiri(size == .large ? 20 : 16)).foregroundStyle(.white).multilineTextAlignment(.center)
                    .lineSpacing(2).minimumScaleFactor(0.5).frame(maxWidth: .infinity, maxHeight: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                let meaning = z.meaning(lang)
                if !meaning.isEmpty {
                    Text(meaning).font(.caption).foregroundStyle(.white.opacity(0.72)).multilineTextAlignment(.center)
                        .lineLimit(size == .large ? 6 : 2)
                }
                HStack {
                    Button(intent: StepAzkarIntent(set: set, step: lang.isRTL ? 1 : -1)) { arrow("chevron.left") }.buttonStyle(.plain)
                    Spacer()
                    Text(footer).font(.caption2).foregroundStyle(gold)
                    Spacer()
                    Button(intent: StepAzkarIntent(set: set, step: lang.isRTL ? -1 : 1)) { arrow("chevron.right") }.buttonStyle(.plain)
                }
                .environment(\.layoutDirection, .leftToRight)
            }
            .widgetURL(link)
        }
    }

    private func arrow(_ symbol: String) -> some View {
        Image(systemName: symbol).font(.footnote.weight(.bold)).foregroundStyle(.white)
            .frame(width: 30, height: 30)
            .background(.white.opacity(0.14), in: Circle())
            .overlay(Circle().strokeBorder(.white.opacity(0.25), lineWidth: 0.6))
    }
}

/// A short dhikr, refreshed every hour.
struct DhikrWidgetView: View {
    let size: WidgetSize
    let zikr: Zikr
    let lang: Lang

    var body: some View {
        let s = L10n.of(lang)
        let link = AzkarData.shared.reading(ofZikr: zikr.id).map { DeepLink.url($0, focus: zikr.id) }
        switch size {
        case .rectangular, .circular:
            VStack(alignment: .leading, spacing: 2) {
                Label(s.widgetDhikr, systemImage: "sparkles").font(.caption2.weight(.semibold)).widgetAccentable()
                Text(zikr.ar).font(.amiri(14)).lineLimit(3)
            }
            .widgetURL(link)
        case .inline:
            Text(zikr.ar).widgetURL(link)
        default:
            VStack(spacing: 6) {
                HStack {
                    EightPointStar().fill(gold).frame(width: 12, height: 12)
                    Text(s.widgetDhikr).font(.caption.weight(.semibold)).foregroundStyle(.white.opacity(0.85))
                    Spacer()
                }
                Text(zikr.ar)
                    .font(.amiri(size == .small ? 17 : 21)).foregroundStyle(.white)
                    .multilineTextAlignment(.center).minimumScaleFactor(0.5)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                let meaning = zikr.meaning(lang)
                if size != .small, !meaning.isEmpty {
                    Text(meaning).font(.caption).foregroundStyle(.white.opacity(0.75))
                        .lineLimit(size == .large ? 6 : 2).multilineTextAlignment(.center)
                }
                Text("ISLAMIC AZKAR · أذكار إسلامية").font(.system(size: 9, weight: .semibold)).tracking(0.8).foregroundStyle(gold)
            }
            .widgetURL(link)
        }
    }

    static func zikr(at date: Date) -> Zikr {
        let pool = AzkarData.shared.shortAzkar
        return pool[Int(date.timeIntervalSince1970 / 3600) % pool.count]
    }
}

/// Shortcuts into the most-read azkar.
struct QuickAzkarWidgetView: View {
    let size: WidgetSize
    let lang: Lang
    let now: Date

    var body: some View {
        let s = L10n.of(lang)
        if size == .small {
            let r = Reading.recommended(at: now)
            VStack(alignment: .leading, spacing: 6) {
                Image(systemName: symbol(r)).font(.title2).foregroundStyle(gold)
                Spacer()
                Text(s.recommendedNow).font(.caption2).foregroundStyle(.white.opacity(0.7))
                Text(AzkarData.shared.title(r, s, lang)).font(.headline).foregroundStyle(.white).lineLimit(2)
                Text(s.azkar(AzkarData.shared.items(r).count)).font(.caption2).foregroundStyle(gold)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .widgetURL(DeepLink.url(r))
        } else {
            let tiles: [(Reading, String, String, Color)] = [
                (.morning, "sun.max.fill", s.morningAzkar, .orange), (.evening, "moon.stars.fill", s.eveningAzkar, Color(hex: 0xBFA6FF)),
                (.chapter(28), "bed.double.fill", s.sleepAzkar, .cyan), (.chapter(25), "hands.and.sparkles.fill", s.afterPrayer, .green),
            ]
            let shown = size == .large ? tiles : Array(tiles.prefix(3))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: size == .large ? 2 : 3), spacing: 8) {
                ForEach(shown, id: \.0) { r, symbol, title, hue in
                    Link(destination: DeepLink.url(r)) {
                        VStack(spacing: 6) {
                            Image(systemName: symbol).font(.title2).foregroundStyle(hue).symbolRenderingMode(.hierarchical)
                            Text(title).font(.caption.weight(.semibold)).foregroundStyle(.white).lineLimit(1).minimumScaleFactor(0.7)
                            if size == .large { Text(s.azkar(AzkarData.shared.items(r).count)).font(.caption2).foregroundStyle(gold) }
                        }
                        .frame(maxWidth: .infinity).frame(height: size == .large ? 140 : 118)
                        .background(.white.opacity(0.12), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(.white.opacity(0.25), lineWidth: 0.8))
                    }
                }
            }
        }
    }

    private func symbol(_ r: Reading) -> String {
        switch r {
        case .morning: "sun.max.fill"
        case .evening: "sun.horizon.fill"
        case .chapter(28): "moon.stars.fill"
        default: "hands.and.sparkles.fill"
        }
    }
}

// MARK: - Quran, hadith, poetry and wisdom

struct ScriptureWidgetView: View {
    let size: WidgetSize
    let source: QuoteSource
    let now: Date
    let lang: Lang
    var script: Script = SharedStore.script

    var body: some View {
        let q = Library.shared.quote(source, at: now)
        let m = More.of(lang)
        let isPoetry = Library.shared.poetry.contains(q)
        let arFont: Font = q.ar.count < 70 || isPoetry ? script.font(size == .small ? 18 : 22) : .amiri(size == .small ? 15 : 18)
        switch size {
        case .rectangular, .circular, .inline:
            VStack(alignment: .leading, spacing: 1) {
                Text(q.ar).font(.amiri(13)).lineLimit(3)
                Text(q.source(lang)).font(.caption2).opacity(0.7)
            }
        default:
            VStack(spacing: 8) {
                HStack {
                    Text(m.sources[source.rawValue]).font(.caption.weight(.semibold)).foregroundStyle(gold)
                    Spacer()
                    EightPointStar().fill(gold.opacity(0.8)).frame(width: 10, height: 10)
                }
                Text(q.ar).font(arFont).foregroundStyle(.white).multilineTextAlignment(.center)
                    .lineSpacing(4).minimumScaleFactor(0.45).frame(maxWidth: .infinity, maxHeight: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                let meaning = q.meaning(lang)
                if size != .small, !meaning.isEmpty {
                    Text(meaning).font(.caption).foregroundStyle(.white.opacity(0.72)).multilineTextAlignment(.center)
                        .lineLimit(size == .large ? 5 : 2)
                }
                Text("— " + q.source(lang)).font(.caption2).foregroundStyle(gold.opacity(0.9)).lineLimit(1)
            }
        }
    }
}

// MARK: - Tasbih

struct TasbihWidgetView: View {
    let size: WidgetSize
    let count: Int
    let target: Int
    let phrase: Int
    let lang: Lang

    var body: some View {
        let inRound = target > 0 ? count % target : count
        switch size {
        case .circular:
            Gauge(value: Double(inRound), in: 0...Double(max(target, 1))) { Text("") } currentValueLabel: {
                Text(lang.num(inRound)).font(.system(size: 16, weight: .bold))
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .widgetURL(DeepLink.tasbih)
        case .rectangular, .inline:
            VStack(alignment: .leading, spacing: 1) {
                Text(TasbihPhrases.ar[phrase]).font(.amiri(14)).lineLimit(1)
                Text("\(lang.num(inRound)) / \(target > 0 ? lang.num(target) : "∞")").font(.headline.monospacedDigit())
            }
            .widgetURL(DeepLink.tasbih)
        case .small:
            counter(inRound, compact: true)
        case .medium, .large:
            HStack(spacing: 14) {
                counter(inRound, compact: false).frame(width: 128)
                Rectangle().fill(.white.opacity(0.18)).frame(width: 1).padding(.vertical, 6)
                WeeklyBars(lang: lang)
            }
        }
    }

    private func counter(_ inRound: Int, compact: Bool) -> some View {
        let m = More.of(lang)
        return VStack(spacing: compact ? 2 : 4) {
            if !compact { Text(m.counter).font(.caption.weight(.semibold)).foregroundStyle(gold) }
            Text(TasbihPhrases.ar[phrase]).font(.amiri(15)).foregroundStyle(.white.opacity(0.9)).lineLimit(1).minimumScaleFactor(0.7)
            Text(lang.num(inRound)).font(.system(size: compact ? 40 : 44, weight: .semibold, design: .serif)).foregroundStyle(.white)
                .contentTransition(.numericText()).lineLimit(1).minimumScaleFactor(0.6)
            if compact {
                Text(target > 0 ? "/ \(lang.num(target))" : "∞").font(.caption).foregroundStyle(gold)
            }
            HStack(spacing: 8) {
                Button(intent: ResetTasbihIntent()) {
                    Image(systemName: "arrow.counterclockwise").font(.caption.bold()).foregroundStyle(.white)
                        .frame(width: 30, height: 30).background(.white.opacity(0.16), in: Circle())
                }
                .buttonStyle(.plain)
                Button(intent: IncrementTasbihIntent()) {
                    Image(systemName: "plus").font(.headline.bold()).foregroundStyle(.white)
                        .frame(width: compact ? 56 : 64, height: 30)
                        .background(gold.opacity(0.35), in: Capsule())
                        .overlay(Capsule().strokeBorder(gold.opacity(0.8), lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 2)
            .environment(\.layoutDirection, .leftToRight)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// The last seven days of tasbih, today in gold.
struct WeeklyBars: View {
    let lang: Lang

    var body: some View {
        let m = More.of(lang)
        let week = TasbihHistory.lastWeek()
        let top = max(week.map(\.count).max() ?? 0, 1)
        let total = week.reduce(0) { $0 + $1.count }
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(m.weeklyStats).font(.caption.weight(.semibold)).foregroundStyle(.white)
                Spacer()
                Text("\(L10n.of(lang).total): \(lang.num(total))").font(.caption2).foregroundStyle(gold)
            }
            HStack(alignment: .bottom, spacing: 5) {
                ForEach(Array(week.enumerated()), id: \.offset) { i, d in
                    let today = i == week.count - 1
                    VStack(spacing: 3) {
                        Text(d.count > 0 ? lang.num(d.count) : "").font(.system(size: 8, weight: .semibold)).foregroundStyle(.white.opacity(0.85))
                            .lineLimit(1).minimumScaleFactor(0.5)
                        RoundedRectangle(cornerRadius: 4, style: .continuous)
                            .fill(today ? AnyShapeStyle(Brand.goldGradient) : AnyShapeStyle(.white.opacity(0.25)))
                            .frame(height: max(4, 58 * CGFloat(d.count) / CGFloat(top)))
                        Text(shortDay(d.weekday)).font(.system(size: 8, weight: .semibold))
                            .foregroundStyle(today ? gold : .white.opacity(0.6)).lineLimit(1).minimumScaleFactor(0.6)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(maxHeight: .infinity, alignment: .bottom)
        }
    }

    private func shortDay(_ d: Int) -> String {
        let name = Hijri.weekday(lang, d)
        return lang == .ar ? String(name.dropFirst(2).prefix(4)) : String(name.prefix(3)).uppercased()
    }
}
