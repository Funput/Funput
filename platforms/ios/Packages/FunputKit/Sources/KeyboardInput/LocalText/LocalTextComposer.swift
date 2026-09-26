#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout

/// A ``LocalTextComposing`` field that composes Vietnamese with an engine of its own.
///
/// It never shares the document's ``FunputComposer``: picking a search result inserts
/// into the document, which clears that composition, and the query must survive it.
/// The text lives here too, so the engine's scalar delete counts apply exactly — no
/// grapheme translation the way ``KeyboardReplacement`` needs for a proxy.
@MainActor
public final class LocalTextComposer: LocalTextComposing {
    public private(set) var text = ""
    private let composer = FunputComposer()
    private var composesVietnamese = true

    public init() {
        composer.setShortcutsEnabled(false)
    }

    /// Applies the durable options when they are known, then the live method and
    /// language switch, and starts from an empty field. Shortcuts stay off: a search
    /// query is not prose.
    public func configure(
        _ options: FunputCompositionOptions?,
        inputMethod: FunputInputMethod,
        composesVietnamese: Bool
    ) {
        if let options { composer.configure(options) }
        composer.setInputMethod(inputMethod)
        composer.setEnabled(composesVietnamese)
        composer.setShortcutsEnabled(false)
        self.composesVietnamese = composesVietnamese
        reset()
    }

    public func apply(_ edit: LocalTextEdit) {
        switch edit {
        case let .text(value):
            compose(value)
        case .space:
            // A refused space still ends the word, so the next letter starts a new one.
            if LocalTextSpacing.accepts(after: text) { compose(" ") } else { composer.clear() }
        case .deleteBackward:
            deleteBackward()
        }
    }

    public func reset() {
        composer.clear()
        text = ""
    }

    private func compose(_ value: String) {
        for scalar in value.unicodeScalars {
            let result = composer.process(scalar)
            if result.action == .none {
                text.unicodeScalars.append(scalar)
            } else if result.deleteCount <= text.unicodeScalars.count {
                text.unicodeScalars.removeLast(result.deleteCount)
                text.append(result.text)
            } else {
                // The engine wants to rewrite text this field never held; keep the key
                // literal rather than guess, as the document path does.
                composer.clear()
                text.unicodeScalars.append(scalar)
            }
        }
    }

    /// Mirrors the document's Backspace: step the engine back over the last composed
    /// character, then reopen a finished word so `chào` ⌫ `s` gives `cháo`.
    private func deleteBackward() {
        guard !text.isEmpty else { return }
        if let composing = composer.buffer().last {
            for _ in composing.unicodeScalars { composer.backspace() }
        }
        text.removeLast()
        if composesVietnamese, composer.buffer().isEmpty, let word = text.wordBeforeCursor() {
            composer.adopt(word)
        }
    }
}
#endif
