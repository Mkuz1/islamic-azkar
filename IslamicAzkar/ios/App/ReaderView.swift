import SwiftUI
#if os(iOS)
import UIKit
#else
import AppKit
#endif

struct ReaderView: View {
    @Environment(AppModel.self) private var model
    let reading: Reading
    let focus: Int?

    @State private var remaining: [Int: Int] = [:]
    @State private var celebration: Int?   // streak shown on the completion card

    var body: some View {
        let data = AzkarData.shared
        let items = data.items(reading)
        let title = data.title(reading, model.s, model.lang)
        let source: String = {
            if !items.isEmpty, items.allSatisfy({ $0.ref?.en.hasPrefix("Quran") == true }) { return GuideTexts.of(model.lang).fromQuran }
            return items.contains { $0.ref != nil } ? GuideTexts.of(model.lang).fromSunnah : model.s.book
        }()
        let subtitle: String = {
            if case .chapter(let id) = reading, model.lang != .ar { return data.chapterById[id]?.ar ?? source }
            return source
        }()
        let total = items.reduce(0) { $0 + $1.repeatCount }
        let done = items.reduce(0) { $0 + ($1.repeatCount - left($1)) }

        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 14) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("\(subtitle) · \(model.s.azkar(items.count))").font(.subheadline).foregroundStyle(.secondary)
                        ProgressView(value: Double(done), total: Double(max(total, 1)))
                    }
                    .padding(.horizontal, 4)

                    ForEach(Array(items.enumerated()), id: \.element.id) { i, z in
                        ZikrCard(zikr: z, index: i + 1, total: items.count, remaining: left(z), chapterTitle: title) { all in
                            let r = all ? 0 : left(z) - 1
                            withAnimation(.snappy) { remaining[z.id] = r }
                            if r == 0, i + 1 < items.count {
                                Task {
                                    try? await Task.sleep(for: .milliseconds(300))
                                    withAnimation(.smooth) { proxy.scrollTo(items[i + 1].id, anchor: .top) }
                                }
                            }
                            if r == 0, items.allSatisfy({ $0.id == z.id || left($0) == 0 }) {
                                Task {
                                    try? await Task.sleep(for: .milliseconds(450))
                                    celebration = Streaks.record(reading.key)
                                }
                            }
                        }
                        .id(z.id)
                    }

                    if done == total {
                        VStack(spacing: 12) {
                            Image(systemName: "checkmark.circle.fill").font(.system(size: 44)).foregroundStyle(.tint)
                                .symbolEffect(.bounce, value: done == total)
                            Text(model.s.allDone).font(.headline).multilineTextAlignment(.center)
                            Button {
                                withAnimation { remaining = [:] }
                                if let first = items.first { withAnimation { proxy.scrollTo(first.id, anchor: .top) } }
                            } label: { Label(model.s.restart, systemImage: "arrow.counterclockwise") }
                                .prominentGlassButton()
                        }
                        .frame(maxWidth: .infinity)
                        .padding(24)
                        .glassCard()
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 40)
                .frame(maxWidth: 760)
                .frame(maxWidth: .infinity)
            }
            .background(Backdrop())
            .navigationTitle(title)
            .navigationBarTitleDisplayModeLarge()
            .onAppear {
                if let focus { proxy.scrollTo(focus, anchor: .top) }
                if model.ciScreen == "celebration" {
                    for z in items { remaining[z.id] = 0 }
                    celebration = 3
                }
            }
            .sheet(isPresented: Binding(get: { celebration != nil }, set: { if !$0 { celebration = nil } })) {
                CelebrationView(reading: reading, streak: celebration ?? 1, count: items.count, repetitions: total) { celebration = nil }
                    .presentationDetents([.large])
                    .presentationCornerRadius(34)
            }
        }
        .sensoryFeedback(.success, trigger: done == total && total > 0) { _, new in new && model.haptics }
    }

    private func left(_ z: Zikr) -> Int { remaining[z.id] ?? z.repeatCount }
}

/// One dhikr: Arabic in Amiri, optional transliteration and translation, and a glass counter.
struct ZikrCard: View {
    @Environment(AppModel.self) private var model
    let zikr: Zikr
    let index: Int?
    let total: Int?
    let remaining: Int
    let chapterTitle: String
    var label: String? = nil
    /// Counts one repetition, or all of them at once (`true`) for azkar said 100 times.
    let onCount: (Bool) -> Void

