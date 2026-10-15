#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

/// Key label size and weight from the theme (issue #499, Android #551).
@MainActor
struct KeyLabelTypographyTests {
    private let traits = UITraitCollection(userInterfaceStyle: .light)

    @Test("A theme that never set them draws the fonts the keyboard always used")
    func defaultsAreUnchanged() {
        let theme = ResolvedTheme.funputGlass
        expect(KeyboardKeyContentStyle.font(for: .character, theme: theme), 22, .regular)
        expect(KeyboardKeyContentStyle.font(for: .symbols, theme: theme), 14, .semibold)
        expect(KeyboardKeyContentStyle.font(for: .space, theme: theme), 12, .medium)
        expect(KeyboardKeyContentStyle.hintFont(theme: theme), 9, .medium)
        expect(KeyboardKeyContentStyle.cornerFont(theme: theme), 17, .regular)
    }

    @Test("Size and weight move every kind of label together")
    func boldAndLarger() {
        var theme = ResolvedTheme.funputGlass
        theme.fontScale = 1.3
        theme.labelWeight = .bold
        expect(KeyboardKeyContentStyle.font(for: .character, theme: theme), 22 * 1.3, .bold)
        // Modifier text stays heavier than letters, stopping short of black.
        expect(KeyboardKeyContentStyle.font(for: .symbols, theme: theme), 14 * 1.3, .heavy)

        theme.labelWeight = .light
        expect(KeyboardKeyContentStyle.font(for: .character, theme: theme), 22 * 1.3, .light)
        expect(KeyboardKeyContentStyle.font(for: .symbols, theme: theme), 14 * 1.3, .medium)
    }

    @Test("Larger text gives way to a short key, but never below its normal size")
    func fitsShortKeys() {
        let font = UIFont.systemFont(ofSize: 22 * 1.3, weight: .bold)
        let fitted = KeyLabelTypography.fitting(font, scale: 1.3, height: 26)
        #expect(fitted.lineHeight <= 26 || abs(fitted.pointSize - 22) < 0.01)
        #expect(fitted.pointSize >= 22 - 0.01)
        // At the normal size nothing moves, however short the key.
        let normal = UIFont.systemFont(ofSize: 22)
        #expect(KeyLabelTypography.fitting(normal, scale: 1, height: 10) == normal)
    }

    @Test("A key's label is fitted to its height when the content is laid out")
    func contentViewFits() throws {
        var theme = ResolvedTheme.funputGlass
        theme.fontScale = 1.3
        let content = KeyboardKeyContentView(frame: CGRect(x: 0, y: 0, width: 32, height: 34))
        content.apply(
            spec: KeySpec(id: "g", label: "g", role: .character),
            presentation: KeyboardPresentation(theme: theme),
            traits: traits
        )
        content.layoutIfNeeded()
        let label = try #require(content.subviews.compactMap { $0 as? UILabel }.first { $0.text == "g" })
        #expect(label.font.pointSize < 22 * 1.3)
        #expect(label.font.pointSize >= 22 - 0.01)
    }

    @Test("The key callout and the alternates follow the theme's type")
    func previewsFollowTheTheme() throws {
        var theme = ResolvedTheme.funputGlass
        theme.fontScale = 1.2
        theme.labelWeight = .medium
        let preview = KeyboardKeyPreviewView()
        preview.apply(theme: theme, traits: traits)
        let label = try #require(preview.subviews.compactMap { $0 as? UILabel }.first)
        expect(label.font, 30 * 1.2, .medium)
    }

    private func expect(
        _ font: UIFont,
        _ size: CGFloat,
        _ weight: UIFont.Weight,
        sourceLocation: SourceLocation = #_sourceLocation
    ) {
        let traits = font.fontDescriptor.object(forKey: .traits) as? [UIFontDescriptor.TraitKey: Any]
        let actual = (traits?[.weight] as? CGFloat) ?? 0
        #expect(abs(font.pointSize - size) < 0.01, sourceLocation: sourceLocation)
        #expect(abs(actual - weight.rawValue) < 0.01, sourceLocation: sourceLocation)
    }
}
#endif
