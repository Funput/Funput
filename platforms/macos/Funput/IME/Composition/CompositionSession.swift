import Foundation

/// Decides, word by word, how the word being composed reaches the focused app, and keeps
/// what it learns about that app for one activation.
///
/// A word is written straight into the document (`DirectPresenter`) when the user allows
/// it and the client takes replacement ranges; otherwise it is marked text
/// (`MarkedPresenter`), exactly as before direct composition existed. The choice is made
/// when a word starts and kept until it is committed, so one word never mixes the two.
struct CompositionSession {
    private enum Availability {
        case unknown
        case available
        case unavailable
    }

    /// The user's switch, read as each word starts so a change applies to the next word.
    private let directEnabled: () -> Bool
    /// Clicks seen so far, anywhere; read only for words written into the document.
    private let clicks: () -> Int

    private var direct = Availability.unknown
    private var active: (any CompositionPresenter)?
    private var clicksAtStart = 0

    init(directEnabled: @escaping () -> Bool, clicks: @escaping () -> Int) {
        self.directEnabled = directEnabled
        self.clicks = clicks
    }

    /// Forget the word and the client: focus has moved to a new one.
    mutating func reset() {
        active = nil
        direct = .unknown
    }

    /// Show `text` as the word being composed. False when the word had to be dropped: the
    /// document is unchanged, and the caller drops it from the engine too.
    mutating func show(_ text: String, on surface: CompositionSurface) -> Bool {
        // Clearing marked text that is not there is how Funput always behaved; keep it.
        var presenter = active ?? (text.isEmpty ? MarkedPresenter() : startWord(on: surface))
        switch presenter.show(text, on: surface) {
        case .shown:
            active = text.isEmpty ? nil : presenter
            return true
        case .dropped:
            active = nil
            return false
        case .unsupported:
            // Every later word would fail the same way; mark them until focus moves.
            direct = .unavailable
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
        guard directEnabled(), isDirectAvailable(on: surface) else { return MarkedPresenter() }
        clicksAtStart = clicks()
        return DirectPresenter()
    }

    /// Asked once per activation: the answer belongs to the client, not to the keystroke.
    private mutating func isDirectAvailable(on surface: CompositionSurface) -> Bool {
        if direct == .unknown {
            direct = surface.supportsDocumentAccess ? .available : .unavailable
        }
        return direct == .available
    }
}
