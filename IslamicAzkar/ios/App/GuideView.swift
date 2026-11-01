import SwiftUI

/// Texts for the guide and the hadith-collection chapters (mirrors the Android app).
struct GuideTexts {
    let guide, guideCta, wordsToSay, fromSunnah: String

    static func of(_ lang: Lang) -> GuideTexts {
        switch lang {
        case .ar: return GuideTexts(guide: "الدليل", guideCta: "خطوات العمرة من الإحرام إلى التقصير", wordsToSay: "أذكار هذه الخطوة", fromSunnah: "من كتب السنة")
        case .en: return GuideTexts(guide: "Guide", guideCta: "Every step from iḥrām to the haircut", wordsToSay: "Words to say", fromSunnah: "From the hadith collections")
        case .fr: return GuideTexts(guide: "Guide", guideCta: "Chaque étape, de l'iḥrām à la coupe des cheveux", wordsToSay: "Paroles à dire", fromSunnah: "Des recueils de hadiths")
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

/// The step-by-step guide: explanation, the duas to say at each step, and the hadith sources.
struct GuideView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.inSplitView) private var split
    let key: String

    var body: some View {
        let data = AzkarData.shared
        let lang = model.lang
        let t = GuideTexts.of(lang)
        ScrollView {
            if let guide = data.guides.first(where: { $0.key == key }) {
                LazyVStack(spacing: 14) {
                    Text(guide.intro(lang)).font(.subheadline).foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 4)
                    ForEach(guide.steps, id: \.title.en) { step in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(step.title(lang)).font(.title3.weight(.semibold))
                            Text(step.text(lang)).font(.body).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                            let duas = step.zikr.compactMap { data.zikrById[$0] }
                            if !duas.isEmpty {
                                Text(t.wordsToSay).font(.footnote.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary).padding(.top, 4)
                                VStack(spacing: 0) {
                                    ForEach(Array(duas.enumerated()), id: \.element.id) { i, z in
                                        Button {
                                            if let r = data.reading(ofZikr: z.id) { model.show(r, focus: z.id, split: split) }
                                        } label: {
                                            VStack(spacing: 4) {
                                                Text(z.ar).font(.amiri(19)).multilineTextAlignment(.center).lineLimit(3)
                                                    .environment(\.layoutDirection, .rightToLeft).frame(maxWidth: .infinity)
                                                let m = z.meaning(lang)
                                                if !m.isEmpty { Text(m).font(.caption).foregroundStyle(.secondary).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading) }
                                            }
                                            .padding(12)
                                            .contentShape(Rectangle())
                                        }
                                        .buttonStyle(.plain)
                                        if i < duas.count - 1 { Divider() }
                                    }
                                }
                                .background(Color.softFill, in: RoundedRectangle(cornerRadius: 14))
                            }
                            if !step.ref(lang).isEmpty {
                                Label(step.ref(lang), systemImage: "book.closed").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
                            }
                        }
                        .padding(18)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .glassCard(cornerRadius: 24)
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
