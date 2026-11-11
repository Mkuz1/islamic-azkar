import SwiftUI

/// The opening page — the Mushaf app's sadaqa jariyah card: dark green, gold calligraphy,
/// a geometric backdrop, and a card that adapts from iPhone to iPad.
struct DedicationView: View {
    @Environment(AppModel.self) private var model
    let onContinue: () -> Void
    var onOpen: (Reading) -> Void = { _ in }
    @State private var appeared = false
    /// Extra shrink so the whole card fits the screen's height (short iPhones, landscape).
    @State private var fit: CGFloat = 1

    private var gold: Color { Brand.mushafGold }

    var body: some View {
        let lang = model.lang
        let text = DedicationText.of(lang)
        let data = AzkarData.shared

        GeometryReader { geo in
            let cardWidth = min(geo.size.width - 32, lang == .ar ? 520 : 760)
            let kw = min(max(cardWidth / 560, 0.78), 1.12)
            let k = max(kw * fit, 0.5)
            let available = geo.size.height - 24

            ZStack {
                Brand.mushafNight.ignoresSafeArea()
                GeometricPattern().ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 0) {
                        Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
                            .font(.quran(24 * k)).foregroundStyle(gold)
                            .environment(\.layoutDirection, .rightToLeft)
                        Ornament(color: gold).padding(.vertical, 14 * k)

                        Text("صدقة جارية")
                            .font(.ruqaa(66 * k))
                            .foregroundStyle(LinearGradient(colors: [Brand.mushafGoldLight, Color(red: 0.75, green: 0.58, blue: 0.28)], startPoint: .top, endPoint: .bottom))
                            .shadow(color: .black.opacity(0.35), radius: 9, y: 3)
                            .padding(.top, 8 * k)
                        if let t = text.transliteration {
                            Text(t).font(.system(size: 13 * k, weight: .semibold, design: .serif)).tracking(6 * k)
                                .foregroundStyle(gold.opacity(0.85)).padding(.top, 4 * k)
                        }
                        // The Arabic dedication appears in every language…
                        Text(dedicationNamesAr)
                            .font(.system(size: 27 * k, weight: .bold))
                            .foregroundStyle(.white).multilineTextAlignment(.center)
                            .environment(\.layoutDirection, .rightToLeft)
                            .padding(.top, 18 * k)
                        // …with its translation underneath in English and French.
                        if let names = text.names {
                            Text(names)
                                .font(.system(size: 19 * k, weight: .semibold, design: .serif))
                                .foregroundStyle(.white.opacity(0.85)).multilineTextAlignment(.center)
                                .padding(.top, 4 * k)
                        }
                        Ornament(color: gold).padding(.vertical, 16 * k)

                        Text("اللَّهُمَّ اجْعَلْهُ فِي مِيزَانِ حَسَنَاتِهِمْ").font(.amiri(22 * k, bold: true)).foregroundStyle(gold)
                        if let m = text.duaMeaning {
                            Text(m).font(.system(size: 15 * k, weight: .semibold, design: .serif).italic())
                                .foregroundStyle(.white.opacity(0.82)).multilineTextAlignment(.center).padding(.top, 2)
                        }
                        Text("إِذَا مَاتَ الإِنْسَانُ انْقَطَعَ عَمَلُهُ إِلَّا مِنْ ثَلَاثٍ: صَدَقَةٍ جَارِيَةٍ… — رواه مسلم")
                            .font(.amiri(15 * k)).foregroundStyle(.white.opacity(0.62)).multilineTextAlignment(.center)
                            .padding(.top, 16 * k)
                        if let h = text.hadithMeaning {
                            Text(h).font(.system(size: 12 * k, design: .serif)).foregroundStyle(.white.opacity(0.5)).multilineTextAlignment(.center)
                        }

                        @Bindable var m = model
                        Picker("", selection: $m.lang) {
                            Text("English").tag(Lang.en)
                            Text("Français").tag(Lang.fr)
                            Text("العربية").tag(Lang.ar)
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                        .environment(\.colorScheme, .dark)   // white labels on the dark green
                        .frame(maxWidth: 260)
                        .environment(\.layoutDirection, .leftToRight)
                        .padding(.top, 24 * k)

                        Button(action: onContinue) {
                            Text(text.begin).font(.headline).foregroundStyle(Color(red: 0.08, green: 0.16, blue: 0.12))
                                .padding(.horizontal, 28).padding(.vertical, 4)
                        }
                        .prominentGlassButton()
                        .tint(gold)
                        .padding(.top, 18 * k)

                        TimelineView(.everyMinute) { ctx in
                            let recommended = Reading.recommended(at: ctx.date)
                            Button {
                                onOpen(recommended)
                            } label: {
                                Text("\(data.title(recommended, model.s, lang)) · \(model.s.azkar(data.items(recommended).count))")
                                    .font(.footnote).foregroundStyle(.white.opacity(0.72))
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.top, 12 * k)
                    }
                    .padding(.horizontal, 24 * min(k, 1))
                    .padding(.vertical, 30 * k)
                    .frame(width: cardWidth)
                    .background(
                        RoundedRectangle(cornerRadius: 30, style: .continuous)
                            .fill(LinearGradient(colors: [Brand.mushafCardTop, Brand.mushafCardBottom], startPoint: .top, endPoint: .bottom))
                    )
                    .overlay(RoundedRectangle(cornerRadius: 30, style: .continuous).strokeBorder(gold.opacity(0.55), lineWidth: 1))
                    .overlay(RoundedRectangle(cornerRadius: 25, style: .continuous).strokeBorder(gold.opacity(0.22), lineWidth: 0.6).padding(6))
                    // Measure the card; if it is taller than the screen, shrink everything to fit.
                    .background(GeometryReader { card in
                        Color.clear
                            .onAppear { shrink(card.size.height, available: available, kw: kw) }
                            .onChange(of: card.size.height) { _, h in shrink(h, available: available, kw: kw) }
                    })
                    .frame(minHeight: geo.size.height - 24)
                    .frame(maxWidth: .infinity)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
            .opacity(appeared ? 1 : 0)
            .onChange(of: geo.size) { _, _ in fit = 1 }   // rotation, iPad split view: measure again
            .onChange(of: lang) { _, _ in fit = 1 }
        }
        .preferredColorScheme(.dark)
        .onAppear { withAnimation(.easeOut(duration: 0.9)) { appeared = true } }
    }

    private func shrink(_ height: CGFloat, available: CGFloat, kw: CGFloat) {
        // Ignore the first layout passes, before the screen size is known.
        guard available > 320, height > available + 1, kw * fit > 0.5 else { return }
        fit = max(fit * (available / height) * 0.98, 0.5 / kw)
    }
}

private struct DedicationText {
    let transliteration: String?
    let names: String?
    let duaMeaning: String?
    let hadithMeaning: String?
    let begin: String

    static func of(_ lang: Lang) -> DedicationText {
        switch lang {
        case .en:
            return DedicationText(transliteration: "SADAQA JARIYAH", names: dedicationTranslation(.en),
                                  duaMeaning: "O Allah, place it in the scale of their good deeds.",
                                  hadithMeaning: "“When a person dies, their deeds end except for three: an ongoing charity…” — Muslim",
                                  begin: "Begin")
        case .fr:
            return DedicationText(transliteration: "SADAQA JARIYA", names: dedicationTranslation(.fr),
                                  duaMeaning: "Ô Allah, place-la dans la balance de leurs bonnes actions.",
                                  hadithMeaning: "« Quand l'homme meurt, ses œuvres cessent sauf trois : une aumône continue… » — Muslim",
                                  begin: "Commencer")
        case .ar:
            return DedicationText(transliteration: nil, names: nil, duaMeaning: nil, hadithMeaning: nil, begin: "ابدأ")
        }
    }
}
