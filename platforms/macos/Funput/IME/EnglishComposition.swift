import Foundation

/// English uses the same owned preedit as Vietnamese, but the engine leaves keys
/// literal. This avoids querying committed text (unavailable in Chrome's omnibox).
enum EnglishComposition {
    struct Edit: Equatable {
        let text: String
        let marked: Bool
        let handled: Bool
    }

    static func process(_ scalar: Unicode.Scalar, composer: FunputComposer) -> Edit {
        let previous = composer.buffer()
        let (result, output) = composer.processText(scalar)
        let current = composer.buffer()
        if !current.isEmpty {
            return Edit(text: current, marked: true, handled: true)
        }
        let actionKey = scalar == "\t" || scalar == "\n" || scalar == "\r"
        let expanded = result.action == ACTION_SEND
        let text = expanded ? output : previous + String(scalar)
        // Remove exactly the boundary scalar, including when CR + LF is one grapheme.
        let committed = actionKey
            ? String(String.UnicodeScalarView(text.unicodeScalars.dropLast())) : text
        return Edit(text: committed, marked: false, handled: !actionKey)
    }

    static func backspace(composer: FunputComposer) -> Edit? {
        guard let last = composer.buffer().last else { return nil }
        for _ in last.unicodeScalars { composer.backspace() }
        return Edit(text: composer.buffer(), marked: true, handled: true)
    }
}
