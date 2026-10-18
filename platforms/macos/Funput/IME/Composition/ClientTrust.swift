import Foundation

/// Whether the focused client may have words written straight into it, decided by what it
/// does rather than what it claims.
///
/// Claiming TSM document access is not enough. A minimal `NSTextInputClient` (Warp, and the
/// many views built on GLFW's) answers every caret question with `(0, 0)`, hands over no
/// text and ignores replacement ranges, so no rewrite could ever land. A client therefore
/// has to show Funput the text right before its caret first, either a character already
/// there when a word starts or the marked word it is displaying. Until it does, words stay
/// marked text. A proof is remembered for the app (`ProvenClients`), so later activations
/// start direct at once; the first rewrite that cannot be placed withdraws it.
struct ClientTrust {
    enum Level {
        case refused
        case unproven
        /// Proven in an earlier activation, not yet in this one.
        case remembered
        case proven
    }

    /// Reads spent proving a client per activation, so one that never answers is not
    /// asked on every keystroke.
    static let probesPerActivation = 4

    private let memory: ProvenClients
    private(set) var level: Level?
    private var probesLeft = 0

    init(memory: ProvenClients) {
        self.memory = memory
    }

    /// Focus moved: the next word asks the new client again.
    mutating func reset() {
        level = nil
    }

    /// Whether a word starting now may be written directly.
    mutating func allowsDirect(on surface: CompositionSurface) -> Bool {
        if level == nil { level = initialLevel(on: surface) }
        if level == .unproven, spendProbe(), DirectPresenter.readsBeforeCaret(on: surface) {
            prove(on: surface)
        }
        return level == .proven || level == .remembered
    }

    /// Whether an unproven client shows the marked word `word` right before its caret.
    mutating func provesWithMarkedWord(_ word: String, on surface: CompositionSurface) -> Bool {
        guard level == .unproven, !word.isEmpty, spendProbe(),
            case .found = DirectPresenter.locate(word, on: surface)
        else { return false }
        prove(on: surface)
        return true
    }

    /// A rewrite landed where the client said the word was.
    mutating func confirm(on surface: CompositionSurface) {
        if level == .remembered { prove(on: surface) }
    }

    /// A rewrite could not be placed. A client trusted only from memory loses that trust
    /// and has to prove itself again; a proven one just loses this word.
    mutating func withdrawIfUnconfirmed(on surface: CompositionSurface) {
        guard level == .remembered else { return }
        forget(on: surface)
        level = .unproven
        probesLeft = Self.probesPerActivation
    }

    /// The client cannot place its caret at all.
    mutating func refuse(on surface: CompositionSurface) {
        forget(on: surface)
        level = .refused
    }

    private mutating func initialLevel(on surface: CompositionSurface) -> Level {
        guard surface.supportsDocumentAccess else { return .refused }
        if let id = surface.clientID, memory.contains(id) { return .remembered }
        probesLeft = Self.probesPerActivation
        return .unproven
    }

    private mutating func spendProbe() -> Bool {
        guard probesLeft > 0 else { return false }
        probesLeft -= 1
        return true
    }

    private mutating func prove(on surface: CompositionSurface) {
        level = .proven
        if let id = surface.clientID { memory.insert(id) }
    }

    private func forget(on surface: CompositionSurface) {
        if let id = surface.clientID { memory.remove(id) }
    }
}

/// Apps whose text fields have proven they take direct writes, for the life of the process.
/// Nothing is persisted: a new Funput process proves every app again.
final class ProvenClients {
    static let shared = ProvenClients()

    private var ids = Set<String>()

    func contains(_ id: String) -> Bool { ids.contains(id) }
    func insert(_ id: String) { ids.insert(id) }
    func remove(_ id: String) { ids.remove(id) }
}
