#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import os

/// The dictionary typo correction consults before it repairs a word.
///
/// A second, **read-only** store, separate from the one the keyboard learns into.
/// That one lives on a serial queue off the main actor and is sometimes busy writing
/// to disk, and the correction handshake cannot wait: the word boundary has deferred
/// an edit and is holding it open. This one is built once, on a background queue,
/// handed over, and never written to again — so reading it from the main actor races
/// with nothing.
///
/// Until it is ready `recognizes` answers `true` for everything, which reads oddly
/// and is deliberate: an unknown answer has to mean "do not correct", and the
/// coordinator vetoes any word the dictionary claims to know.
@MainActor
public final class CorrectionWordSource: CorrectionDictionary {
    private var engine: PersonalSuggestionEngine?
    private var loading = false
    private let lexiconURL: @Sendable () -> URL?
    /// Words the user took a correction back on, oldest first so the cap drops the
    /// oldest. Lowercased: `Ko` at the start of a sentence is the same word.
    private var kept: [String] = []
    private var keptSet: Set<String> = []
    /// Called with the whole kept list whenever it grows, so the host can store it.
    /// Nil when there is nowhere to store it — without Full Access the list lasts as
    /// long as the keyboard does.
    public var onKeep: (@MainActor ([String]) -> Void)?

    /// How many kept words are remembered.
    public static let keptLimit = 200

    public init(
        lexiconURL: @escaping @Sendable () -> URL? = {
            Bundle.main.url(forResource: "en", withExtension: "lex")
        }
    ) {
        self.lexiconURL = lexiconURL
    }

    /// Start building it. Opening the lexicon measures around 5 ms, which is why it
    /// does not happen on the way to the first keystroke.
    public func prepare() {
        guard engine == nil, !loading else { return }
        loading = true
        let lexiconURL = lexiconURL
        Task.detached(priority: .utility) {
            let built = Self.build(lexiconURL: lexiconURL)
            await MainActor.run { [weak self] in
                self?.engine = built
                self?.loading = false
            }
        }
    }

    public func recognizes(_ word: String) -> Bool {
        if isKept(word) { return true }
        guard let engine else { return true }
        return engine.recognizes(word)
    }

    public func keep(_ word: String) {
        let key = word.lowercased()
        guard !key.isEmpty, keptSet.insert(key).inserted else { return }
        kept.append(key)
        if kept.count > Self.keptLimit {
            keptSet.remove(kept.removeFirst())
        }
        onKeep?(kept)
    }

    /// Whether the user took a correction of `word` back, in this session or one
    /// restored from an earlier one.
    public func isKept(_ word: String) -> Bool {
        keptSet.contains(word.lowercased())
    }

    /// Put back the words kept in an earlier session.
    public func restoreKept(_ words: [String]) {
        kept = Array(words.suffix(Self.keptLimit))
        keptSet = Set(kept)
    }

    /// Built off the main actor, then handed over and never written to again. The
    /// engine is a serial-owner type, so a single hand-off like this is safe where
    /// sharing it would not be.
    private nonisolated static func build(
        lexiconURL: @Sendable () -> URL?
    ) -> PersonalSuggestionEngine? {
        guard let engine = PersonalSuggestionEngine.inMemory() else { return nil }
        guard let url = lexiconURL(), engine.attachLexicon(url: url) else {
            os_log(
                .error,
                log: .default,
                "Typo correction has no word list; corrections stay off"
            )
            return nil
        }
        return engine
    }
}
#endif
