import SwiftUI
import WidgetKit

/// The widget gallery: every widget as a live preview, how to add it, and the widget style.
/// iOS doesn't let apps place widgets themselves, so "Add" shows the two-step path instead.
struct WidgetsView: View {
    @Environment(AppModel.self) private var model
    @State private var adding: GalleryItem?

    var body: some View {
        let m = model.m
        ScrollViewReader { proxy in
        ScrollView {
            VStack(spacing: 0) {
                Text(m.widgetsHint).font(.subheadline).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 20).padding(.top, 4)

                ForEach(0..<4, id: \.self) { g in
                    SectionLabel(text: m.groups[g]).padding(.top, 8)
                    VStack(spacing: 14) {
                        ForEach(GalleryItem.all.filter { $0.group == g }) { item in
                            WidgetCard(item: item) { adding = item }.id(item.id)
                        }
                    }
                    .padding(.horizontal, 16).padding(.top, 6)
                }

                SectionLabel(text: lockTitle).padding(.top, 8).id("lock")
                LockScreenStrip().padding(.horizontal, 16).padding(.top, 6)

                SectionLabel(text: m.widgetStyle).padding(.top, 8).id("style")
                WidgetStyleSettings().padding(.horizontal, 16).padding(.top, 6).padding(.bottom, 40)
            }
            .frame(maxWidth: 760).frame(maxWidth: .infinity)
        }
        .onAppear {
            // CI screenshots: -ciScroll <widget id | lock | style>
            if let target = UserDefaults.standard.string(forKey: "ciScroll") {
                Task { try? await Task.sleep(for: .milliseconds(600)); proxy.scrollTo(target, anchor: .top) }
            }
        }
        }
        .background(Backdrop())
        .navigationTitle(m.widgetsTitle)
        .sheet(item: $adding) { item in
            AddWidgetSheet(item: item).presentationDetents([.medium, .large])
        }
    }

    private var lockTitle: String {
        switch model.lang { case .ar: "شاشة القفل"; case .en: "Lock Screen"; case .fr: "Écran verrouillé" }
    }
}

/// One widget in the gallery.
struct GalleryItem: Identifiable {
    let id: String
    let group: Int
    let size: WidgetSize
    let sizes: [WidgetSize]

    static let all: [GalleryItem] = [
        GalleryItem(id: "prayer", group: 0, size: .medium, sizes: [.small, .medium, .large, .rectangular, .circular, .inline]),
        GalleryItem(id: "hijri", group: 0, size: .small, sizes: [.small, .medium, .rectangular, .circular, .inline]),
        GalleryItem(id: "clock", group: 0, size: .small, sizes: [.small]),
        GalleryItem(id: "digital", group: 0, size: .medium, sizes: [.small, .medium]),
        GalleryItem(id: "progress", group: 0, size: .medium, sizes: [.small, .medium, .circular]),
        GalleryItem(id: "timeline", group: 1, size: .medium, sizes: [.small, .medium, .large, .rectangular]),
        GalleryItem(id: "morning", group: 1, size: .medium, sizes: [.small, .medium, .large, .rectangular]),
        GalleryItem(id: "day", group: 1, size: .medium, sizes: [.small, .medium, .large, .rectangular]),
        GalleryItem(id: "night", group: 1, size: .medium, sizes: [.small, .medium, .large, .rectangular]),
        GalleryItem(id: "dhikr", group: 1, size: .small, sizes: [.small, .medium, .large, .rectangular, .inline]),
        GalleryItem(id: "quick", group: 1, size: .medium, sizes: [.small, .medium, .large]),
        GalleryItem(id: "scripture", group: 2, size: .medium, sizes: [.small, .medium, .large, .rectangular]),
        GalleryItem(id: "tasbih", group: 3, size: .small, sizes: [.small, .circular, .rectangular]),
        GalleryItem(id: "counter", group: 3, size: .medium, sizes: [.medium]),
    ]
}

/// The live face of a gallery item.
struct WidgetFace: View {
    @Environment(AppModel.self) private var model
    let id: String
    let size: WidgetSize
    let now: Date

