import Foundation

/// A context Funput authored, and when it did so.
struct KeyboardAuthoredContext {
    let context: String
    let recordedAt: TimeInterval
}

/// A generation of authored contexts, closed at word boundaries.
struct KeyboardEchoEpoch {
    /// How long an authored context keeps acknowledging host callbacks.
    ///
    /// Content alone cannot separate a late echo from an external edit that happens to land
    /// on a context Funput itself authored: an app clearing its field reports the very same
    /// empty context Funput saw before the word started, and mid-word deletions stage every
    /// intermediate prefix. Promptness is what actually separates them — a host echoes within
    /// a runloop turn, while an external edit follows a human action.
    static let echoLifetime: TimeInterval = 0.35

    let generation: UInt64
    var contexts: [KeyboardAuthoredContext] = []

    func acknowledges(_ context: String, notBefore horizon: TimeInterval) -> Bool {
        contexts.contains { $0.context == context && $0.recordedAt >= horizon }
    }
}
