import Foundation
import KeyboardInput
import KeyboardRenderer
import PersonalDictionary
import PersonalSuggestions

/// A service with a worker that answers only when a test tells it to, and every
/// list the bar was handed, empty ones included.
@MainActor
final class ServiceProbe {
    let service: PersonalSuggestionService
    private(set) var drawn: [[KeyboardSuggestionCandidate]] = []
    private let worker = ScriptedWorker()

    init(enabled: Bool = true) {
        let worker = worker
        service = PersonalSuggestionService { handler in
            worker.handler = handler
            return worker
        }
        service.onCandidates = { [weak self] _, values in self?.drawn.append(values) }
        service.configure(
            enabled: enabled, hasFullAccess: true, resetToken: nil, activationGeneration: 1
        )
    }

    var shown: [String] { drawn.last?.map(\.text) ?? [] }
    var learned: [ScriptedWorker.Learned] { worker.learned }
    var queries: [PersonalSuggestionQueryRequest] { worker.queries }

    func type(_ prefix: String, after context: String? = nil) {
        service.update(.init(prefix: prefix, completedToken: nil, context: context), canQuery: true)
    }

    /// Answers the last question asked, and lets the service take the answer in.
    func answer(_ words: String...) async {
        guard let request = worker.queries.last else { return }
        worker.handler?(request, words.map(PersonalSuggestionCandidate.init(text:)))
        await Task.yield()
    }
}

final class ScriptedWorker: PersonalSuggestionWorking, @unchecked Sendable {
    struct Learned: Equatable {
        let token: String
        let previous: String?
    }

    var handler: PersonalSuggestionResultHandler?
    private(set) var queries: [PersonalSuggestionQueryRequest] = []
    private(set) var learned: [Learned] = []

    func configure(_ configuration: PersonalSuggestionWorkerConfiguration) {}
    func learn(_ token: String, after previous: String?) {
        learned.append(.init(token: token, previous: previous))
    }
    func query(_ request: PersonalSuggestionQueryRequest) { queries.append(request) }
    func flush() {}
    func setSeedLanguage(_ language: SuggestionSeedLanguage) {}
    func edit(_ kind: DictionaryEditKind, word: String) {}
}
