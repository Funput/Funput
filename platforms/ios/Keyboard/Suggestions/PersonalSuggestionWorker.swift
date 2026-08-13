import Foundation
import FunputShared
import os
import PersonalSuggestions

final class PersonalSuggestionWorker: PersonalSuggestionWorking, @unchecked Sendable {
    typealias Query = PersonalSuggestionQueryRequest

    private let queue = DispatchQueue(label: "app.funput.keyboard.personal-suggestions")
    private let querySlot = PersonalSuggestionQuerySlot()
    private let onResult: @Sendable (Query, [PersonalSuggestionCandidate]) -> Void
    private let flushTimer: DispatchSourceTimer
    private var engine: PersonalSuggestionEngine?
    private var activeStoreURL: URL?
    private var enabled = true
    private var learnedSinceFlush = 0

    init(onResult: @escaping @Sendable (Query, [PersonalSuggestionCandidate]) -> Void) {
        self.onResult = onResult
        flushTimer = DispatchSource.makeTimerSource(queue: queue)
        flushTimer.setEventHandler { [weak self] in self?.flushWhenIdle() }
        flushTimer.activate()
    }

    deinit {
        flushTimer.cancel()
    }

    func configure(_ configuration: PersonalSuggestionWorkerConfiguration) {
        queue.async { [weak self] in
            self?.applyConfiguration(
                enabled: configuration.enabled,
                requestedURL: configuration.hasFullAccess ? Self.prepareStoreURL() : nil,
                resetToken: configuration.resetToken
            )
        }
    }

    func learn(_ token: String) {
        queue.async { [weak self] in
            guard let self, enabled else { return }
            ensureEngine()
            guard engine?.learn(token) == true else { return }
            learnedSinceFlush += 1
            if learnedSinceFlush >= 32 { flushWhenIdle() }
            else { flushTimer.schedule(deadline: .now() + 2) }
        }
    }

    func query(_ request: Query) {
        let shouldSchedule = querySlot.submit(request)
        if shouldSchedule { queue.async { [weak self] in self?.drainQueries() } }
    }

    func flush() {
        queue.async { [weak self] in self?.flushWhenIdle() }
    }

    private func applyConfiguration(enabled: Bool, requestedURL: URL?, resetToken: UUID?) {
        self.enabled = enabled
        if activeStoreURL != requestedURL || engine == nil {
            _ = engine?.flush()
            let persistent = requestedURL.flatMap(PersonalSuggestionEngine.open)
            activeStoreURL = persistent == nil ? nil : requestedURL
            engine = persistent ?? PersonalSuggestionEngine.inMemory()
        }
        let persistentReady = requestedURL == nil || activeStoreURL == requestedURL
        if persistentReady { applyResetIfNeeded(resetToken) }
        if !enabled { _ = engine?.flush() }
    }

    private func applyResetIfNeeded(_ token: UUID?) {
        guard let token else { return }
        let defaults = UserDefaults(suiteName: FunputAppGroup.identifier)
        let applied = defaults?.string(forKey: FunputAppGroup.personalSuggestionAppliedResetKey)
        guard applied != token.uuidString, engine?.reset() == true else { return }
        defaults?.set(token.uuidString, forKey: FunputAppGroup.personalSuggestionAppliedResetKey)
    }

    private func drainQueries() {
        while let request = querySlot.takeLatest() {
            let signpostID = OSSignpostID(log: PersonalSuggestionSignposts.log)
            os_signpost(
                .begin,
                log: PersonalSuggestionSignposts.log,
                name: "SuggestionQuery",
                signpostID: signpostID,
                "generation=%{public}llu",
                request.generation
            )
            ensureEngine()
            let result = enabled ? engine?.query(request.prefix) ?? [] : []
            os_signpost(
                .end,
                log: PersonalSuggestionSignposts.log,
                name: "SuggestionQuery",
                signpostID: signpostID,
                "generation=%{public}llu count=%{public}d",
                request.generation,
                result.count
            )
            let superseded = querySlot.hasNewer(than: request.generation)
            if !superseded { onResult(request, result) }
        }
    }

    private func ensureEngine() {
        if engine == nil { engine = PersonalSuggestionEngine.inMemory() }
    }

    private func flushWhenIdle() {
        guard !querySlot.hasPending else {
            flushTimer.schedule(deadline: .now() + .milliseconds(100))
            return
        }
        if engine?.flush() == true { learnedSinceFlush = 0 }
    }
}
