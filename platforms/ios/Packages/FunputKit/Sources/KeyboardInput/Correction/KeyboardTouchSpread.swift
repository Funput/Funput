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

    /// Population standard deviation — the σ the scoring model is written in terms of.
    public var standardDeviation: Double {
        guard count > 1 else { return 0 }
        let variance = totalSquares / Double(count) - mean * mean
        return variance > 0 ? variance.squareRoot() : 0
    }
}
