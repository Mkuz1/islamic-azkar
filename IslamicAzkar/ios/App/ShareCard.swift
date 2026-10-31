import SwiftUI

/// The four share-image looks (same as the Android app).
struct ShareStyle {
    let bg: Color
    let cardTop: Color
    let cardBottom: Color
    let pattern: Color?
    let frame: Color
    let doubleFrame: Bool
    let text: Color
    let text2: Color
    let accent: Color

    static let all: [ShareStyle] = [
        // Emerald — the Mushaf's opening page
        ShareStyle(bg: Brand.mushafNight, cardTop: Brand.mushafCardTop, cardBottom: Brand.mushafCardBottom,
                   pattern: .white.opacity(0.035), frame: Brand.mushafGold, doubleFrame: true,
                   text: .white, text2: .white.opacity(0.7), accent: Brand.mushafGold),
        // Parchment
        ShareStyle(bg: Color(red: 0.94, green: 0.90, blue: 0.82), cardTop: Color(red: 0.98, green: 0.97, blue: 0.93), cardBottom: Color(red: 0.96, green: 0.94, blue: 0.87),
                   pattern: Color(red: 0.69, green: 0.54, blue: 0.24).opacity(0.08), frame: Color(red: 0.69, green: 0.54, blue: 0.24), doubleFrame: true,
                   text: Color(red: 0.16, green: 0.13, blue: 0.09), text2: Color(red: 0.16, green: 0.13, blue: 0.09).opacity(0.7), accent: Brand.tint),
        // Night
        ShareStyle(bg: Color(red: 0.02, green: 0.02, blue: 0.02), cardTop: Color(red: 0.11, green: 0.11, blue: 0.12), cardBottom: Color(red: 0.08, green: 0.08, blue: 0.09),
                   pattern: .white.opacity(0.03), frame: Brand.mushafGold, doubleFrame: true,
                   text: .white, text2: Color(red: 0.92, green: 0.92, blue: 0.96).opacity(0.65), accent: Brand.mushafGold),
        // Light — plain Apple grouped style
        ShareStyle(bg: Color(red: 0.95, green: 0.95, blue: 0.97), cardTop: .white, cardBottom: .white,
                   pattern: nil, frame: Color(red: 0.24, green: 0.24, blue: 0.26).opacity(0.18), doubleFrame: false,
                   text: .black, text2: Color(red: 0.24, green: 0.24, blue: 0.26).opacity(0.6), accent: Brand.tint),
    ]
}

/// A 1080×1350 share image in the Mushaf's style: a double-framed card, calligraphic signature.
struct ShareCardView: View {
    let zikr: Zikr
    let title: String
    let style: ShareStyle
    let lang: Lang
    let withMeaning: Bool

