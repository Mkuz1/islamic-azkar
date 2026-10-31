import SwiftUI

/// Brand colours and typefaces shared by the app, the share images and the widgets.
enum Brand {
    static let emerald = Color(red: 0.05, green: 0.54, blue: 0.41)       // #0C8A69
    static let emeraldBright = Color(red: 0.27, green: 0.84, blue: 0.66) // #44D7A8
    static let emeraldDeep = Color(red: 0.04, green: 0.24, blue: 0.21)
    static let gold = Color(red: 0.90, green: 0.78, blue: 0.46)          // #E6C676
    static let goldDeep = Color(red: 0.72, green: 0.53, blue: 0.17)      // #B8862B
    static let night = Color(red: 0.04, green: 0.10, blue: 0.17)

    // The Mushaf's opening-page palette.
    static let mushafNight = Color(red: 0.04, green: 0.11, blue: 0.08)      // #0A1C15
    static let mushafCardTop = Color(red: 0.15, green: 0.28, blue: 0.24)    // #26473C
    static let mushafCardBottom = Color(red: 0.11, green: 0.23, blue: 0.18) // #1B3A2F
    static let mushafGold = Color(red: 0.84, green: 0.70, blue: 0.42)       // #D6B36A
    static let mushafGoldLight = Color(red: 0.95, green: 0.86, blue: 0.64)  // #F1DBA3
    static let tint = Color(red: 0.12, green: 0.42, blue: 0.32)             // #1E6B52

    /// The Mushaf's dark green, used behind widgets.
    static var widgetGradient: LinearGradient {
        LinearGradient(colors: [mushafCardTop, mushafCardBottom], startPoint: .top, endPoint: .bottom)
    }

    static var goldGradient: LinearGradient {
        LinearGradient(colors: [Color(red: 0.92, green: 0.78, blue: 0.46), Color(red: 0.65, green: 0.47, blue: 0.12)],
                       startPoint: .top, endPoint: .bottom)
    }
}

extension Font {
    /// Amiri, a classical Naskh with full diacritics — used for every dhikr.
    static func amiri(_ size: CGFloat, bold: Bool = false) -> Font {
        .custom(bold ? "Amiri-Bold" : "Amiri-Regular", size: size)
    }

    /// Ruqaa calligraphy for «صدقة جارية» and the app name.
    static func ruqaa(_ size: CGFloat) -> Font { .custom("ArefRuqaa-Bold", size: size) }

    /// Quranic script for the basmala.
    static func quran(_ size: CGFloat) -> Font { .custom("AmiriQuran-Regular", size: size) }
}

/// Eight-pointed star (Rub el Hizb), drawn as two overlapping squares.
struct EightPointStar: Shape {
    var inner: CGFloat = 0.62

    func path(in rect: CGRect) -> Path {
        let c = CGPoint(x: rect.midX, y: rect.midY)
        let r = min(rect.width, rect.height) / 2
        var p = Path()
        for i in 0..<16 {
            let a = Double(i) * .pi / 8 - .pi / 2
            let rr = i % 2 == 0 ? r : r * inner
            let pt = CGPoint(x: c.x + rr * cos(a), y: c.y + rr * sin(a))
            if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
        }
        p.closeSubpath()
        return p
    }
}
