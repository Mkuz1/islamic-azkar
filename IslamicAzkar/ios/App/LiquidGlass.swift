import SwiftUI
#if os(iOS)
import UIKit
#endif

// Apple's guidance for Liquid Glass: glass belongs to the navigation layer (tab bars,
// toolbars, sheets, buttons) — iOS draws those itself. Content sits on the plain grouped
// background in ordinary cards, so that's what these helpers provide.

extension Color {
    /// systemGroupedBackground
    static var groupedBackground: Color {
        #if os(iOS)
        Color(uiColor: .systemGroupedBackground)
        #else
        Color(nsColor: .windowBackgroundColor)
        #endif
    }

    /// secondarySystemGroupedBackground — cards on the grouped background.
    static var groupedCard: Color {
        #if os(iOS)
        Color(uiColor: .secondarySystemGroupedBackground)
        #else
        Color(nsColor: .controlBackgroundColor)
        #endif
    }

    /// tertiarySystemFill — chips, idle controls.
    static var softFill: Color {
        #if os(iOS)
        Color(uiColor: .tertiarySystemFill)
        #else
        Color.gray.opacity(0.15)
        #endif
    }
}

extension View {
    /// A plain content card on the grouped background (iOS 26 corner radius).
    func card(cornerRadius: CGFloat = 22) -> some View {
        background(Color.groupedCard, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    /// The tinted, filled call-to-action: native Liquid Glass on iOS 26.
    @ViewBuilder
    func prominentGlassButton() -> some View {
        if #available(iOS 26.0, macOS 26.0, *) {
            self.buttonStyle(.glassProminent)
        } else {
            self.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
        }
    }

    /// Secondary button: native Liquid Glass on iOS 26.
    @ViewBuilder
    func glassButton() -> some View {
        if #available(iOS 26.0, macOS 26.0, *) {
            self.buttonStyle(.glass)
        } else {
            self.buttonStyle(.bordered).buttonBorderShape(.capsule)
        }
    }
}

/// Rounded square with a white SF Symbol, in the style of the Settings app.
struct SymbolSquare: View {
    let symbol: String
    let color: Color
    var size: CGFloat = 30

    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: size * 0.5, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(color, in: RoundedRectangle(cornerRadius: size * 0.24, style: .continuous))
    }
}

/// Apple system colours, one per category.
func categoryHue(_ key: String) -> Color {
    switch key {
    case "prayer": return .green
    case "sleep": return .indigo
    case "home": return .orange
    case "hardship": return .blue
    case "illness": return .red
    case "food": return Color(red: 1.0, green: 0.62, blue: 0.04)
    case "family": return .purple
    case "travel": return .teal
    case "nature": return .mint
    case "hajj": return .brown
    default: return .pink
    }
}

/// Transient capsule message ("Copied").
struct ToastView: View {
    let text: String
    var body: some View {
        if #available(iOS 26.0, macOS 26.0, *) {
            Text(text).font(.subheadline.weight(.medium))
                .padding(.horizontal, 18).padding(.vertical, 10)
                .glassEffect(.regular, in: .capsule)
        } else {
            Text(text).font(.subheadline.weight(.medium))
                .padding(.horizontal, 18).padding(.vertical, 10)
                .background(.regularMaterial, in: Capsule())
        }
    }
}

/// Interlaced eight-pointed stars — the Mushaf's backdrop pattern.
struct GeometricPattern: View {
    var color: Color = .white.opacity(0.035)
    var step: CGFloat = 72

    var body: some View {
        Canvas { ctx, size in
            var y: CGFloat = 0
            while y < size.height + step {
                var x: CGFloat = 0
                while x < size.width + step {
                    let r = step / 2
                    ctx.stroke(EightPointStar(inner: 0.78).path(in: CGRect(x: x - r, y: y - r, width: step, height: step)), with: .color(color), lineWidth: 1)
                    ctx.stroke(Path(CGRect(x: x - step * 0.18, y: y - step * 0.18, width: step * 0.36, height: step * 0.36)), with: .color(color), lineWidth: 1)
                    x += step
                }
                y += step
            }
        }
        .allowsHitTesting(false)
    }
}

extension View {
    /// UITableView.Style.insetGrouped (iOS); a plain list elsewhere.
    @ViewBuilder
    func insetGroupedList() -> some View {
        #if os(iOS)
        self.listStyle(.insetGrouped)
        #else
        self.listStyle(.inset)
        #endif
    }
}
