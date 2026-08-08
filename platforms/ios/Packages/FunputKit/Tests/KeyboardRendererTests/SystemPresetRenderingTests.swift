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
