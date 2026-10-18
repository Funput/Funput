import Foundation

/// Decides, word by word, how the word being composed reaches the focused app.
///
/// A word is written straight into the document (`DirectPresenter`) when the user allows it
/// and the client has earned it (`ClientTrust`); otherwise it is marked text
/// (`MarkedPresenter`), exactly as before direct composition existed. A word keeps the way it
/// started until it is committed, with one exception: an unproven client that shows Funput
/// the marked word it is displaying has proven itself, and that word turns direct at once.
struct CompositionSession {
    /// The user's switch, read as each word starts so a change applies to the next word.
    private let directEnabled: () -> Bool
    /// Clicks seen so far, anywhere; read only for words written into the document.
    private let clicks: () -> Int

    private var trust: ClientTrust
    private var active: (any CompositionPresenter)?
    private var clicksAtStart = 0

    init(directEnabled: @escaping () -> Bool, clicks: @escaping () -> Int, memory: ProvenClients) {
        self.directEnabled = directEnabled
        self.clicks = clicks
        trust = ClientTrust(memory: memory)
    }

    /// Forget the word and the client: focus has moved to a new one.
    mutating func reset() {
        active = nil
        trust.reset()
    }

    /// Show `text` as the word being composed. False when the word had to be dropped: the
    /// document is unchanged, and the caller drops it from the engine too.
    mutating func show(_ text: String, on surface: CompositionSurface) -> Bool {
        if let marked = active, !marked.writesIntoDocument, !text.isEmpty, directEnabled(),
            trust.provesWithMarkedWord(marked.shown, on: surface) {
            // Typing at the caret commits over the marked text: from here the word is plain.
            active = startDirect()
        }
        // Clearing marked text that is not there is how Funput always behaved; keep it.
        var presenter = active ?? (text.isEmpty ? MarkedPresenter() : startWord(on: surface))
        let outcome = presenter.show(text, on: surface)
        switch outcome {
        case .shown, .rewritten:
            if outcome == .rewritten { trust.confirm(on: surface) }
            active = text.isEmpty ? nil : presenter
            return true
        case .dropped:
            trust.withdrawIfUnconfirmed(on: surface)
            active = nil
            return false
        case .unsupported:
            trust.refuse(on: surface)
            active = nil
            return false
        }
    }

    /// End the word so the document reads `word` followed by `boundary`. With no word on
    /// screen this is ordinary typing.
    mutating func commit(_ word: String, boundary: String, on surface: CompositionSurface) {
        guard var presenter = active else {
            if !(word + boundary).isEmpty { surface.insert(word + boundary, replacing: nil) }
            return
        }
        active = nil
        presenter.commit(word, boundary: boundary, on: surface)
    }

    /// Make a committed word the live composition again (retone after Backspace).
    mutating func reopen(_ word: String, replacing range: NSRange, on surface: CompositionSurface) {
        var presenter = startWord(on: surface)
        presenter.reopen(word, replacing: range, on: surface)
        active = presenter
    }

    /// End a word written into the document if the user clicked since it started. With no
    /// marked text IMKit never reports that click, and the caret may now be anywhere.
    /// Returns whether a word ended, so the caller clears the engine as well.
    mutating func endIfClicked() -> Bool {
        guard active?.writesIntoDocument == true, clicks() != clicksAtStart else { return false }
        active = nil
        return true
    }

    private mutating func startWord(on surface: CompositionSurface) -> any CompositionPresenter {
        guard directEnabled(), trust.allowsDirect(on: surface) else { return MarkedPresenter() }
        return startDirect()
    }

    private mutating func startDirect() -> any CompositionPresenter {
        clicksAtStart = clicks()
        return DirectPresenter()
    }
}
