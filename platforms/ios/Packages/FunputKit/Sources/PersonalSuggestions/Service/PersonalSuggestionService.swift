import Foundation
import KeyboardInput
import KeyboardLayout
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
    private(set) var generation: UInt64 = 0
    private var activationGeneration: UInt64 = 0
    private(set) var prefix = ""
    private var context: String?
    /// Whether the bar is currently answering a context rather than a prefix.
    private var predicting = false
    /// As the engine returned them. `visibleCandidates` puts them in the user's case.
    var received: [PersonalSuggestionCandidate] = []
    var shift: ShiftState = .lowercase
    /// Last drawn; `nil` after a reconfigure, so reactivation always redraws.
    private var published: [String]?

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
        published = nil
        invalidateAndPublish()
    }

    public func update(
        _ update: KeyboardSuggestionInputUpdate,
        canQuery: Bool,
        shift: ShiftState = .lowercase
    ) {
        let signpostID = PersonalSuggestionServiceSignposts.beginDispatch()
        defer {
            PersonalSuggestionServiceSignposts.endDispatch(
                signpostID,
                generation: generation
            )
        }
        if let token = update.completedToken, workerConfiguration.enabled {
            ensureWorker()?.learn(token, after: context)
        }
        context = update.context
        self.shift = shift
        let allowed = canQuery && workerConfiguration.enabled
        let next = allowed && update.prefix.count >= 2 ? update.prefix : ""
        // A prediction answers a context with no prefix at all. One character is
        // neither a prefix worth completing nor a word boundary, and stays out.
        let predicts = allowed && update.prefix.isEmpty && context != nil
        if update.completedToken == nil, next == prefix, predicts == predicting { return }
        generation &+= 1
        prefix = next
        predicting = predicts
        received = []
        publish([])
        guard !prefix.isEmpty || predicting else { return }
        ensureWorker()?.query(.init(prefix: prefix, generation: generation, context: context))
    }

    public func acceptance(
        for candidate: KeyboardSuggestionCandidate
    ) -> (String, String)? {
        // An empty prefix is the ordinary shape of accepting a prediction: it
        // replaces nothing and inserts a word.
        guard candidate.generation == generation,
              visibleCandidates.contains(candidate)
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
        let needsPublish = !prefix.isEmpty || predicting || !received.isEmpty
        generation &+= 1
        prefix = ""
        context = nil
        predicting = false
        received = []
        if needsPublish { publish([]) }
    }

    private func receive(
        request: PersonalSuggestionQueryRequest,
        candidates: [PersonalSuggestionCandidate]
    ) {
        guard workerConfiguration.enabled,
              request.generation == generation, request.prefix == prefix
        else { return }
        received = candidates
        let values = visibleCandidates
        publish(values)
        PersonalSuggestionServiceSignposts.recordUI(
            generation: generation,
            count: values.count
        )
    }

    func publish(_ candidates: [KeyboardSuggestionCandidate]) {
        // Prediction adds a bar update after every space, most of them one empty
        // list after another. Comparing here keeps that cost off UIKit.
        let texts = candidates.map(\.text)
        guard texts != published else { return }
        published = texts
        onCandidates?(activationGeneration, candidates)
    }
}
