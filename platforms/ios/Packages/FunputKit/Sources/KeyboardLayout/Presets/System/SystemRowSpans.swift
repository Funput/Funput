import Foundation

/// Where Apple puts the keys of its letters, symbols and action rows on Face ID iPhones, on
/// the ten-column letter grid. Measured on iOS 27 at 390–440pt; see
/// `docs/KEY_ACCURACY_INVESTIGATION.md`.
enum SystemRowSpans {
    /// `<switch> emoji <space> <enter>`. The spacebar runs from under `x` to the end of `n`,
    /// enter takes the rest, and the switch key and emoji key split what is left at the
    /// leading edge. Apple shows the emoji key whenever the Emoji keyboard is enabled, which
    /// is the default; the globe and dictation keys sit below the keyboard instead.
    static let action: [KeyColumnSpan] = [
        KeyColumnSpan(start: .leading, end: .grid(columns: 1.25, gaps: -1)),
        KeyColumnSpan(start: .grid(columns: 1.25), end: .grid(columns: 2.5, gaps: -1)),
        KeyColumnSpan(start: .grid(columns: 2.5), end: .grid(columns: 7.5, gaps: -1)),
        KeyColumnSpan(start: .grid(columns: 7.5), end: .trailing),
    ]

    /// `shift z x c v b n m delete`, with the letters under `s`…`l`.
    ///
    /// Without a number row Apple leaves about a quarter key plus two-thirds of a gap
    /// between Shift and `z` (13.6pt at 390pt, 14.6pt at 440pt); with one, Shift and Delete
    /// grow to a plain gap from the letters.
    static func bottomLetters(hasNumberRow: Bool) -> [KeyColumnSpan] {
        [shift(hasNumberRow: hasNumberRow)]
            + (0..<7).map { KeyColumnSpan.letter(at: lettersStart + CGFloat($0)) }
            + [delete(hasNumberRow: hasNumberRow)]
    }

    /// `<switch> . , ? ! ' <delete>`, the third row of both symbol pages.
    ///
    /// The page switch and Delete take the places Shift and Delete have on a letters page
    /// without digits, and the punctuation shares out the seven columns `z`…`m` cover, so
    /// each of five keys is 1.4 columns wide. At 393pt that is 6.7–50.7 for the switch,
    /// 64.3–112.3 for the period and 342.3–386.3 for Delete.
    static func symbolPunctuation(count: Int) -> [KeyColumnSpan] {
        let width = lettersWidth / CGFloat(count)
        return [shift(hasNumberRow: false)]
            + (0..<count).map { index in
                let start = lettersStart + width * CGFloat(index)
                return KeyColumnSpan(
                    start: .grid(columns: start),
                    end: .grid(columns: start + width, gaps: -1)
                )
            }
            + [delete(hasNumberRow: false)]
    }

    /// Where `z` starts, under `s`.
    private static let lettersStart: CGFloat = 1.5
    /// How many columns `z`…`m` cover.
    private static let lettersWidth: CGFloat = 7

    private static func shift(hasNumberRow: Bool) -> KeyColumnSpan {
        let end: KeyColumnSpan.Anchor = hasNumberRow
            ? .grid(columns: 1.5, gaps: -1)
            : .grid(columns: 1.25, gaps: -2.0 / 3.0)
        return KeyColumnSpan(start: .leading, end: end)
    }

    private static func delete(hasNumberRow: Bool) -> KeyColumnSpan {
        let start: KeyColumnSpan.Anchor = hasNumberRow
            ? .grid(columns: 8.5)
            : .grid(columns: 8.75, gaps: -1.0 / 3.0)
        return KeyColumnSpan(start: start, end: .trailing)
    }
}
