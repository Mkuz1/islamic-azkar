import SwiftUI

struct HomeView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var query = ""

    private let data = AzkarData.shared

    var body: some View {
        let s = model.s
        List {
            if query.isEmpty {
                Section {
                    let tiles = todayTiles
                    let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: sizeClass == .regular ? 4 : 2)
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(tiles, id: \.reading) { t in
                            Button { model.show(t.reading, split: split) } label: { TodayCard(tile: t) }
                                .buttonStyle(.plain)
                        }
                    }
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                } header: {
                    Text(hijriDate)
                }

                Section(s.categories) {
                    ForEach(data.categories) { cat in
                        NavigationLink(value: Route.category(cat.key)) {
                            HStack(spacing: 14) {
                                SymbolSquare(symbol: categorySymbol(cat.key), color: categoryHue(cat.key))
                                Text(s.category(cat.key))
                                Spacer()
                                Text(model.lang.num(cat.chapters.count)).foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section {
                } footer: {
                    Text("صدقة جارية عن محمد كوسا وديما غنوم")
                        .font(.amiri(15)).frame(maxWidth: .infinity)
                }
            } else {
                SearchResults(query: query)
            }
        }
        .insetGroupedList()
        .navigationTitle(s.appName)
        .searchable(text: $query, prompt: s.searchHint)
        .toolbar {
            ToolbarItem(placement: .primaryAction) { LanguageMenu() }
        }
    }

    private struct Tile {
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
            Tile(reading: .evening, title: s.eveningAzkar, symbol: "moon.stars.fill", color: .indigo, count: data.evening.count, now: rec == .evening),
            Tile(reading: .chapter(28), title: s.sleepAzkar, symbol: "bed.double.fill", color: .blue, count: data.items(.chapter(28)).count, now: rec == .chapter(28)),
            Tile(reading: .chapter(25), title: s.afterPrayer, symbol: "hands.and.sparkles.fill", color: .green, count: data.items(.chapter(25)).count, now: rec == .chapter(25)),
        ]
    }

    /// Summary card in the style of the Health and Fitness apps.
    private struct TodayCard: View {
        @Environment(AppModel.self) private var model
        let tile: Tile

        var body: some View {
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    SymbolSquare(symbol: tile.symbol, color: tile.color, size: 32)
                    Spacer()
                    if tile.now {
                        Text(model.lang == .ar ? "الآن" : model.lang == .fr ? "Maintenant" : "Now")
                            .font(.caption.weight(.semibold)).foregroundStyle(.tint)
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(.tint.opacity(0.12), in: Capsule())
                    }
                }
                .padding(.bottom, 12)
                Text(tile.title).font(.headline).lineLimit(1)
                Text(model.s.azkar(tile.count)).font(.footnote).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .card()
            .contentShape(Rectangle())
        }
    }

    private var hijriDate: String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .islamicUmmAlQura)
        f.locale = model.lang.locale
        f.dateFormat = "d MMMM y"
        return f.string(from: .now)
    }
}

/// Language switcher in the navigation bar (a Liquid Glass button on iOS 26).
struct LanguageMenu: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var m = model
        Menu {
            Picker(model.s.language, selection: $m.lang) {
                Text("العربية").tag(Lang.ar)
                Text("English").tag(Lang.en)
                Text("Français").tag(Lang.fr)
            }
        } label: {
            Label(model.s.language, systemImage: "globe")
        }
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

        if chapters.isEmpty && hits.isEmpty {
            ContentUnavailableView.search(text: query)
        }
        if !chapters.isEmpty {
            Section(model.s.categories) {
                ForEach(chapters) { c in
                    ChapterRow(chapter: c) { model.show(.chapter(c.id), split: split) }
                }
            }
        }
        if !hits.isEmpty {
            Section(model.s.azkar(hits.count)) {
                ForEach(hits, id: \.1.id) { c, z in
                    Button { model.show(.chapter(c.id), focus: z.id, split: split) } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(c.title(model.lang)).font(.footnote.weight(.semibold)).foregroundStyle(.tint)
                            Text(z.ar).font(.amiri(18)).lineLimit(3).foregroundStyle(.primary)
                                .frame(maxWidth: .infinity, alignment: .trailing).multilineTextAlignment(.trailing)
                        }
                    }
                }
            }
        }
    }
}

/// A chapter row: title, Arabic subtitle and count, with the system disclosure chevron.
struct ChapterRow: View {
    @Environment(AppModel.self) private var model
    let chapter: Chapter
    var selected = false
    let action: () -> Void

    var body: some View {
        let lang = model.lang
        Button(action: action) {
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
            .contentShape(Rectangle())
        }
        .listRowBackground(selected ? Color.accentColor.opacity(0.15) : nil)
    }
}

struct CategoryView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
    let key: String

    var body: some View {
        let data = AzkarData.shared
        let chapters = (data.categories.first { $0.key == key }?.chapters ?? []).compactMap { data.chapterById[$0] }
        List {
            Section {
                ForEach(chapters) { c in
                    ChapterRow(chapter: c, selected: split && model.detail == .chapter(c.id)) {
                        model.show(.chapter(c.id), split: split)
                    }
                }
            } header: {
                Text(model.s.chapters(chapters.count))
            }
        }
        .insetGroupedList()
        .navigationTitle(model.s.category(key))
    }
}
