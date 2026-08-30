import Foundation
import PersonalSuggestions

@MainActor
final class SuggestionWorkerFactorySpy {
    private(set) var workers: [SuggestionWorkerSpy] = []

    func make(
        handler: @escaping PersonalSuggestionResultHandler
    ) -> any PersonalSuggestionWorking {
        let worker = SuggestionWorkerSpy(handler: handler)
        workers.append(worker)
        return worker
    }

    func makeService() -> PersonalSuggestionService {
        PersonalSuggestionService(workerFactory: make(handler:))
    }

    @discardableResult
    func configure(
        _ service: PersonalSuggestionService,
        activation: UInt64 = 1,
        enabled: Bool = true,
        hasFullAccess: Bool = true
    ) -> PersonalSuggestionWorkerConfiguration {
        let value = PersonalSuggestionWorkerConfiguration(
            enabled: enabled,
            hasFullAccess: hasFullAccess,
            resetToken: UUID()
        )
        service.configure(
            enabled: value.enabled,
            hasFullAccess: value.hasFullAccess,
            resetToken: value.resetToken,
            activationGeneration: activation
        )
        return value
    }
}

final class SuggestionWorkerSpy: PersonalSuggestionWorking, @unchecked Sendable {
    enum Event: Equatable {
        case configure(PersonalSuggestionWorkerConfiguration)
        case learn(String, context: String?)
        case query(PersonalSuggestionQueryRequest)
        case flush

        var learned: (token: String, context: String?)? {
            guard case .learn(let token, let context) = self else { return nil }
            return (token, context)
        }

        var query: PersonalSuggestionQueryRequest? {
            guard case .query(let value) = self else { return nil }
            return value
        }

        var configuration: PersonalSuggestionWorkerConfiguration? {
            guard case .configure(let value) = self else { return nil }
            return value
        }
    }

    private let handler: PersonalSuggestionResultHandler
    private(set) var events: [Event] = []

    init(handler: @escaping PersonalSuggestionResultHandler) {
        self.handler = handler
    }

    func configure(_ configuration: PersonalSuggestionWorkerConfiguration) {
        events.append(.configure(configuration))
    }

    func learn(_ token: String, after previous: String?) {
        events.append(.learn(token, context: previous))
    }

    func query(_ request: PersonalSuggestionQueryRequest) {
        events.append(.query(request))
    }

    func flush() { events.append(.flush) }

    func emit(_ values: [String], for request: PersonalSuggestionQueryRequest) {
        handler(request, values.map(PersonalSuggestionCandidate.init(text:)))
    }
}
