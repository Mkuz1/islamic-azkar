import SwiftUI
#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// The Android app's palette: Apple's label hierarchy tinted with the Mushaf's green and gold,
/// over a soft backdrop that the glass panes refract.
enum Theme {
    static let accent = dynamic(light: 0x1E6B52, dark: 0x5DC79C)
    static let accentSoft = dynamic(light: 0x1E6B52, dark: 0x5DC79C, alphaLight: 0.10, alphaDark: 0.16)
    static let gold = dynamic(light: 0xB08A3E, dark: 0xD9B66C)
    static let goldSoft = dynamic(light: 0xD6B36A, dark: 0xD6B36A, alphaLight: 0.20, alphaDark: 0.18)
    static let onAccent = dynamic(light: 0xFFFFFF, dark: 0x062118)
    static let pane = dynamic(light: 0xFFFFFF, dark: 0x1C1C1E, alphaLight: 0.62, alphaDark: 0.42)
    static let wallTop = dynamic(light: 0xE9F0EC, dark: 0x07140F)
    static let wallBottom = dynamic(light: 0xF5F1E8, dark: 0x020504)
    static let glowGreen = dynamic(light: 0x1E6B52, dark: 0x204A3C, alphaLight: 0.30, alphaDark: 0.60)
    static let glowGold = dynamic(light: 0xD6B36A, dark: 0xD6B36A, alphaLight: 0.25, alphaDark: 0.15)
    static let pattern = dynamic(light: 0x1E6B52, dark: 0xFFFFFF, alphaLight: 0.06, alphaDark: 0.04)
    static let rim = dynamic(light: 0xFFFFFF, dark: 0xFFFFFF, alphaLight: 0.80, alphaDark: 0.14)

    static func dynamic(light: UInt32, dark: UInt32, alphaLight: Double = 1, alphaDark: Double = 1) -> Color {
        #if os(iOS)
        Color(uiColor: UIColor { t in
            let hex = t.userInterfaceStyle == .dark ? dark : light
            let a = t.userInterfaceStyle == .dark ? alphaDark : alphaLight
            return UIColor(red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
                           blue: CGFloat(hex & 0xFF) / 255, alpha: a)
        })
        #else
        // Follows the window's appearance (Light / Dark / Auto), like the iOS version.
        Color(nsColor: NSColor(name: nil) { appearance in
            let isDark = appearance.bestMatch(from: [.aqua, .darkAqua]) == .darkAqua
            let hex = isDark ? dark : light
            return NSColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
                           blue: CGFloat(hex & 0xFF) / 255, alpha: isDark ? alphaDark : alphaLight)
        })
        #endif
    }
}

/// The page backdrop: a mint-to-sand wash (deep green at night), two soft glows and the
/// Mushaf's star lattice.
struct Backdrop: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Theme.wallTop, Theme.wallBottom], startPoint: .top, endPoint: .bottom)
            RadialGradient(colors: [Theme.glowGreen, .clear], center: .topLeading, startRadius: 0, endRadius: 520)
            RadialGradient(colors: [Theme.glowGold, .clear], center: .bottomTrailing, startRadius: 0, endRadius: 560)
            GeometricPattern(color: Theme.pattern, step: 84)
        }
        .ignoresSafeArea()
    }
}

extension View {
    /// A content pane of Liquid Glass (iOS 26), tinted so text stays easy to read.
    @ViewBuilder
    func glassCard(cornerRadius: CGFloat = 26) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        #if os(macOS)
        // On the Mac a live glass effect behind every card (dozens in a reading) makes scrolling and
        // navigation lag: a tinted card with a hairline rim looks the same at a fraction of the cost.
        self.background(Theme.pane, in: shape)
            .overlay(shape.strokeBorder(Theme.rim, lineWidth: 0.7))
        #else
        if #available(iOS 26.0, *) {
            self.background(Theme.pane, in: shape)
                .glassEffect(.regular, in: shape)
        } else {
            self.background(.regularMaterial, in: shape)
                .overlay(shape.strokeBorder(Theme.rim, lineWidth: 0.7))
                .shadow(color: .black.opacity(0.06), radius: 12, y: 4)
        }
        #endif
    }

    /// A small glass capsule (chips, the language switch).
    @ViewBuilder
    func glassCapsule() -> some View {
        if #available(iOS 26.0, macOS 26.0, *) {
            self.glassEffect(.regular.interactive(), in: Capsule())
        } else {
            self.background(.regularMaterial, in: Capsule())
        }
    }

    /// Page chrome: backdrop behind, hidden system background.
    @ViewBuilder
    func pageBackdrop() -> some View {
        #if os(macOS)
        // macOS draws a Form as label/value columns across the whole window: use the grouped
        // (iOS Settings-like) style in a centred column instead.
        self.formStyle(.grouped).scrollContentBackground(.hidden)
            .frame(maxWidth: 720).frame(maxWidth: .infinity)
            .background(Backdrop())
        #else
        self.scrollContentBackground(.hidden).background(Backdrop())
        #endif
    }
}

/// Section header in the iOS grouped-list style.
struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text).font(.footnote.weight(.semibold)).textCase(.uppercase).foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 20).padding(.top, 18).padding(.bottom, 2)
    }
}

/// The language switch: ع · EN · FR on a glass capsule.
struct LanguagePill: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        HStack(spacing: 2) {
            ForEach([(Lang.ar, "ع"), (.en, "EN"), (.fr, "FR")], id: \.0) { lang, label in
                let on = model.lang == lang
                Button {
                    withAnimation(.snappy) { model.lang = lang }
                } label: {
                    Text(label).font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(on ? Color.primary : Color.secondary)
                        .frame(width: 42, height: 32)
                        .background(on ? AnyShapeStyle(.background.opacity(0.9)) : AnyShapeStyle(.clear), in: Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .glassCapsule()
        .environment(\.layoutDirection, .leftToRight)
    }
}
