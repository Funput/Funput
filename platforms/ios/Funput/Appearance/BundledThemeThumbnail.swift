import CryptoKit
import Foundation
import ThemeRuntime
import ThemeSchema
import UIKit

/// Pre-rendered gallery artwork for the themes that ship with the app.
///
/// The images are captured from the production keyboard surface by
/// `Scripts/export-theme-thumbnails.sh`, so the gallery shows the real keyboard without
/// mounting one renderer per card. `ThemeThumbnails.json` records the theme definition
/// each image was captured from; a unit test fails when a theme changes and the artwork
/// was not regenerated.
enum BundledThemeThumbnail {
    static let manifestName = "ThemeThumbnails"

    static func assetName(themeID: String, style: UIUserInterfaceStyle) -> String {
        "ThemeThumbnail-\(themeID)-\(style == .dark ? "dark" : "light")"
    }

    /// The artwork for a bundled theme, or `nil` when it would not match what this device
    /// draws — custom themes, and glass themes where glass degrades to translucent.
    static func image(themeID: String, theme: ResolvedTheme, style: UIUserInterfaceStyle) -> UIImage? {
        guard BundledThemes.theme(id: themeID) != nil else { return nil }
        if theme.material == .glass, !rendersGlass { return nil }
        return UIImage(named: assetName(themeID: themeID, style: style))
    }

    /// A stable digest of a theme definition, used to spot artwork captured from an older one.
    static func fingerprint(_ theme: KeyboardTheme) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let data = (try? encoder.encode(theme)) ?? Data()
        return SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }

    /// The artwork is captured with Liquid Glass, so it only stands in for glass themes
    /// where the keyboard really draws glass.
    private static var rendersGlass: Bool {
        guard #available(iOS 26, *) else { return false }
        return !UIAccessibility.isReduceTransparencyEnabled
    }
}
