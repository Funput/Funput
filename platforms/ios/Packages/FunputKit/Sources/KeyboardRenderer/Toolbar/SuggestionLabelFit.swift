import CoreGraphics

/// How much a suggestion's text shrinks before it is narrowed.
///
/// Candidates share the prefix being typed, so the part that tells them apart is the end.
/// Truncating every long one at the tail left a bar of identical "Autom…" (Android issue
/// #546, the same bar here). A long word is instead made smaller first, which keeps the
/// letter shapes, then drawn in a narrower width of the system font, the way the stock
/// keyboard fits "Automatically". Only a word too long for both is shortened, and then in
/// the middle, so the distinguishing ending still shows. Android does the same with a
/// horizontal text scale; iOS has real condensed widths of SF to use instead.
enum SuggestionLabelFit {
    /// Smallest text size, as a share of the normal one, before narrowing begins.
    static let minimumSizeFactor: CGFloat = 0.8

    /// The size factor for text `textWidth` wide at the normal size in `availableWidth`.
    static func sizeFactor(textWidth: CGFloat, availableWidth: CGFloat) -> CGFloat {
        guard textWidth > availableWidth, availableWidth > 0 else { return 1 }
        return max(minimumSizeFactor, availableWidth / textWidth)
    }
}
