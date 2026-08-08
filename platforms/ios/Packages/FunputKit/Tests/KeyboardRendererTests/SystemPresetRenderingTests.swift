#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct SystemPresetRenderingTests {
    @Test("The action-row emoji key renders as an icon, like the toolbar one")
    func emojiKeyRendersAnIcon() {
        // The renderer builds a control for every spec in the rows without filtering on
        // role, so placing an emoji key in a row needs no renderer change — this test is
        // what keeps that true.
        let keys = SystemKeyboardLayouts.letters(.vni).rows.last?.keys ?? []
        let emoji = keys.first { $0.role == .emoji }
        #expect(emoji?.label.isEmpty == true)
        #expect(KeyboardKeyContentStyle.icon(for: .emoji, shiftState: .lowercase) != nil)
    }

    @Test("The toolbar drops its emoji button when a row provides one")
    func toolbarYieldsToTheRowEmojiKey() {
        let system = surface(for: SystemKeyboardLayouts.letters(.vni))
        #expect(!emojiLabels(in: system).contains { $0 == "Biểu tượng cảm xúc" })
        #expect(emojiLabels(in: system) == ["Mở bảng biểu tượng cảm xúc"])

        // The Funput preset has no row emoji key, so its toolbar button stays.
        let funput = surface(for: StandardKeyboardLayouts.letters(.vni))
        #expect(emojiLabels(in: funput) == ["Biểu tượng cảm xúc"])
    }

    private func surface(for layout: KeyboardLayout) -> KeyboardSurfaceView {
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        return surface
    }

    /// Accessibility labels of every visible control that opens the emoji panel,
    /// wherever it lives — toolbar button or keycap.
    private func emojiLabels(in view: UIView) -> [String] {
        var labels: [String] = []
        for subview in view.subviews where !subview.isHidden {
            if let label = subview.accessibilityLabel, label.contains("biểu tượng cảm xúc")
                || label.contains("Biểu tượng cảm xúc") {
                labels.append(label)
            }
            labels.append(contentsOf: emojiLabels(in: subview))
        }
        return labels
    }

    @Test("Symbol pages are shorter than the letters page when the number row shows")
    func symbolPagesAreShorter() {
        // Deliberate deviation from the stock keyboard, which keeps one height and
        // stretches its rows. Funput derives height from the row count, so under the
        // system preset a VNI keyboard shrinks by a row when the user taps "123".
        // Documented so it reads as a decision rather than a bug.
        let letters = SystemKeyboardLayouts.letters(.vni)
        let symbols = SystemSymbolKeyboardLayouts.primary(.vni)
        #expect(
            KeyboardMetrics.phonePortraitHeight(for: letters)
                > KeyboardMetrics.phonePortraitHeight(for: symbols)
        )
    }

    @Test("Telex without the number row keeps one height across pages")
    func telexPagesShareOneHeight() {
        let letters = SystemKeyboardLayouts.letters(.telex, showsNumberRow: false)
        let symbols = SystemSymbolKeyboardLayouts.primary(.telex)
        #expect(letters.rows.count == symbols.rows.count)
        #expect(
            KeyboardMetrics.phonePortraitHeight(for: letters)
                == KeyboardMetrics.phonePortraitHeight(for: symbols)
        )
    }
}
#endif
