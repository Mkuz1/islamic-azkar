import SwiftUI

struct HomeView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var query = ""

    private let data = AzkarData.shared

    var body: some View {
        let s = model.s
        ScrollView {
            VStack(spacing: 0) {
                header
                SearchField(text: $query, prompt: s.searchHint).padding(.horizontal, 16).padding(.top, 14)

                if query.trimmingCharacters(in: .whitespaces).isEmpty {
                    UpdateBanner().padding(.horizontal, 16).padding(.top, 16)
                    NextPrayerCard().padding(.horizontal, 16).padding(.top, 16)

                    SectionLabel(text: s.recommendedNow).padding(.top, 8)
                    let columns = Array(repeating: GridItem(.flexible(), spacing: 14), count: sizeClass == .regular ? 4 : 2)
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(todayTiles, id: \.reading) { t in
                            Button { model.show(t.reading, split: split) } label: { TodayCard(tile: t) }
                                .buttonStyle(PressStyle())
                        }
                    }
                    .padding(.horizontal, 16).padding(.top, 6)

                    SectionLabel(text: s.categories).padding(.top, 10)
                    GlassGroup {
                        ForEach(Array(data.categories.enumerated()), id: \.element.id) { i, cat in
                            NavigationLink(value: Route.category(cat.key)) {
                                GlassRow(divider: i < data.categories.count - 1) {
                                    SymbolSquare(symbol: categorySymbol(cat.key), color: categoryHue(cat.key), size: 32)
                                    Text(s.category(cat.key)).foregroundStyle(.primary)
                                    Spacer()
                                    Text(model.lang.num(cat.chapters.count)).foregroundStyle(.secondary)
                                    Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16).padding(.top, 6)

                    if let guide = data.umrahGuide {
                        SectionLabel(text: GuideTexts.of(model.lang).guide).padding(.top, 10)
                        GlassGroup { GuideLinkRow(guide: guide) }.padding(.horizontal, 16).padding(.top, 6)
                    }

                    VStack(spacing: 4) {
                        Text(dedicationAr).font(.amiri(17)).foregroundStyle(.secondary)
                        if let t = dedicationTranslation(model.lang) {
                            Text(t).font(.caption).foregroundStyle(.tertiary)
                        }
                    }
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24).padding(.top, 26).padding(.bottom, 40)
                } else {
                    SearchResults(query: query).padding(.horizontal, 16).padding(.top, 14).padding(.bottom, 40)
                }
            }
            .frame(maxWidth: 900)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.immediately)
        .background(Backdrop())
        .hiddenNavigationBar()
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(Hijri.format(model.lang, Hijri.of(.now, offset: model.hijriOffset)))
                .font(.subheadline).foregroundStyle(.secondary)
            HStack(alignment: .center) {
                Text(model.s.appName)
                    .font(model.lang == .ar ? .system(size: 38, weight: .heavy) : .system(size: 34, weight: .bold))
                    .lineLimit(1).minimumScaleFactor(0.7)
                Spacer(minLength: 12)
                LanguagePill()
            }
        }
        .padding(.horizontal, 20).padding(.top, 12)
    }

    fileprivate struct Tile {
        let reading: Reading
        let title: String
        let symbol: String
        let color: Color
        let count: Int
        let now: Bool
    }

    private var todayTiles: [Tile] {
        let s = model.s
        let rec = Reading.recommended()
        return [
            Tile(reading: .morning, title: s.morningAzkar, symbol: "sun.max.fill", color: .orange, count: data.morning.count, now: rec == .morning),
            Tile(reading: .evening, title: s.eveningAzkar, symbol: "moon.haze.fill", color: .indigo, count: data.evening.count, now: rec == .evening),
            Tile(reading: .chapter(28), title: s.sleepAzkar, symbol: "moon.fill", color: .blue, count: data.items(.chapter(28)).count, now: rec == .chapter(28)),
            Tile(reading: .chapter(25), title: s.afterPrayer, symbol: "building.columns.fill", color: .green, count: data.items(.chapter(25)).count, now: rec == .chapter(25)),
        ]
    }

    /// Glass summary card, in the style of the Health and Fitness apps.
    private struct TodayCard: View {
        @Environment(AppModel.self) private var model
        let tile: Tile

        var body: some View {
            VStack(alignment: .leading, spacing: 3) {
                HStack(alignment: .top) {
                    SymbolSquare(symbol: tile.symbol, color: tile.color, size: 38)
                    Spacer()
                    if tile.now {
                        Text(model.lang == .ar ? "الآن" : model.lang == .fr ? "Maintenant" : "Now")
                            .font(.caption.weight(.semibold)).foregroundStyle(Theme.accent)
                            .padding(.horizontal, 10).padding(.vertical, 4)
                            .background(Theme.accentSoft, in: Capsule())
                    }
                }
                .padding(.bottom, 18)
                Text(tile.title).font(.headline).foregroundStyle(.primary).lineLimit(1).minimumScaleFactor(0.8)
                Text(model.s.azkar(tile.count)).font(.subheadline).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .glassCard(cornerRadius: 24)
            .contentShape(Rectangle())
        }
    }
}

