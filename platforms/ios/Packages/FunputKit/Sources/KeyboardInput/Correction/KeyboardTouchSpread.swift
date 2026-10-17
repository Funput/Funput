import Foundation

/// How far the user's touches land from the centres of the keys they hit, in key
/// pitches.
///
/// The one number the whole feature turns on, and the one nobody has measured. The
/// simulated corpus says corrections are effectively free of harm at a spread of
/// 0.20, worth their cost at 0.25, and not worth shipping at 0.30 — see
/// `docs/features/typo-correction.md` §12.1. Real fingers have never been measured
/// against that scale, so the keyboard counts as it goes.
///
/// Counts only: a distance, never a key and never a word.
public struct KeyboardTouchSpread: Sendable, Equatable {
    public private(set) var count = 0
    private var total = 0.0
    private var totalSquares = 0.0

    public init() {}

    public mutating func record(_ distance: Float) {
        let distance = Double(distance)
        count += 1
        total += distance
        totalSquares += distance * distance
    }

    public var mean: Double {
        count > 0 ? total / Double(count) : 0
    }

    /// Population standard deviation of the distances themselves.
    public var standardDeviation: Double {
        guard count > 1 else { return 0 }
        let variance = totalSquares / Double(count) - mean * mean
        return variance > 0 ? variance.squareRoot() : 0
    }

    /// The spread **per axis** — the σ everything else in this feature is written in
    /// terms of, from the engine's scoring constant to the harness's `--noise`.
    ///
    /// What is measured here is the distance from a key's centre, which is not the
    /// same quantity: a touch scattered by σ in each direction lands, on average,
    /// about 1.25σ away. For a two-dimensional Gaussian those distances are Rayleigh
    /// distributed, whose best estimate of σ is `√(Σd² / 2n)` — exact, and no
    /// constant to misremember. Reporting the raw mean instead is how a reader ends
    /// up comparing 0.22 against a 0.25 that means something else.
    public var perAxisSigma: Double {
        guard count > 0 else { return 0 }
        return (totalSquares / (2 * Double(count))).squareRoot()
    }

    /// Start again, so one session's numbers are never counted twice.
    public mutating func reset() {
        self = KeyboardTouchSpread()
    }
}
