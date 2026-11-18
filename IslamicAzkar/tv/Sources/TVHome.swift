import SwiftUI

/// Home: the date, the next prayer with today's times, the four azkar of the day and every category.
struct TVHomeView: View {
    @Environment(TVModel.self) private var model

    private struct TileInfo: Identifiable {
        let reading: Reading, title: String, symbol: String, color: Color
        var id: String { reading.key }
    }

    var body: some View {
        let s = model.s, lang = model.lang
        let tiles = [
            TileInfo(reading: .morning, title: s.morningAzkar, symbol: "sun.max.fill", color: .orange),
            TileInfo(reading: .evening, title: s.eveningAzkar, symbol: "moon.stars.fill", color: .indigo),
            TileInfo(reading: .chapter(28), title: s.sleepAzkar, symbol: "moon.zzz.fill", color: .blue),
            TileInfo(reading: .chapter(25), title: s.afterPrayer, symbol: "building.columns.fill", color: .green),
        ]
        ScrollView {
            VStack(alignment: .leading, spacing: 36) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(Hijri.format(lang, Hijri.of(.now))).font(.callout).foregroundStyle(.secondary)
                    Text(s.appName).font(lang == .ar ? .amiri(76, bold: true) : .system(size: 64, weight: .bold))
                        .foregroundStyle(.white)
                }
                TVPrayerCard()

                section(s.recommendedNow)
                HStack(spacing: 40) {
                    ForEach(tiles) { tile in
                        NavigationLink(value: TVRoute.reader(tile.reading)) {
                            TVTile(title: tile.title, subtitle: s.azkar(AzkarData.shared.items(tile.reading).count),
                                   symbol: tile.symbol, color: tile.color,
                                   badge: tile.reading == Reading.recommended() ? model.m.today : nil)
                        }
                        .buttonStyle(.card)
                    }
                }

                section(s.categories)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 40), count: 3), spacing: 40) {
                    ForEach(AzkarData.shared.categories) { c in
                        NavigationLink(value: TVRoute.category(c.key)) {
                            HStack(spacing: 24) {
                                Image(systemName: categorySymbol(c.key)).font(.title2)
                                    .frame(width: 64, height: 64)
                                    .background(Brand.emerald, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                    .foregroundStyle(.white)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(s.category(c.key)).font(.headline).lineLimit(1)
                                    Text(s.chapters(c.chapters.count)).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(24)
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.card)
                    }
                }
            }
            .padding(.horizontal, 90)
            .padding(.vertical, 50)
        }
    }

    private func section(_ text: String) -> some View {
        Text(text).font(.headline).foregroundStyle(Brand.mushafGold).textCase(.uppercase)
    }
}

/// A big tile: coloured symbol, title and a count.
struct TVTile: View {
    let title, subtitle, symbol: String
    let color: Color
    var badge: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .top) {
                Image(systemName: symbol).font(.system(size: 40, weight: .semibold)).foregroundStyle(.white)
                    .frame(width: 84, height: 84)
                    .background(color, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                Spacer()
                if let badge {
                    Text(badge).font(.caption.weight(.semibold)).foregroundStyle(Brand.emeraldBright)
                        .padding(.horizontal, 14).padding(.vertical, 6)
                        .background(Brand.emerald.opacity(0.3), in: Capsule())
                }
            }
            Spacer(minLength: 0)
            Text(title).font(.title3.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
            Text(subtitle).font(.callout).foregroundStyle(.secondary)
        }
        .padding(30)
        .frame(width: 370, height: 260)
    }
}

/// The next prayer with a countdown, and today's six times.
struct TVPrayerCard: View {
    @Environment(TVModel.self) private var model

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { ctx in
            let lang = model.lang, m = model.m, place = model.place
            let next = Prayers.next(place, now: ctx.date)
            let times = Prayers.times(place, day: ctx.date)
            HStack(alignment: .center, spacing: 40) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(m.nextPrayer).font(.callout).foregroundStyle(.secondary)
                    Text(m.prayers[next.index]).font(lang == .ar ? .amiri(56, bold: true) : .system(size: 46, weight: .bold))
                        .foregroundStyle(Brand.mushafGold)
                    Text(next.time, style: .relative).font(.callout.monospacedDigit()).foregroundStyle(.secondary)
                    Label(place.label(lang), systemImage: "location.fill").font(.caption).foregroundStyle(.tertiary)
                }
                .frame(minWidth: 360, alignment: .leading)
                Spacer()
                HStack(spacing: 18) {
                    ForEach(0..<6, id: \.self) { i in
                        let on = i == next.index
                        VStack(spacing: 8) {
                            Text(m.prayers[i]).font(.callout.weight(on ? .bold : .regular))
                            Text(Prayers.clock(times[i], place, lang)).font(.title3.monospacedDigit().weight(on ? .bold : .regular))
                        }
                        .foregroundStyle(on ? Brand.mushafGold : .white.opacity(i == Prayers.sunrise ? 0.5 : 0.85))
                        .frame(width: 150).padding(.vertical, 18)
                        .background(on ? Color.white.opacity(0.12) : .clear, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                    }
                }
            }
            .padding(36)
            .background(.white.opacity(0.07), in: RoundedRectangle(cornerRadius: 34, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 34, style: .continuous).strokeBorder(Brand.mushafGold.opacity(0.25), lineWidth: 1.5))
        }
    }
}

/// A category: its chapters, each opening the reader.
struct TVCategoryView: View {
    @Environment(TVModel.self) private var model
    let key: String

    var body: some View {
        let s = model.s, lang = model.lang, data = AzkarData.shared
        let ids = data.categories.first { $0.key == key }?.chapters ?? []
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text(s.category(key)).font(.system(size: 54, weight: .bold))
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 40), GridItem(.flexible(), spacing: 40)], spacing: 30) {
                    ForEach(ids, id: \.self) { id in
                        if let c = data.chapterById[id] {
                            NavigationLink(value: TVRoute.reader(.chapter(id))) {
                                HStack {
                                    Text(c.title(lang)).font(.headline).multilineTextAlignment(.leading).lineLimit(2)
                                    Spacer()
                                    Text(lang.num(c.items.count)).font(.callout).foregroundStyle(.secondary)
                                }
                                .padding(28).frame(maxWidth: .infinity, minHeight: 110)
                            }
                            .buttonStyle(.card)
                        }
                    }
                }
            }
            .padding(.horizontal, 90).padding(.vertical, 50)
        }
        .background(TVBackdrop())
    }
}
