import os

enum PersonalSuggestionServiceSignposts {
    static let log = OSLog(
        subsystem: "app.funput.keyboard",
        category: "PersonalSuggestions"
    )

    static func beginDispatch() -> OSSignpostID {
        let id = OSSignpostID(log: log)
        os_signpost(.begin, log: log, name: "SuggestionDispatch", signpostID: id)
        return id
    }

    static func endDispatch(_ id: OSSignpostID, generation: UInt64) {
        os_signpost(
            .end, log: log, name: "SuggestionDispatch", signpostID: id,
            "generation=%{public}llu", generation
        )
    }

    static func recordWorkerCreated() {
        os_signpost(.event, log: log, name: "SuggestionWorkerCreated")
    }

    static func recordUI(generation: UInt64, count: Int) {
        os_signpost(
            .event, log: log, name: "SuggestionUI",
            "generation=%{public}llu count=%{public}d", generation, count
        )
    }
}
