import Carbon
import Foundation

/// Hands keys untouched to password fields that cannot hold marked text.
///
/// macOS leaves an ASCII-capable input method such as Funput selected in a secure field, but
/// AppKit's secure text view cannot hold marked text: it types the first marked string as
/// plain text and ignores the rest, so committing the word typed its first letter twice
/// (#720). Such a field gets every key raw, the way a keyboard layout would type it.
///
/// Secure input only says that some field wants it, so the focused field is asked: a key that
/// types text is marked, and the marked range the field then reports is its answer.
/// - A non-empty range: the field holds marked text. The probe is cleared and the key is
///   composed as usual, so a secure field that holds marked text, as a browser's may, is
///   still typed in Vietnamese (`docs/KNOWN_ISSUES.md`).
/// - An empty range at the caret: the field typed the probe. It refuses marked text, and
///   every key goes to it raw until secure input ends or focus moves to another client.
/// - No range at all: the field does not say (Warp never reports marked text), so it may
///   be showing the probe. The probe is cleared and the field composes as it always has.
///
/// The probe never lands on a composition: a word in progress ends first. A field that held
/// marked text is asked again at the next word, since focus may have moved to another field
/// of the same client. Nothing before the caret is read, and no permission beyond being the
/// input method is needed.
struct SecureInputGuard {
    enum Decision: Equatable {
        /// Not a field that refuses marked text: handle the key as usual.
        case compose
        /// A field that refuses marked text: the app types the key itself.
        case handOver
        /// The probe typed the key into a field that refuses marked text, so the key is done.
        case typed
    }

    private enum Answer {
        case holdsMarkedText
        case refusesMarkedText
    }

    private let secureInputEnabled: () -> Bool
    /// What the focused field answered, kept while secure input stays on.
    private var answer: Answer?

    init(secureInputEnabled: @escaping () -> Bool = { IsSecureEventInputEnabled() }) {
        self.secureInputEnabled = secureInputEnabled
    }

    /// Focus moved to another client: its fields are asked afresh.
    mutating func reset() {
        answer = nil
    }

    /// What to do with a key that types `key`, or nil for a key that types nothing
    /// (`InputEventPolicy.typedText`). `composing` says whether a word is in progress and
    /// `endWord` ends it; both are used only while secure input is on, so the usual typing
    /// path pays for neither.
    mutating func decide(
        typing key: String?,
        on surface: CompositionSurface,
        composing: @autoclosure () -> Bool = false,
        endWord: () -> Void = {}
    ) -> Decision {
        guard secureInputEnabled() else {
            answer = nil
            return .compose
        }
        if answer == .refusesMarkedText { return .handOver }
        let wordInProgress = composing()
        if answer == .holdsMarkedText, wordInProgress || key == nil { return .compose }
        if wordInProgress { endWord() }
        // Until the field has answered, a key that types nothing reaches it untouched, so
        // nothing before the caret of a password field is read.
        guard let key else { return .handOver }
        return probe(with: key, on: surface)
    }

    private mutating func probe(with key: String, on surface: CompositionSurface) -> Decision {
        surface.mark(key, replacing: nil)
        // Ask for the marked text, not the caret: within one key IMKit may report the caret
        // as it was when the key arrived, while the client answers for its marked text live.
        guard surface.markedRange()?.length == 0 else {
            answer = .holdsMarkedText
            surface.mark("", replacing: nil)
            return .compose
        }
        answer = .refusesMarkedText
        return .typed
    }
}
