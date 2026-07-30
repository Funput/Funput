#if os(iOS)
extension String {
    /// The unbroken run of non-boundary characters at the end of this text, or nil when
    /// it ends on a boundary (or is empty).
    ///
    /// Mirrors the engine's word-boundary contract — whitespace and ASCII punctuation —
    /// the same rule Android's `CompositionBoundary` and Linux's `funput::isBoundary`
    /// state for their own shells.
    func wordBeforeCursor() -> String? {
        let word = String(reversed().prefix { !$0.isCompositionBoundary }.reversed())
        return word.isEmpty ? nil : word
    }
}

extension Character {
    /// Whitespace or ASCII punctuation ends a word, as it does in the engine.
    var isCompositionBoundary: Bool {
        if isWhitespace { return true }
        guard let ascii = asciiValue else { return false }
        return (0x21...0x2F).contains(ascii)
            || (0x3A...0x40).contains(ascii)
            || (0x5B...0x60).contains(ascii)
            || (0x7B...0x7E).contains(ascii)
    }
}
#endif
