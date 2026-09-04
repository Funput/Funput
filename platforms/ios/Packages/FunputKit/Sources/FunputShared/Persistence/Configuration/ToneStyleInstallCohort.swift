import Foundation

/// Which tone placement an install starts on when nothing has been stored yet.
///
/// Modern placement became the product default in the release that added this
/// type. New installs get it; anyone already typing here keeps the placement
/// their fingers know, because a tone that moves mid-word during an upgrade
/// reads as a broken keyboard rather than a new default.
public enum ToneStyleInstallCohort: String, Sendable {
    /// Funput has run on this device since before modern placement shipped.
    case legacy
    /// Nothing preceded this install.
    case modern

    public var toneStyle: ToneStyleOption {
        switch self {
        case .legacy: .traditional
        case .modern: .modern
        }
    }
}

/// Decides an install's ``ToneStyleInstallCohort`` once and writes it down.
///
/// The decision is recorded rather than recomputed because the evidence it rests
/// on is circumstantial and only accumulates: asking again a month later would
/// answer `.legacy` for someone this store already called `.modern`, and the
/// placement would move under them exactly when it was supposed to be settled.
public struct ToneStyleInstallCohortStore {
    /// Keys only an earlier run of Funput can have written.
    ///
    /// The configuration payload is deliberately not among them — this store is
    /// consulted precisely when that key is missing. Someone who has granted Full
    /// Access, picked an emoji or kaomoji, built a theme, or reset the personal
    /// lexicon has left one of these behind. Someone who has done none of those
    /// since installing reads as a new install and moves to modern placement: the
    /// one gap this evidence cannot close, and the reason the answer is written
    /// down the first time it is asked for.
    public static let priorStateKeys = [
        FunputAppGroup.observedFullAccessKey,
        FunputAppGroup.emojiRecentsKey,
        FunputAppGroup.kaomojiRecentsKey,
        FunputAppGroup.customThemesKey,
        FunputAppGroup.personalSuggestionAppliedResetKey,
    ]

    private let defaults: UserDefaults
    private let hasPriorState: () -> Bool

    /// - Parameter hasPriorState: overridable so tests can drive both cohorts
    ///   without staging one of the ``priorStateKeys`` by hand.
    public init(defaults: UserDefaults, hasPriorState: (() -> Bool)? = nil) {
        self.defaults = defaults
        self.hasPriorState = hasPriorState ?? {
            Self.priorStateKeys.contains { defaults.object(forKey: $0) != nil }
        }
    }

    /// The recorded cohort, deciding and persisting it on the first call.
    public func cohort() -> ToneStyleInstallCohort {
        if let recorded = defaults.string(forKey: FunputAppGroup.toneStyleCohortKey)
            .flatMap(ToneStyleInstallCohort.init(rawValue:)) {
            return recorded
        }
        let resolved: ToneStyleInstallCohort = hasPriorState() ? .legacy : .modern
        defaults.set(resolved.rawValue, forKey: FunputAppGroup.toneStyleCohortKey)
        return resolved
    }
}
