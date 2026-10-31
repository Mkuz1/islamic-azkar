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

    var body: some View {
        let data = AzkarData.shared
        let items = data.items(reading)
        let title = data.title(reading, model.s, model.lang)
        let subtitle: String = {
            if case .chapter(let id) = reading, model.lang != .ar { return data.chapterById[id]?.ar ?? model.s.book }
            return model.s.book
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
                        ZikrCard(zikr: z, index: i + 1, total: items.count, remaining: left(z), chapterTitle: title) {
                            let r = left(z) - 1
                            withAnimation(.snappy) { remaining[z.id] = r }
                            if r == 0, i + 1 < items.count {
                                Task {
                                    try? await Task.sleep(for: .milliseconds(300))
                                    withAnimation(.smooth) { proxy.scrollTo(items[i + 1].id, anchor: .top) }
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
                        .card()
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 40)
                .frame(maxWidth: 760)
                .frame(maxWidth: .infinity)
            }
            .background(Color.groupedBackground)
            .navigationTitle(title)
            .onAppear {
                if let focus { proxy.scrollTo(focus, anchor: .top) }
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
    let onCount: () -> Void

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

            Button(action: count) {
                HStack(spacing: 10) {
                    ZStack {
                        Circle().stroke(.quaternary, lineWidth: 3)
                        Circle()
                            .trim(from: 0, to: CGFloat(zikr.repeatCount - remaining) / CGFloat(zikr.repeatCount))
                            .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 3, lineCap: .round))
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
        .padding(18)
        .card()
        .contentShape(RoundedRectangle(cornerRadius: 22))
        .onTapGesture(perform: count)
    }

    private func count() { if remaining > 0 { onCount() } }

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
                             chapterTitle: title, label: title) {
                        remaining[z.id] = (remaining[z.id] ?? z.repeatCount) - 1
                    }
                }
            }
            .padding(.horizontal, 16).padding(.bottom, 30)
            .frame(maxWidth: 760).frame(maxWidth: .infinity)
        }
        .background(Color.groupedBackground)
        .navigationTitle(model.s.tabFavorites)
    }

    private func chapterTitle(_ z: Zikr) -> String {
        guard let r = AzkarData.shared.reading(ofZikr: z.id) else { return model.s.book }
        return AzkarData.shared.title(r, model.s, model.lang)
    }
}
