/// Line structure around the caret, derived from the text the host hands back.
///
/// Lines here are logical — newline-separated — not the soft-wrapped lines the user sees.
/// A keyboard extension has no access to the host's text layout, so wrap positions are
/// simply unknowable from this side.
///
/// The window is whatever the proxy returned, which some hosts truncate to the current
/// paragraph. Truncation only hides lines further out, so a step can stop short of where the
/// document would have allowed; it never lands the caret somewhere it should not be.
struct CaretLineGeometry {
    struct Resolution: Equatable {
        /// Characters to hand to the document's `moveCursor` mutation.
        let offset: Int
        /// Column the pan is aiming for, carried into the next vertical step so passing
        /// through a short line does not permanently pull the caret left. `nil` when the
        /// move was purely horizontal and no column was ever computed.
        let column: Int?
    }

    /// Characters between the start of the caret's line and the caret.
    let column: Int
    /// Characters between the caret and the end of its line.
    private let trailing: Int
    /// Length of every line in the window, in order.
    private let lineLengths: [Int]
    /// Index of the caret's own line within `lineLengths`.
    private let lineIndex: Int

    init(_ context: KeyboardCaretContext) {
        let before = context.before.split(
            omittingEmptySubsequences: false,
            whereSeparator: \.isNewline
        )
        let after = context.after.split(
            omittingEmptySubsequences: false,
            whereSeparator: \.isNewline
        )
        column = before.last?.count ?? 0
        trailing = after.first?.count ?? 0
        lineIndex = max(before.count - 1, 0)
        lineLengths = before.dropLast().map(\.count)
            + [column + trailing]
            + after.dropFirst().map(\.count)
    }

    /// Resolves a trackpad step into one character offset.
    ///
    /// - Parameter desiredColumn: the column an earlier step in the same pan was aiming
    ///   for, or `nil` to take the caret's current column as the target.
    func resolve(columns: Int, lines: Int, desiredColumn: Int?) -> Resolution {
        // The horizontal-only case needs no line structure at all, and stays exactly the
        // character offset the caller asked for.
        guard lines != 0 else { return Resolution(offset: columns, column: nil) }
        let target = min(max(lineIndex + lines, 0), lineLengths.count - 1)
        guard target != lineIndex else {
            // Nowhere left to go on that side, so the vertical part of the step does nothing.
            // Sliding to the line's own edge instead reads as the caret wandering off on its
            // own — the finger is still moving, so the jump looks like a fault rather than a
            // limit. A diagonal drag keeps its horizontal half, which is still meaningful.
            return Resolution(offset: columns, column: nil)
        }
        // The horizontal component of a diagonal drag still applies; the remembered
        // column only replaces the one the caret happens to sit on right now.
        let wanted = max(0, (desiredColumn ?? column) + columns)
        let landing = min(wanted, lineLengths[target])
        return Resolution(offset: distance(to: target, landingOn: landing), column: wanted)
    }

    private func distance(to target: Int, landingOn landing: Int) -> Int {
        if target < lineIndex {
            let skipped = lineLengths[(target + 1)..<lineIndex].reduce(0, +)
            let newlines = lineIndex - target
            return -(column + skipped + newlines + lineLengths[target] - landing)
        }
        let skipped = lineLengths[(lineIndex + 1)..<target].reduce(0, +)
        return trailing + skipped + (target - lineIndex) + landing
    }
}
