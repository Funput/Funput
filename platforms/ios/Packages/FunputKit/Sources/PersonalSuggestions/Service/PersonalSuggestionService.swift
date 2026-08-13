import Foundation
import KeyboardInput
import KeyboardRenderer

@MainActor
public final class PersonalSuggestionService {
    public var onCandidates: ((UInt64, [KeyboardSuggestionCandidate]) -> Void)?
    private let workerFactory: PersonalSuggestionWorkerFactory
    private var worker: (any PersonalSuggestionWorking)?
    private var workerConfiguration = PersonalSuggestionWorkerConfiguration(
        enabled: false,
        hasFullAccess: false,
        resetToken: nil
    )
    private var generation: UInt64 = 0
    private var activationGeneration: UInt64 = 0
    private var prefix = ""
    private var visibleCandidates: [KeyboardSuggestionCandidate] = []

    public init(workerFactory: @escaping PersonalSuggestionWorkerFactory) {
        self.workerFactory = workerFactory
    }

    public func configure(
        enabled: Bool,
        hasFullAccess: Bool,
        resetToken: UUID?,
        activationGeneration: UInt64
    ) {
        workerConfiguration = .init(
            enabled: enabled,
            hasFullAccess: hasFullAccess,
            resetToken: resetToken
        )
        self.activationGeneration = activationGeneration
        worker?.configure(workerConfiguration)
        invalidateAndPublish()
    }

    public func update(_ update: KeyboardSuggestionInputUpdate, canQuery: Bool) {
        let signpostID = PersonalSuggestionServiceSignposts.beginDispatch()
        defer {
            PersonalSuggestionServiceSignposts.endDispatch(
                signpostID,
                generation: generation
            )
        }
        if let token = update.completedToken, workerConfiguration.enabled {
            ensureWorker()?.learn(token)
        }
        let next = canQuery && workerConfiguration.enabled && update.prefix.count >= 2
            ? update.prefix : ""
        if update.completedToken == nil, next == prefix { return }
        generation &+= 1
        prefix = next
        visibleCandidates = []
        publish([])
        guard !prefix.isEmpty else { return }
        ensureWorker()?.query(.init(prefix: prefix, generation: generation))
    }

    public func acceptance(
        for candidate: KeyboardSuggestionCandidate
    ) -> (String, String)? {
        guard candidate.generation == generation,
              visibleCandidates.contains(candidate), !prefix.isEmpty
        else { return nil }
        return (prefix, candidate.text)
    }

    public func clear() { invalidateAndPublish() }
    public func flush() { worker?.flush() }

    private func ensureWorker() -> (any PersonalSuggestionWorking)? {
        guard workerConfiguration.enabled else { return nil }
        if let worker { return worker }
        let created = workerFactory { [weak self] request, candidates in
            Task { @MainActor [weak self] in
                self?.receive(request: request, candidates: candidates)
            }
        }
        PersonalSuggestionServiceSignposts.recordWorkerCreated()
        created.configure(workerConfiguration)
        worker = created
        return created
    }

    private func invalidateAndPublish() {
        let needsPublish = !prefix.isEmpty || !visibleCandidates.isEmpty
        generation &+= 1
        prefix = ""
        visibleCandidates = []
        if needsPublish { publish([]) }
    }

    private func receive(
        request: PersonalSuggestionQueryRequest,
        candidates: [PersonalSuggestionCandidate]
    ) {
        guard workerConfiguration.enabled,
              request.generation == generation, request.prefix == prefix
        else { return }
        let values = candidates.map {
            KeyboardSuggestionCandidate(
                text: PersonalSuggestionCasing.apply($0.text, matching: prefix),
                generation: generation
            )
        }
        visibleCandidates = values
        publish(values)
        PersonalSuggestionServiceSignposts.recordUI(
            generation: generation,
            count: values.count
        )
    }

    private func publish(_ candidates: [KeyboardSuggestionCandidate]) {
        onCandidates?(activationGeneration, candidates)
    }
}
