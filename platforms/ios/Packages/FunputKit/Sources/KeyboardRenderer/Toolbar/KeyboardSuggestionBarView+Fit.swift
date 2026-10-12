#if canImport(UIKit)
import UIKit

extension KeyboardSuggestionBarView {
    /// Room a candidate's title gets inside its slot, leaving a margin on both sides.
    static let titleHorizontalInset: CGFloat = 8

    /// System font widths tried in turn once the size has reached its floor.
    private static let widths: [UIFont.Width] = [.standard, .condensed, .compressed]

    /// The title for `text` in a slot `slotWidth` wide; see `SuggestionLabelFit`.
    ///
    /// UIKit has no horizontal text scale: the string's `.expansion` and a font matrix were
    /// both tried, and text measurement ignores both, so a title "fitted" that way still
    /// overflowed. SF's own condensed widths are measured like any other font.
    static func fittedTitle(
        _ text: String,
        slotWidth: CGFloat,
        font: UIFont,
        color: UIColor
    ) -> NSAttributedString {
        let available = slotWidth - titleHorizontalInset * 2
        let factor = SuggestionLabelFit.sizeFactor(
            textWidth: measure(text, font), availableWidth: available
        )
        let size = font.pointSize * factor
        let fitted = widths.lazy
            .map { UIFont.systemFont(ofSize: size, weight: .regular, width: $0) }
            .first { measure(text, $0) <= available }
        // Nothing fits: the narrowest width, and the label truncates in the middle.
        let chosen = fitted ?? .systemFont(ofSize: size, weight: .regular, width: .compressed)
        return NSAttributedString(string: text, attributes: [.font: chosen, .foregroundColor: color])
    }

    private static func measure(_ text: String, _ font: UIFont) -> CGFloat {
        (text as NSString).size(withAttributes: [.font: font]).width
    }
}
#endif
