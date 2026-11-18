import SwiftUI

/// One dhikr at a time, large enough to read from the sofa. Pressing the remote (or Play/Pause)
/// counts; when a dhikr reaches its count the next one comes; swiping moves back and forth.
struct TVReaderView: View {
    @Environment(TVModel.self) private var model
    let reading: Reading
    @State private var index = 0
    @State private var counts: [Int] = []
    @State private var done = false
    @FocusState private var focused: Bool

    private var items: [Zikr] { AzkarData.shared.items(reading) }

    var body: some View {
        ZStack {
            TVBackdrop()
            if done {
                TVDoneView(reading: reading) { restart() }
            } else if let z = items[safe: index] {
                card(z)
            }
        }
        .onAppear {
            if counts.count != items.count { counts = Array(repeating: 0, count: items.count) }
            if UserDefaults.standard.string(forKey: "ciScreen") == "done" { finish() }
        }
    }

    private func card(_ z: Zikr) -> some View {
        let lang = model.lang, t = model.t
        let c = counts[safe: index] ?? 0
        return Button(action: count) {
            VStack(spacing: 26) {
                HStack {
                    Text(AzkarData.shared.title(reading, model.s, lang)).font(.headline).foregroundStyle(Brand.mushafGold)
                    Spacer()
                    Text("\(lang.num(index + 1)) / \(lang.num(items.count))").font(.headline.monospacedDigit()).foregroundStyle(.secondary)
                }
                Text(z.ar)
                    .font(.amiri(50))
                    .lineSpacing(16)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.4)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                    .layoutPriority(1)
                if lang != .ar, !z.tr.isEmpty {
                    Text(z.tr).font(.callout).italic().foregroundStyle(.white.opacity(0.7))
                        .multilineTextAlignment(.center).lineLimit(3).minimumScaleFactor(0.6)
                }
                if lang != .ar, !z.meaning(lang).isEmpty {
                    Text(z.meaning(lang)).font(.callout).foregroundStyle(.white.opacity(0.9))
                        .multilineTextAlignment(.center).lineLimit(4).minimumScaleFactor(0.6)
                }
                HStack(spacing: 24) {
                    ProgressView(value: Double(c), total: Double(z.repeatCount)).tint(Brand.mushafGold)
                    Text("\(lang.num(c)) / \(lang.num(z.repeatCount))").font(.title3.monospacedDigit().weight(.semibold))
                }
                Text("\(t.pressToCount) · \(t.swipeHint)").font(.caption).foregroundStyle(.secondary)
            }
            .padding(60)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .buttonStyle(.card)
        .focused($focused)
        .onMoveCommand { dir in
            // Swiping towards the reading direction goes to the next dhikr.
            switch (dir, lang.isRTL) {
            case (.right, false), (.left, true): go(1)
            case (.left, false), (.right, true): go(-1)
            default: break
            }
        }
        .onPlayPauseCommand(perform: count)
        .padding(.horizontal, 90).padding(.vertical, 50)
        .onAppear { focused = true }
    }

    private func count() {
        guard index < counts.count, let z = items[safe: index] else { return }
        counts[index] = min(counts[index] + 1, z.repeatCount)
        guard counts[index] >= z.repeatCount else { return }
        if let open = firstOpen {
            // On to the next dhikr, or back to one that was skipped.
            let target = (index + 1..<items.count).first { counts[$0] < items[$0].repeatCount } ?? open
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { withAnimation(.easeInOut(duration: 0.25)) { index = target } }
        } else {
            finish()   // every dhikr counted in full
        }
    }

    /// The first dhikr not yet counted in full.
    private var firstOpen: Int? { items.indices.first { counts[$0] < items[$0].repeatCount } }

    private func go(_ step: Int) {
        let n = index + step
        guard n >= 0, n < items.count else { return }
        withAnimation(.easeInOut(duration: 0.25)) { index = n }
    }

    private func finish() {
        AzkarHistory.markReading(reading.key)
        model.historyVersion += 1
        withAnimation { done = true }
    }

    private func restart() {
        counts = Array(repeating: 0, count: items.count)
        index = 0
        withAnimation { done = false }
    }
}

/// The completion screen: a short prayer, the streak, and what to do next.
struct TVDoneView: View {
    @Environment(TVModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let reading: Reading
    let again: () -> Void

    var body: some View {
        let m = model.m, t = model.t, lang = model.lang
        let title = AzkarData.shared.title(reading, model.s, lang)
        VStack(spacing: 30) {
            Image(systemName: "checkmark.seal.fill").font(.system(size: 110)).foregroundStyle(Brand.mushafGold)
            Text(m.congratsTitle).font(.system(size: 58, weight: .bold))
            Text(m.congratsDone(title)).font(.title3).foregroundStyle(.white.opacity(0.85)).multilineTextAlignment(.center)
            Text(m.streak(AzkarHistory.streak(reading.key))).font(.headline).foregroundStyle(Brand.emeraldBright)
            Text(m.alhamdulillah).font(.amiri(40)).foregroundStyle(Brand.mushafGold)
            HStack(spacing: 40) {
                Button(t.readAgain, action: again)
                Button(t.backHome) { dismiss() }
            }
            .padding(.top, 20)
        }
        .padding(80)
    }
}
