import Foundation
import ThemeSchema

/// WCAG 2.1 relative-contrast helpers over ``ThemeRGBA``.
enum ContrastRatio {
    /// The WCAG contrast ratio between two colors, in the range `1...21`.
    static func between(_ lhs: ThemeRGBA, _ rhs: ThemeRGBA) -> Double {
        let a = relativeLuminance(lhs)
        let b = relativeLuminance(rhs)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private static func relativeLuminance(_ color: ThemeRGBA) -> Double {
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }

    private static func linear(_ channel: Double) -> Double {
        channel <= 0.03928 ? channel / 12.92 : pow((channel + 0.055) / 1.055, 2.4)
    }
}
