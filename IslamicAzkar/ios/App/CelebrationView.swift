import SwiftUI

/// Shown when a reading is finished: a gold-to-green check, the day streak, a prayer for the
/// reader and a short verse or hadith encouraging them to keep up the azkar.
struct CelebrationView: View {
    @Environment(AppModel.self) private var model
    let reading: Reading
    let streak: Int
    let count: Int
    let repetitions: Int
    let onClose: () -> Void

    @State private var appeared = false

    var body: some View {
        let s = model.s, m = model.m, lang = model.lang
        let title = AzkarData.shared.title(reading, s, lang)
        let encouragement = Library.shared.encouragements[(Calendar.current.ordinality(of: .day, in: .era, for: .now) ?? 0) % Library.shared.encouragements.count]

        ScrollView {
            VStack(spacing: 14) {
                ZStack {
                    ForEach(0..<8, id: \.self) { i in
                        EightPointStar(inner: 0.5).fill(Theme.gold.opacity(0.7))
                            .frame(width: i.isMultiple(of: 2) ? 14 : 9, height: i.isMultiple(of: 2) ? 14 : 9)
                            .offset(y: appeared ? -96 : -50)
                            .rotationEffect(.degrees(Double(i) * 45 + 20))
                            .opacity(appeared ? 1 : 0)
                    }
                    Circle()
                        .trim(from: 0, to: appeared ? 1 : 0)
                        .stroke(AngularGradient(colors: [Theme.gold, Theme.accent, Theme.gold], center: .center), style: StrokeStyle(lineWidth: 9, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .frame(width: 118, height: 118)
                    Circle().fill(Theme.accentSoft).frame(width: 100, height: 100)
                    Image(systemName: "checkmark").font(.system(size: 46, weight: .bold)).foregroundStyle(Theme.accent)
                        .scaleEffect(appeared ? 1 : 0.4).opacity(appeared ? 1 : 0)
                }
                .frame(height: 230)

                Text(m.congratsTitle).font(.system(size: 30, weight: .bold)).multilineTextAlignment(.center)
                Text(m.congratsDone(title)).font(.title3).foregroundStyle(.secondary).multilineTextAlignment(.center)

                HStack(spacing: 8) {
                    chip(s.azkar(count))
                    chip(m.repetitions(repetitions))
                }
                Label(m.streak(streak), systemImage: "sparkles")
                    .font(.headline).foregroundStyle(Theme.gold)
                    .padding(.horizontal, 18).padding(.vertical, 9)
                    .background(Theme.goldSoft, in: Capsule())

                Ornament().padding(.vertical, 8)

                Text(m.prayerLabel).font(.subheadline).foregroundStyle(.secondary)
                Text(completionPrayer)
                    .font(.amiri(26, bold: true)).foregroundStyle(Theme.accent)
                    .multilineTextAlignment(.center).lineSpacing(6)
                    .environment(\.layoutDirection, .rightToLeft)
                if !m.prayerMeaning.isEmpty {
                    Text(m.prayerMeaning).font(.callout).italic().foregroundStyle(.secondary).multilineTextAlignment(.center)
                }

                VStack(spacing: 6) {
                    Text(encouragement.ar).font(.amiri(22)).multilineTextAlignment(.center)
                        .environment(\.layoutDirection, .rightToLeft)
                    let meaning = encouragement.meaning(lang)
                    if !meaning.isEmpty { Text(meaning).font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center) }
                    Text(encouragement.source(lang)).font(.caption).foregroundStyle(.tertiary)
                }
                .padding(18)
                .frame(maxWidth: .infinity)
                .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.top, 6)

                if reading == .morning || reading == .evening {
                    Text(reading == .morning ? m.seeYouEvening : m.seeYouMorning)
                        .font(.callout).foregroundStyle(.secondary).padding(.top, 4)
                }

                Button(action: onClose) {
                    Text(m.alhamdulillah).font(.title3.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 8)
                }
                .prominentGlassButton()
                .tint(Theme.accent)
                .padding(.top, 8)
            }
            .padding(.horizontal, 24).padding(.vertical, 20)
            .frame(maxWidth: 560).frame(maxWidth: .infinity)
        }
        .background(Backdrop())
        .sensoryFeedback(.success, trigger: appeared) { _, new in new && model.haptics }
        .onAppear { withAnimation(.spring(response: 0.9, dampingFraction: 0.7).delay(0.15)) { appeared = true } }
    }

    private func chip(_ text: String) -> some View {
        Text(text).font(.subheadline).foregroundStyle(.secondary)
            .padding(.horizontal, 14).padding(.vertical, 7)
            .background(Color.primary.opacity(0.06), in: Capsule())
    }
}

/// Thin gold rule with a small rosette.
struct Ornament: View {
    var color: Color = Theme.gold
    var body: some View {
        HStack(spacing: 10) {
            LinearGradient(colors: [.clear, color.opacity(0.8)], startPoint: .leading, endPoint: .trailing).frame(width: 110, height: 1)
            EightPointStar(inner: 0.72).fill(color).frame(width: 10, height: 10)
            LinearGradient(colors: [color.opacity(0.8), .clear], startPoint: .leading, endPoint: .trailing).frame(width: 110, height: 1)
        }
    }
}