/// The next prayer, or an invitation to set a location.
private struct NextPrayerCard: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        let m = model.m
        Button { model.tab = .settings } label: {
            HStack(spacing: 14) {
                SymbolSquare(symbol: "building.columns.fill", color: Theme.accent, size: 40)
                if let place = model.place {
                    TimelineView(.periodic(from: .now, by: 30)) { ctx in
                        let next = Prayers.next(place, now: ctx.date)
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(m.nextPrayer).font(.caption).foregroundStyle(.secondary)
                                Text(m.prayers[next.index]).font(.headline).foregroundStyle(.primary)
                                Text(place.label(model.lang)).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text(Prayers.clock(next.time, place, model.lang)).font(.title2.weight(.semibold).monospacedDigit())
                                    .foregroundStyle(Theme.accent)
                                Text(next.time, style: .relative).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }
                } else {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(m.prayerTimes).font(.headline).foregroundStyle(.primary)
                        Text(m.setLocationWidget).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
                }
            }
            .padding(14)
            .glassCard(cornerRadius: 24)
        }
        .buttonStyle(PressStyle())
    }
}

/// Glass search field.
struct SearchField: View {
    @Binding var text: String
    let prompt: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
            TextField(prompt, text: $text).textFieldStyle(.plain).autocorrectionDisabled()
            if !text.isEmpty {
                Button { text = "" } label: { Image(systemName: "xmark.circle.fill").foregroundStyle(.tertiary) }.buttonStyle(.plain)
            }
        }
        .font(.body)
        .padding(.horizontal, 16).frame(height: 50)
        .glassCard(cornerRadius: 25)
    }
}

/// A rounded glass group of rows (inset-grouped list look).
struct GlassGroup<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        VStack(spacing: 0) { content }
            .glassCard(cornerRadius: 24)
    }
}

struct GlassRow<Content: View>: View {
    var divider = true
    @ViewBuilder let content: Content
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 14) { content }
                .padding(.horizontal, 16).frame(minHeight: 56)
                .contentShape(Rectangle())
            if divider { Divider().padding(.leading, 62) }
        }
        .contentShape(Rectangle())
    }
}

extension View {
    /// Hides the navigation bar on iOS (the Home screen draws its own header).
    @ViewBuilder
    func hiddenNavigationBar() -> some View {
        #if os(iOS)
        self.toolbar(.hidden, for: .navigationBar)
        #else
        self
        #endif
    }
}

/// Springy press feedback for cards.
struct PressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())   // the whole card is clickable, not only its text (macOS)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

