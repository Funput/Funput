import Foundation
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer

@MainActor
public final class PersonalSuggestionService {
    public var onCandidates: ((UInt64, [KeyboardSuggestionCandidate]) -> Void)?
    private let workerFactory: PersonalSuggestionWorkerFactory
    var worker: (any PersonalSuggestionWorking)?
    var workerConfiguration = PersonalSuggestionWorkerConfiguration(
        enabled: false,
        hasFullAccess: false,
        resetToken: nil
    )
    private(set) var generation: UInt64 = 0
    private(set) var activationGeneration: UInt64 = 0
    private(set) var prefix = ""
    /// The tracker's last update: what the next word follows, and the words the bar asks after.
    private var latest = KeyboardSuggestionInputUpdate.empty
    /// Whether the bar is currently answering a context rather than a prefix.
    private var predicting = false
    /// As the engine returned them. `visibleCandidates` puts them in the user's case.
    var received: [PersonalSuggestionCandidate] = []
    /// The generation `received` answered. It trails `generation` while the answer to
    /// a newer question is on its way: the words stay on the bar and take no tap.
    private(set) var answered: UInt64 = 0
    var shift: ShiftState = .lowercase
    /// What typo correction says about the word under the caret; drawn ahead of
    /// the stored words. See `SuggestionBarComposer`.
    var correction: CorrectionPreview?
    /// Last drawn, by kind, text and the generation it answered; `nil` after a
    /// reconfigure, so reactivation always redraws.
    var published: [String]?

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
            resetToken: resetToken,
            seedLanguage: workerConfiguration.seedLanguage
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
            ensureWorker()?.learn(token, after: latest.learnContext)
        }
        // The host can move the caret to other words with no key typed: then the
        // same empty prefix asks anew.
        let sameWords = update.queryContext == latest.queryContext
        latest = update
        self.shift = shift
        let allowed = canQuery && workerConfiguration.enabled
        let next = allowed && update.prefix.count >= 2 ? update.prefix : ""
        // A prediction answers a context with no prefix at all. One character is
        // neither a prefix worth completing nor a word boundary, and stays out.
        let predicts = allowed && update.prefix.isEmpty && latest.context != nil
        if update.completedToken == nil, next == prefix, predicts == predicting, sameWords { return }
        prefix = next
        predicting = predicts
        requery()
    }

    /// Asks again for what the bar is answering. What it showed stays up until the
    /// answer lands, so the bar does not blink between keystrokes; with nothing to
    /// ask, or nobody to ask, it is dropped at once.
    func requery() {
        generation &+= 1
        guard !prefix.isEmpty || predicting, let worker = ensureWorker() else {
            received = []
            return publish([])
        }
        // The kept words still carry the generation they answered, so a tap on one
        // is refused; the correction slots are stamped for this one.
        publish(visibleCandidates)
        worker.query(.init(prefix: prefix, generation: generation, context: latest.queryContext))
    }

    public func clear() { invalidateAndPublish() }
    public func flush() { worker?.flush() }

    func ensureWorker() -> (any PersonalSuggestionWorking)? {
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
            || correction != nil
        generation &+= 1
        prefix = ""
        latest = .empty
        predicting = false
        received = []
        correction = nil
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
        answered = generation
        let values = visibleCandidates
        publish(values)
        PersonalSuggestionServiceSignposts.recordUI(
            generation: generation,
            count: values.count
        )
    }
}