    var body: some View {
        let lang = model.lang
        let script = Script(rawValue: model.script) ?? .ruqaa
        switch id {
        case "prayer": PrayerWidgetView(size: size, place: model.place, now: now, lang: lang, script: script)
        case "hijri": HijriWidgetView(size: size, now: now, lang: lang, script: script, offset: model.hijriOffset)
        case "clock": IslamicClockFace(date: now, palette: Palette.all[safe: model.palette] ?? .mushaf).padding(-10)
        case "digital": DigitalClockWidgetView(size: size, now: now, lang: lang, script: script, offset: model.hijriOffset)
        case "progress": ProgressWidgetView(size: size, now: now, lang: lang)
        case "timeline": AzkarListWidgetView(size: size, set: .timeline, now: now, lang: lang)
        case "morning": AzkarListWidgetView(size: size, set: .morning, now: now, lang: lang)
        case "day": AzkarListWidgetView(size: size, set: .day, now: now, lang: lang)
        case "night": AzkarListWidgetView(size: size, set: .night, now: now, lang: lang)
        case "dhikr": DhikrWidgetView(size: size, zikr: DhikrWidgetView.zikr(at: now), lang: lang)
        case "quick": QuickAzkarWidgetView(size: size, lang: lang, now: now)
        case "scripture": ScriptureWidgetView(size: size, source: .mixed, now: now, lang: lang, script: script)
        default:
            TasbihWidgetView(size: size, count: model.tasbihCount, target: model.tasbihTarget, phrase: model.tasbihPhrase, lang: lang)
        }
    }
}

private struct WidgetCard: View {
    @Environment(AppModel.self) private var model
    let item: GalleryItem
    let onAdd: () -> Void

    var body: some View {
        let m = model.m
        VStack(spacing: 14) {
            TimelineView(.periodic(from: .now, by: item.id == "clock" ? 1 : 30)) { ctx in
                WidgetPreviewFrame(size: item.size, palette: Palette.all[safe: model.palette] ?? .mushaf) {
                    WidgetFace(id: item.id, size: item.size, now: ctx.date)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(LinearGradient(colors: [Color(hex: 0x5B7FA3), Color(hex: 0xB98A6E), Color(hex: 0x3E6B58)], startPoint: .topLeading, endPoint: .bottomTrailing).opacity(0.85),
                        in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .environment(\.layoutDirection, model.lang.isRTL ? .rightToLeft : .leftToRight)

            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(m.widgetNames[item.id] ?? item.id).font(.headline)
                    Text(m.widgetDescriptions[item.id] ?? "").font(.subheadline).foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(sizesText).font(.caption).foregroundStyle(.tertiary)
                }
                Spacer(minLength: 8)
                Button(action: onAdd) {
                    Label(m.add, systemImage: "plus").font(.subheadline.weight(.semibold)).padding(.horizontal, 4)
                }
                .prominentGlassButton()
                .tint(Theme.accent)
            }
        }
        .padding(14)
        .glassCard(cornerRadius: 26)
    }

    private var sizesText: String {
        let names: [WidgetSize: (String, String, String)] = [
            .small: ("صغير", "Small", "Petit"), .medium: ("متوسط", "Medium", "Moyen"), .large: ("كبير", "Large", "Grand"),
            .circular: ("قفل دائري", "Lock circle", "Verrou rond"), .rectangular: ("قفل مستطيل", "Lock rectangle", "Verrou rectangle"),
            .inline: ("قفل سطري", "Lock line", "Verrou ligne"),
        ]
        return item.sizes.compactMap { names[$0] }.map { model.lang == .ar ? $0.0 : model.lang == .en ? $0.1 : $0.2 }.joined(separator: " · ")
    }
}

/// Lock Screen widgets on a wallpaper-like strip.
private struct LockScreenStrip: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { ctx in
            VStack(spacing: 10) {
                Text(ctx.date, style: .time).font(.system(size: 54, weight: .semibold, design: .rounded)).foregroundStyle(.white.opacity(0.92))
                HStack(spacing: 10) {
                    WidgetPreviewFrame(size: .circular) { WidgetFace(id: "hijri", size: .circular, now: ctx.date) }
                    WidgetPreviewFrame(size: .circular) { WidgetFace(id: "tasbih", size: .circular, now: ctx.date) }
                    WidgetPreviewFrame(size: .rectangular) { WidgetFace(id: "prayer", size: .rectangular, now: ctx.date) }
                }
                WidgetPreviewFrame(size: .rectangular) { WidgetFace(id: "morning", size: .rectangular, now: ctx.date) }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
            .background(LinearGradient(colors: [Color(hex: 0x1B2A3A), Color(hex: 0x3C5A50)], startPoint: .top, endPoint: .bottom),
                        in: RoundedRectangle(cornerRadius: 26, style: .continuous))
        }
        .environment(\.layoutDirection, model.lang.isRTL ? .rightToLeft : .leftToRight)
    }
}

