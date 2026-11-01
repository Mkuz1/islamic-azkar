import SwiftUI

struct TasbihView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.verticalSizeClass) private var vSize
    private let targets = [33, 99, 100, 0]

    var body: some View {
        let s = model.s
        let lang = model.lang
        let target = model.tasbihTarget
        let inRound = target > 0 ? model.tasbihCount % target : model.tasbihCount
        let rounds = target > 0 ? model.tasbihCount / target : 0

        // Landscape iPhone: bead beside the controls instead of above them.
        let layout = vSize == .compact ? AnyLayout(HStackLayout(spacing: 28)) : AnyLayout(VStackLayout(spacing: 22))

        ScrollView {
            layout {
                bead(inRound: inRound, target: target, rounds: rounds)
                    .frame(maxWidth: 320)

                VStack(spacing: 18) {
                    phrases
                    let meaning = TasbihPhrases.meaning(model.tasbihPhrase, lang)
                    if !meaning.isEmpty { Text(meaning).foregroundStyle(.secondary) }
                    Text(s.tapAnywhere).font(.caption).foregroundStyle(.tertiary)

                    HStack(spacing: 12) {
                        Picker(s.target, selection: Binding(get: { model.tasbihTarget }, set: { model.tasbihTarget = $0; model.tasbihCount = 0 })) {
                            ForEach(targets, id: \.self) { t in Text(t == 0 ? "∞" : lang.num(t)).tag(t) }
                        }
                        .pickerStyle(.segmented)
                        Button { model.tasbihCount = 0 } label: {
                            Image(systemName: "arrow.counterclockwise").font(.headline).frame(width: 30, height: 30)
                        }
                        .glassButton()
                        .accessibilityLabel(s.reset)
                    }
                }
                .frame(maxWidth: 520)
            }
            .padding(20)
            .frame(maxWidth: .infinity)
        }
        .background(Backdrop())
        .navigationTitle(s.tasbih)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Text("\(s.total): \(lang.num(model.tasbihTotal))").font(.footnote.monospacedDigit()).foregroundStyle(.secondary)
            }
        }
    }

    private func bead(inRound: Int, target: Int, rounds: Int) -> some View {
        let lang = model.lang
        return Button {
            model.tasbihCount += 1
            model.tasbihTotal += 1
            TasbihHistory.add()
        } label: {
            ZStack {
                Circle().fill(.clear).glassCard(cornerRadius: 400)
                Circle()
                    .stroke(Color.primary.opacity(0.08), lineWidth: 10)
                    .padding(10)
                Circle()
                    .trim(from: 0, to: target > 0 ? CGFloat(inRound) / CGFloat(target) : 0)
                    .stroke(LinearGradient(colors: [Theme.accent, Theme.gold], startPoint: .top, endPoint: .bottom), style: StrokeStyle(lineWidth: 10, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(10)
                VStack(spacing: 4) {
                    Text(TasbihPhrases.ar[model.tasbihPhrase]).font(.amiri(24))
                    Text(lang.num(inRound))
                        .font(.system(size: 72, weight: .semibold, design: .rounded))
                        .contentTransition(.numericText())
                    Text(target > 0 ? "/ \(lang.num(target))" + (rounds > 0 ? "  ·  ×\(lang.num(rounds))" : "") : "∞")
                        .font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .aspectRatio(1, contentMode: .fit)
        }
        .buttonStyle(.plain)
        .animation(.snappy, value: model.tasbihCount)
        .sensoryFeedback(trigger: model.tasbihCount) { _, new in
            guard model.haptics else { return nil }
            return target > 0 && new > 0 && new % target == 0 ? .success : .impact(weight: .light)
        }
    }

    private var phrases: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(TasbihPhrases.ar.indices, id: \.self) { i in
                    let selected = i == model.tasbihPhrase
                    Button {
                        model.tasbihPhrase = i
                        model.tasbihCount = 0
                    } label: {
                        Text(TasbihPhrases.ar[i]).font(.amiri(17))
                            .foregroundStyle(selected ? Theme.onAccent : Color.primary)
                            .padding(.horizontal, 14).padding(.vertical, 5)
                    }
                    .buttonStyle(.plain)
                    .background(selected ? Theme.accent : Color.primary.opacity(0.07), in: Capsule())
                }
            }
            .padding(.horizontal, 2).padding(.vertical, 4)
        }
    }
}
