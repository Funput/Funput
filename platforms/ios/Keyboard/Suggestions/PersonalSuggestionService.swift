import Foundation
import KeyboardInput
import KeyboardRenderer
import os
import PersonalSuggestions

@MainActor
final class PersonalSuggestionService {
    var onCandidates: (([KeyboardSuggestionCandidate]) -> Void)?

    private lazy var worker = PersonalSuggestionWorker { [weak self] request, candidates in
        DispatchQueue.main.async {
            self?.receive(request: request, candidates: candidates)
        }
    }
    private var generation: UInt64 = 0
    private var prefix = ""
    private var visibleCandidates: [KeyboardSuggestionCandidate] = []
    private var enabled = true

    func configure(enabled: Bool, hasFullAccess: Bool, resetToken: UUID?) {
        self.enabled = enabled
        worker.configure(
            enabled: enabled,
            hasFullAccess: hasFullAccess,
            resetToken: resetToken
        )
        if !enabled { clear() }
    }

    func update(_ update: KeyboardSuggestionInputUpdate, canQuery: Bool) {
        let signpostID = OSSignpostID(log: PersonalSuggestionSignposts.log)
        os_signpost(
            .begin,
            log: PersonalSuggestionSignposts.log,
            name: "SuggestionDispatch",
            signpostID: signpostID
        )
        defer {
            os_signpost(
                .end,
                log: PersonalSuggestionSignposts.log,
                name: "SuggestionDispatch",
                signpostID: signpostID,
                "generation=%{public}llu",
                generation
            )
        }
        if let token = update.completedToken, enabled {
            worker.learn(token)
        }
        let nextPrefix = canQuery && enabled && update.prefix.count >= 2 ? update.prefix : ""
        if update.completedToken == nil, nextPrefix == prefix { return }
        generation &+= 1
        prefix = nextPrefix
        visibleCandidates = []
        onCandidates?([])
        guard !prefix.isEmpty else { return }
        worker.query(.init(prefix: prefix, generation: generation))
    }

    func acceptance(for candidate: KeyboardSuggestionCandidate) -> (String, String)? {
        guard candidate.generation == generation,
              visibleCandidates.contains(candidate),
              !prefix.isEmpty else { return nil }
        return (prefix, candidate.text)
    }

    func clear() {
        generation &+= 1
        prefix = ""
        visibleCandidates = []
        onCandidates?([])
    }

    func flush() {
        worker.flush()
    }

    private func receive(
        request: PersonalSuggestionWorker.Query,
        candidates: [PersonalSuggestionCandidate]
    ) {
        guard enabled, request.generation == generation, request.prefix == prefix else { return }
        let values = candidates.map {
            KeyboardSuggestionCandidate(
                text: PersonalSuggestionCasing.apply($0.text, matching: prefix),
                generation: generation
            )
        }
        visibleCandidates = values
        onCandidates?(values)
        os_signpost(
            .event,
            log: PersonalSuggestionSignposts.log,
            name: "SuggestionUI",
            "generation=%{public}llu count=%{public}d",
            generation,
            values.count
        )
    }
}