    var body: some View {
        let s = model.s
        let lang = model.lang
        let done = remaining == 0
        let fav = model.favorites.contains(zikr.id)

        VStack(spacing: 12) {
            HStack(spacing: 4) {
                if let index, let total {
                    Text("\(lang.num(index)) / \(lang.num(total))")
                        .font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                        .padding(.horizontal, 9).padding(.vertical, 3)
                        .background(Color.softFill, in: Capsule())
                }
                if let label { Text(label).font(.caption).foregroundStyle(.secondary).lineLimit(1) }
                Spacer()
                IconAction(symbol: "doc.on.doc", label: s.copy) {
                    copyToPasteboard(shareText)
                    model.toast = s.copied
                }
                IconAction(symbol: "square.and.arrow.up", label: s.share) {
                    model.shareTarget = ShareTarget(zikr: zikr, title: chapterTitle)
                }
                IconAction(symbol: fav ? "heart.fill" : "heart", label: s.tabFavorites, tint: fav ? .pink : .accentColor) {
                    withAnimation(.bouncy) { model.toggleFavorite(zikr.id) }
                }
                .sensoryFeedback(.selection, trigger: fav) { _, _ in model.haptics }
            }

            VStack(spacing: 10) {
                Text(zikr.ar)
                    .font(.amiri(23 * model.textScale))
                    .lineSpacing(10 * model.textScale)
                    .multilineTextAlignment(.center)
                    .environment(\.layoutDirection, .rightToLeft)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 4)
                if lang != .ar, model.showTranslit, !zikr.tr.isEmpty {
                    Text(zikr.tr)
                        .font(.system(size: 14 * model.textScale).italic())
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                let meaning = zikr.meaning(lang)
                if model.showMeaning, !meaning.isEmpty {
                    Text(meaning)
                        .font(.system(size: 16 * model.textScale))
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .opacity(done ? 0.5 : 1)

            if let ref = zikr.ref {
                Label(ref(lang), systemImage: "book.closed")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .multilineTextAlignment(.center)
                    .opacity(done ? 0.5 : 1)
            }

            if zikr.repeatCount >= 100 && !done {
                Button { onCount(true) } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark").font(.subheadline.weight(.bold))
                        Text(s.completed).font(.headline)
                        Text("· " + s.repeatTimes(zikr.repeatCount)).font(.subheadline).opacity(0.8)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 4)
                }
                .prominentGlassButton()
                .tint(Theme.accent)
                .sensoryFeedback(.success, trigger: done) { _, _ in model.haptics }
            } else {
            Button(action: count) {
                HStack(spacing: 10) {
                    ZStack {
                        Circle().stroke(.quaternary, lineWidth: 3)
                        Circle()
                            .trim(from: 0, to: CGFloat(zikr.repeatCount - remaining) / CGFloat(zikr.repeatCount))
                            .stroke(Theme.accent, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                            .rotationEffect(.degrees(-90))
                        if done {
                            Image(systemName: "checkmark").font(.footnote.bold()).foregroundStyle(.tint)
                        } else {
                            Text(lang.num(remaining)).font(.footnote.bold()).contentTransition(.numericText(countsDown: true))
                        }
                    }
                    .frame(width: 32, height: 32)
                    Text(done ? s.completed : s.repeatTimes(zikr.repeatCount))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.tint)
                }
                .padding(.leading, 4).padding(.trailing, 10).padding(.vertical, 2)
            }
            .glassButton()
            .disabled(done)
            .sensoryFeedback(.increase, trigger: remaining) { _, _ in model.haptics }
            }
        }
        .padding(18)
        .glassCard()
        .contentShape(RoundedRectangle(cornerRadius: 22))
        .onTapGesture(perform: count)
    }

    private func count() { if remaining > 0 && zikr.repeatCount < 100 { onCount(false) } }

    private var shareText: String {
        var t = zikr.ar
        let m = zikr.meaning(model.lang)
        if !m.isEmpty { t += "\n\n" + m }
        return t + "\n\n— \(chapterTitle) · Islamic Azkar | أذكار إسلامية"
    }
}

private struct IconAction: View {
    let symbol: String
    let label: String
    var tint: Color = .accentColor
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 16, weight: .medium)).foregroundStyle(tint)
                .frame(width: 36, height: 36).contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

func copyToPasteboard(_ text: String) {
    #if os(iOS)
    UIPasteboard.general.string = text
    #else
    NSPasteboard.general.clearContents()
    NSPasteboard.general.setString(text, forType: .string)
    #endif
}

struct FavoritesView: View {
    @Environment(AppModel.self) private var model
    @State private var remaining: [Int: Int] = [:]

    var body: some View {
        let data = AzkarData.shared
        let favs = model.favorites.compactMap { data.zikrById[$0] }.sorted { $0.id % 10000 < $1.id % 10000 }
        ScrollView {
            LazyVStack(spacing: 14) {
                if favs.isEmpty {
                    ContentUnavailableView(model.s.noFavorites, systemImage: "heart", description: Text(model.s.noFavoritesHint))
                        .padding(.top, 60)
                }
                ForEach(favs) { z in
                    let title = chapterTitle(z)
                    ZikrCard(zikr: z, index: nil, total: nil, remaining: remaining[z.id] ?? z.repeatCount,
                             chapterTitle: title, label: title) { all in
                        remaining[z.id] = all ? 0 : (remaining[z.id] ?? z.repeatCount) - 1
                    }
                }
            }
            .padding(.horizontal, 16).padding(.bottom, 30)
            .frame(maxWidth: 760).frame(maxWidth: .infinity)
        }
        .background(Backdrop())
        .navigationTitle(model.s.tabFavorites)
    }

    private func chapterTitle(_ z: Zikr) -> String {
        guard let r = AzkarData.shared.reading(ofZikr: z.id) else { return model.s.book }
        return AzkarData.shared.title(r, model.s, model.lang)
    }
}
