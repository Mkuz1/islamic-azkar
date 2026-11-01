import SwiftUI

/// Texts for the guide and the hadith-collection chapters (mirrors the Android app).
struct GuideTexts {
    let guide, guideCta, wordsToSay, fromSunnah, fromQuran: String

    static func of(_ lang: Lang) -> GuideTexts {
        switch lang {
        case .ar: return GuideTexts(guide: "الدليل", guideCta: "خطوات العمرة من الإحرام إلى التقصير", wordsToSay: "أذكار هذه الخطوة", fromSunnah: "من كتب السنة", fromQuran: "من القرآن الكريم")
        case .en: return GuideTexts(guide: "Guide", guideCta: "Every step from iḥrām to the haircut", wordsToSay: "Words to say", fromSunnah: "From the hadith collections", fromQuran: "From the Holy Quran")
        case .fr: return GuideTexts(guide: "Guide", guideCta: "Chaque étape, de l'iḥrām à la coupe des cheveux", wordsToSay: "Paroles à dire", fromSunnah: "Des recueils de hadiths", fromQuran: "Du Saint Coran")
        }
    }
}

/// Row opening the ʿUmrah guide (Home and the Hajj & Umrah category).
struct GuideLinkRow: View {
    @Environment(AppModel.self) private var model
    let guide: Guide

    var body: some View {
        NavigationLink(value: Route.guide(guide.key)) {
            GlassRow(divider: false) {
                SymbolSquare(symbol: "location.north.circle.fill", color: .brown, size: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(guide.title(model.lang)).foregroundStyle(.primary)
                    Text(GuideTexts.of(model.lang).guideCta).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer()
                Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
            }
        }
        .buttonStyle(.plain)
    }
}

/// The step-by-step guide in the reader's card system: a numbered step card (title, explanation,
/// sources) followed by its duas as full `ZikrCard`s with counters, favourite, copy and share.
struct GuideView: View {
    @Environment(AppModel.self) private var model
    let key: String
    @State private var remaining: [Int: Int] = [:]

    private func left(_ z: Zikr) -> Int { remaining[z.id] ?? z.repeatCount }

    var body: some View {
        let data = AzkarData.shared
        let lang = model.lang
        ScrollView {
            if let guide = data.guides.first(where: { $0.key == key }) {
                let steps = guide.steps.map { st in (st, st.zikr.compactMap { data.zikrById[$0] }) }
                let all = steps.flatMap { $0.1 }
                let total = all.reduce(0) { $0 + $1.repeatCount }
                let done = all.reduce(0) { $0 + ($1.repeatCount - left($1)) }
                let numberOf = Dictionary(uniqueKeysWithValues: all.enumerated().map { ($1.id, $0 + 1) })
                LazyVStack(spacing: 14) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text(guide.intro(lang)).font(.subheadline).foregroundStyle(.secondary)
                        ProgressView(value: Double(done), total: Double(max(total, 1)))
                    }
                    .padding(.horizontal, 4)
                    ForEach(Array(steps.enumerated()), id: \.offset) { i, pair in
                        StepCard(number: i + 1, count: steps.count, step: pair.0)
                        ForEach(pair.1) { z in
                            ZikrCard(zikr: z, index: numberOf[z.id], total: all.count, remaining: left(z), chapterTitle: guide.title(lang)) { everything in
                                withAnimation(.snappy) { remaining[z.id] = everything ? 0 : left(z) - 1 }
                            }
                        }
                        Spacer().frame(height: 8)
                    }
                    Text(guide.note(lang)).font(.footnote).foregroundStyle(.tertiary).multilineTextAlignment(.center).padding(.horizontal, 12).padding(.bottom, 40)
                }
                .padding(.horizontal, 16)
                .frame(maxWidth: 900).frame(maxWidth: .infinity)
                .navigationTitle(guide.title(lang))
            }
        }
        .background(Backdrop())
        .navigationBarTitleDisplayModeLarge()
    }
}

/// A numbered step: gold number badge, "3 / 9", title, explanation and hadith sources.
private struct StepCard: View {
    @Environment(AppModel.self) private var model
    let number: Int
    let count: Int
    let step: Guide.Step

    var body: some View {
        let lang = model.lang
        let gold = Color(red: 0.72, green: 0.57, blue: 0.25)
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Text(lang.num(number)).font(.headline.bold()).foregroundStyle(gold)
                    .frame(width: 36, height: 36).background(gold.opacity(0.2), in: Circle())
                Text("\(lang.num(number)) / \(lang.num(count))")
                    .font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    .padding(.horizontal, 9).padding(.vertical, 3)
                    .background(Color.softFill, in: Capsule())
            }
            Text(step.title(lang).replacingOccurrences(of: #"^[0-9٠-٩]+\.\s*"#, with: "", options: .regularExpression))
                .font(.title3.weight(.semibold))
            Text(step.text(lang)).font(.body).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
            if !step.ref(lang).isEmpty {
                Label(step.ref(lang), systemImage: "book.closed").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .leading) {
            Capsule().fill(gold).frame(width: 4).padding(.vertical, 18)
        }
        .glassCard(cornerRadius: 24)
    }
}
