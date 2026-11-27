import CryptoKit
import Foundation
import FunputShared
import ThemeSchema
import UIKit

/// Names one captured picture of a custom theme: which theme, in which interface style,
/// and a digest of everything that changes how its keyboard looks. A picture whose
/// digest no longer matches is stale and is captured again the next time it shows.
struct ThemeSnapshotKey: Hashable {
    /// Bump when the keyboard renderer draws differently, so every capture is redone.
    static let rendererRevision = 1

    let themeID: String
    let mode: AppearancePreviewMode
    let fingerprint: String

    init(theme: KeyboardTheme, mode: AppearancePreviewMode, configuration: FunputConfiguration) {
        themeID = theme.id
        self.mode = mode
        fingerprint = Self.fingerprint(theme: theme, configuration: configuration)
    }

    /// The theme without its name and author, plus every setting the renderer draws
    /// with: the layout options, the space key's language, a pinned appearance (glass
    /// then brings its own backdrop), and the device's glass and transparency settings.
    private static func fingerprint(theme: KeyboardTheme, configuration: FunputConfiguration) -> String {
        var looks = theme
        looks.metadata = ThemeMetadata(name: "", author: "")
        let inputs = [
            BundledThemeThumbnail.fingerprint(looks),
            "\(rendererRevision)",
            "\(BundledThemeThumbnail.rendersGlass)",
            "\(UIAccessibility.isReduceTransparencyEnabled)",
            "\(configuration.inputMethod)",
            "\(configuration.language)",
            "\(configuration.keyboardAppearance)",
            "\(configuration.showsNumberRow)",
            "\(configuration.layoutPreset)",
            "\(configuration.keySizing)",
            "\(configuration.showsToolbar)",
            "\(configuration.languageToggleEnabled)",
        ].joined(separator: "|")
        let digest = SHA256.hash(data: Data(inputs.utf8))
        return digest.prefix(12).map { String(format: "%02x", $0) }.joined()
    }
}