private struct SearchResults: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
    let query: String

    var body: some View {
        let data = AzkarData.shared
        let q = normalizeArabic(query.trimmingCharacters(in: .whitespaces))
        let chapters = Array(data.chapters.filter {
            normalizeArabic($0.ar).contains(q) || $0.en.lowercased().contains(q) || $0.fr.lowercased().contains(q)
        }.prefix(30))
        let hits = Array(data.chapters.flatMap { c in c.items.map { (c, $0) } }.filter { _, z in
            normalizeArabic(z.ar).contains(q) || z.en.lowercased().contains(q) || z.fr.lowercased().contains(q)
        }.prefix(40))

        VStack(spacing: 0) {
            if chapters.isEmpty && hits.isEmpty {
                ContentUnavailableView.search(text: query).padding(.top, 40)
            }
            if !chapters.isEmpty {
                SectionLabel(text: model.s.categories)
                GlassGroup {
                    ForEach(Array(chapters.enumerated()), id: \.element.id) { i, c in
                        ChapterRow(chapter: c, divider: i < chapters.count - 1) { model.show(.chapter(c.id), split: split) }
                    }
                }
            }
            if !hits.isEmpty {
                SectionLabel(text: model.s.azkar(hits.count))
                VStack(spacing: 10) {
                    ForEach(hits, id: \.1.id) { c, z in
                        Button { model.show(.chapter(c.id), focus: z.id, split: split) } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(c.title(model.lang)).font(.footnote.weight(.semibold)).foregroundStyle(Theme.accent)
                                Text(z.ar).font(.amiri(18)).lineLimit(3).foregroundStyle(.primary)
                                    .frame(maxWidth: .infinity, alignment: .trailing).multilineTextAlignment(.trailing)
                            }
                            .padding(14)
                            .glassCard(cornerRadius: 20)
                        }
                        .buttonStyle(PressStyle())
                    }
                }
            }
        }
    }
}

/// A chapter row: title, Arabic subtitle and count, with a chevron.
struct ChapterRow: View {
    @Environment(AppModel.self) private var model
    let chapter: Chapter
    var selected = false
    var divider = true
    let action: () -> Void

    var body: some View {
        let lang = model.lang
        Button(action: action) {
            VStack(spacing: 0) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(chapter.title(lang)).foregroundStyle(.primary).multilineTextAlignment(.leading)
                        if lang != .ar {
                            Text(chapter.ar).font(.amiri(15)).foregroundStyle(.secondary).lineLimit(1)
                        }
                    }
                    Spacer(minLength: 8)
                    Text(lang.num(chapter.items.count)).foregroundStyle(.secondary)
                    Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
                }
                .padding(.horizontal, 16).padding(.vertical, 12).frame(minHeight: 56)
                .contentShape(Rectangle())
                .background(selected ? Theme.accentSoft : .clear)
                if divider { Divider().padding(.leading, 16) }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

struct CategoryView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
    let key: String

    var body: some View {
        let data = AzkarData.shared
        let chapters = (data.categories.first { $0.key == key }?.chapters ?? []).compactMap { data.chapterById[$0] }
        ScrollView {
            VStack(spacing: 0) {
                if key == "hajj", let guide = data.umrahGuide {
                    GlassGroup { GuideLinkRow(guide: guide) }.padding(.horizontal, 16).padding(.top, 8)
                }
                SectionLabel(text: model.s.chapters(chapters.count))
                GlassGroup {
                    ForEach(Array(chapters.enumerated()), id: \.element.id) { i, c in
                        ChapterRow(chapter: c, selected: split && model.detail == .chapter(c.id), divider: i < chapters.count - 1) {
                            model.show(.chapter(c.id), split: split)
                        }
                    }
                }
                .padding(.horizontal, 16).padding(.bottom, 40)
            }
            .frame(maxWidth: 900).frame(maxWidth: .infinity)
        }
        .background(Backdrop())
        .navigationTitle(model.s.category(key))
        .navigationBarTitleDisplayModeLarge()
    }
}
