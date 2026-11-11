import Carbon
import Foundation

/// Hands keys untouched to password fields that cannot hold marked text.
///
/// macOS leaves an ASCII-capable input method such as Funput selected in a secure field, but
/// AppKit's secure text view cannot hold marked text: it types the first marked string as
/// plain text and ignores the rest, so committing the word typed its first letter twice
/// (#720). Such a field gets every key raw, the way a keyboard layout would type it.
///
/// Secure input only says that some field wants it, so the focused field is asked. The first
/// key typed while secure input is on is marked, and a field that then reports no marked text
/// has typed it. A field that reports the mark gets the probe cleared and keeps composing, so
/// a secure field that does hold marked text, as a browser's may, is composed as usual
/// (`docs/KNOWN_ISSUES.md`). Nothing before the caret is read, and no permission beyond
/// being the input method is needed.
struct SecureInputGuard {
    enum Decision: Equatable {
        /// Not a field that refuses marked text: handle the key as usual.
        case compose
        /// A field that refuses marked text: the app types the key itself.
        case handOver
        /// The probe typed the key into a field that refuses marked text, so the key is done.
        case typed
    }

    private let secureInputEnabled: () -> Bool
    /// What the focused field answered, kept while secure input stays on.
    private var refusesMarkedText: Bool?

    init(secureInputEnabled: @escaping () -> Bool = { IsSecureEventInputEnabled() }) {
        self.secureInputEnabled = secureInputEnabled
    }

    /// Focus moved to another client: its fields are asked afresh.
    mutating func reset() {
        refusesMarkedText = nil
    }

    /// What to do with a key that types `key`, or nil for a key that types nothing
    /// (`InputEventPolicy.typedText`).
    mutating func decide(typing key: String?, on surface: CompositionSurface) -> Decision {
        guard secureInputEnabled() else {
            refusesMarkedText = nil
            return .compose
        }
        if let refusesMarkedText { return refusesMarkedText ? .handOver : .compose }
        // Until the field has answered, a key that types nothing reaches it untouched.
        guard let key, !key.isEmpty else { return .handOver }

        surface.mark(key, replacing: nil)
        // Ask for the marked text, not the caret: within one key IMKit may report the caret
        // as it was when the key arrived, while the client answers for its marked text live.
        let refuses = !surface.holdsMarkedText()
        refusesMarkedText = refuses
        guard !refuses else { return .typed }
        surface.mark("", replacing: nil)
        return .compose
    }
}