    var body: some View {
        let meaning = withMeaning ? zikr.meaning(lang) : ""
        ZStack {
            style.bg
            if let p = style.pattern { GeometricPattern(color: p, step: 150) }

            VStack(spacing: 0) {
                Text(title)
                    .font(lang == .ar ? .amiri(42, bold: true) : .system(size: 32, weight: .semibold))
                    .foregroundStyle(style.text2)
                    .lineLimit(1).minimumScaleFactor(0.6)
                    .padding(.horizontal, 100).padding(.top, 76)
                ornament(half: 150).padding(.top, 18)

                VStack(spacing: 30) {
                    Text(zikr.ar)
                        .font(.amiri(92))
                        .lineSpacing(28)
                        .minimumScaleFactor(0.3)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(style.text)
                        .environment(\.layoutDirection, .rightToLeft)
                    if !meaning.isEmpty {
                        ornament(half: 80)
                        Text(meaning)
                            .font(.system(size: 30, design: .serif).italic())
                            .lineSpacing(8)
                            .minimumScaleFactor(0.6)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(style.text2)
                    }
                    if zikr.repeatCount > 1 {
                        Text("× \(zikr.repeatCount)")
                            .font(.system(size: 28, weight: .bold)).foregroundStyle(style.accent)
                            .padding(.horizontal, 26).padding(.vertical, 8)
                            .background(style.accent.opacity(0.15), in: Capsule())
                    }
                }
                .padding(70)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 60, style: .continuous)
                        .fill(LinearGradient(colors: [style.cardTop, style.cardBottom], startPoint: .top, endPoint: .bottom))
                )
                .overlay(RoundedRectangle(cornerRadius: 60, style: .continuous).strokeBorder(style.frame.opacity(style.doubleFrame ? 0.6 : 1), lineWidth: 2.5))
                .overlay {
                    if style.doubleFrame {
                        RoundedRectangle(cornerRadius: 48, style: .continuous).strokeBorder(style.frame.opacity(0.24), lineWidth: 1.5).padding(14)
                    }
                }
                .padding(.horizontal, 70)
                .padding(.top, 34)

                HStack(spacing: 30) {
                    Text("ISLAMIC AZKAR").font(.system(size: 26, weight: .bold, design: .serif)).tracking(9).foregroundStyle(style.text2)
                    EightPointStar(inner: 0.72).fill(style.frame).frame(width: 20, height: 20)
                    Text("أذكار إسلامية").font(.ruqaa(60)).foregroundStyle(style.accent)
                }
                .environment(\.layoutDirection, .leftToRight)
                .padding(.top, 44)
                Text("حصن المسلم  ·  Hisn Al Muslim").font(.system(size: 22, weight: .medium)).foregroundStyle(style.text2.opacity(0.8))
                    .padding(.top, 10).padding(.bottom, 64)
            }
        }
        .frame(width: 1080, height: 1350)
        .clipped()
    }

    private func ornament(half: CGFloat) -> some View {
        HStack(spacing: 14) {
            LinearGradient(colors: [.clear, style.frame], startPoint: .leading, endPoint: .trailing).frame(width: half, height: 2)
            EightPointStar(inner: 0.72).fill(style.frame).frame(width: 18, height: 18)
            LinearGradient(colors: [style.frame, .clear], startPoint: .leading, endPoint: .trailing).frame(width: half, height: 2)
        }
    }
}

/// Glass sheet: preview the image, pick a style, share.
struct ShareSheetView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let target: ShareTarget

    @State private var style = 0
    @State private var withMeaning = true
    @State private var image: Image?

    var body: some View {
        let s = model.s
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    Group {
                        if let image {
                            image.resizable().scaledToFit()
                        } else {
                            ProgressView().frame(maxWidth: .infinity, minHeight: 300)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                    .shadow(color: .black.opacity(0.18), radius: 18, y: 8)
                    .frame(maxWidth: 320)

                    HStack(spacing: 16) {
                        ForEach(ShareStyle.all.indices, id: \.self) { i in
                            Button { style = i } label: {
                                VStack(spacing: 6) {
                                    Circle()
                                        .fill(LinearGradient(colors: [ShareStyle.all[i].bg, ShareStyle.all[i].cardTop],
                                                             startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .frame(width: 46, height: 46)
                                        .overlay(Circle().strokeBorder(i == style ? Color.accentColor : Color.secondary.opacity(0.3), lineWidth: i == style ? 3 : 1))
                                    Text(s.styleNames[i]).font(.caption2).foregroundStyle(i == style ? Color.accentColor : .secondary)
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    if model.lang != .ar {
                        Toggle(s.includeMeaning, isOn: $withMeaning).padding(.horizontal, 6)
                    }

                    if let image {
                        ShareLink(item: image, preview: SharePreview(target.title, image: image)) {
                            Label(s.shareNow, systemImage: "square.and.arrow.up").font(.headline).frame(maxWidth: 240).padding(.vertical, 4)
                        }
                        .prominentGlassButton()
                    }
                }
                .padding(20)
            }
            .navigationTitle(s.shareImage)
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(s.done) { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
        .task(id: "\(style)-\(withMeaning)") { render() }
    }

    @MainActor
    private func render() {
        let card = ShareCardView(zikr: target.zikr, title: target.title, style: ShareStyle.all[style],
                                 lang: model.lang, withMeaning: withMeaning)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 1
        if let cg = renderer.cgImage {
            image = Image(decorative: cg, scale: 1)
        }
    }
}