private struct AddWidgetSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let item: GalleryItem

    var body: some View {
        let m = model.m
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    HStack {
                        Spacer()
                        TimelineView(.periodic(from: .now, by: 30)) { ctx in
                            WidgetPreviewFrame(size: item.size, palette: Palette.all[safe: model.palette] ?? .mushaf) {
                                WidgetFace(id: item.id, size: item.size, now: ctx.date)
                            }
                        }
                        Spacer()
                    }
                    Text(m.widgetNames[item.id] ?? "").font(.title3.weight(.semibold))
                    ForEach(Array(m.addSteps.enumerated()), id: \.offset) { i, step in
                        HStack(alignment: .top, spacing: 12) {
                            Text(model.lang.num(i + 1)).font(.headline).foregroundStyle(Theme.onAccent)
                                .frame(width: 30, height: 30).background(Theme.accent, in: Circle())
                            Text(step).font(.body).fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    Text(m.lockScreenHint).font(.footnote).foregroundStyle(.secondary)
                }
                .padding(20)
            }
            .background(Backdrop())
            .navigationTitle(m.howToAdd)
            .navigationBarTitleDisplayModeInline()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(model.s.done) { dismiss() }
                }
            }
        }
    }
}

/// Colours, calligraphy and the Hijri adjustment for every widget.
struct WidgetStyleSettings: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var am = model
        let m = model.m
        VStack(alignment: .leading, spacing: 16) {
            Text(m.palette).font(.subheadline.weight(.semibold))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(Palette.all) { p in
                        Button { withAnimation(.snappy) { model.palette = p.id } } label: {
                            VStack(spacing: 6) {
                                Circle().fill(p.gradient).frame(width: 44, height: 44)
                                    .overlay(Circle().strokeBorder(model.palette == p.id ? Theme.gold : .white.opacity(0.4), lineWidth: model.palette == p.id ? 3 : 1))
                                Text(p.label(model.lang)).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                            }
                            .frame(width: 70)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Divider()
            Text(m.calligraphy).font(.subheadline.weight(.semibold))
            HStack(spacing: 8) {
                ForEach(Script.allCases) { sc in
                    let on = model.script == sc.rawValue
                    Button { withAnimation(.snappy) { model.script = sc.rawValue } } label: {
                        VStack(spacing: 2) {
                            Text("الجمعة").font(sc.font(24)).foregroundStyle(on ? Theme.accent : Color.primary).lineLimit(1).minimumScaleFactor(0.6)
                            Text(sc.label(model.lang)).font(.caption2).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity).frame(height: 70)
                        .background(on ? Theme.accentSoft : Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder(on ? Theme.accent : .clear, lineWidth: 1.5))
                    }
                    .buttonStyle(.plain)
                }
            }

            Divider()
            Stepper(value: $am.hijriOffset, in: -2...2) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(m.hijriOffset).font(.subheadline.weight(.semibold))
                    Text("\(m.days(model.hijriOffset)) · \(Hijri.format(model.lang, Hijri.of(.now, offset: model.hijriOffset)))")
                        .font(.caption).foregroundStyle(Theme.accent)
                }
            }
            Text(m.hijriOffsetHint).font(.caption).foregroundStyle(.secondary)
        }
        .padding(16)
        .glassCard(cornerRadius: 24)
    }
}

extension View {
    /// Large navigation title on iOS (no-op elsewhere).
    @ViewBuilder
    func navigationBarTitleDisplayModeLarge() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.large)
        #else
        self
        #endif
    }

    /// Inline navigation title on iOS (no-op elsewhere).
    @ViewBuilder
    func navigationBarTitleDisplayModeInline() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }
}
