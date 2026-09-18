import Foundation
import Testing
import ThemeRuntime
import ThemeSchema
import UIKit
@testable import Funput

@MainActor
@Suite("Bundled theme thumbnails")
struct BundledThemeThumbnailTests {
    @Test("Every bundled theme ships artwork for both interface styles")
    func artworkExists() {
        for theme in BundledThemes.all {
            for style in [UIUserInterfaceStyle.light, .dark] {
                let name = BundledThemeThumbnail.assetName(themeID: theme.id, style: style)
                #expect(UIImage(named: name) != nil, "Missing \(name); run Scripts/export-theme-thumbnails.sh")
            }
        }
    }

    @Test("Artwork was captured from the current theme definitions")
    func artworkIsCurrent() throws {
        let url = try #require(
            Bundle.main.url(forResource: BundledThemeThumbnail.manifestName, withExtension: "json")
        )
        let manifest = try JSONDecoder().decode([String: String].self, from: Data(contentsOf: url))

        #expect(Set(manifest.keys) == Set(BundledThemes.all.map(\.id)))
        for theme in BundledThemes.all {
            #expect(
                manifest[theme.id] == BundledThemeThumbnail.fingerprint(theme),
                "\(theme.id) changed since its thumbnail was captured; run Scripts/export-theme-thumbnails.sh"
            )
        }
    }

    @Test("Custom themes keep the drawn sketch")
    func customThemesHaveNoArtwork() {
        let resolved = ThemeRuntime.resolve(.classicLight)
        #expect(BundledThemeThumbnail.image(themeID: "custom-theme", theme: resolved, style: .light) == nil)
    }
}
