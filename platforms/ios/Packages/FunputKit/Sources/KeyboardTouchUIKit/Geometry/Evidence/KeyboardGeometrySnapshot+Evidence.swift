import CoreGraphics
import KeyboardLayout

extension KeyboardGeometrySnapshot {
    /// What the keys around `point` say about a touch that resolved to `typed`.
    ///
    /// Deliberately **not** part of `touchHit(at:)`: that runs on every moved sample
    /// at the screen's refresh rate, while this is wanted once, for the key a lift
    /// actually commits. Doing it here keeps a four-neighbour search and its distance
    /// arithmetic off the tracking path entirely.
    ///
    /// Unlike the hit test this does not narrow to a row band. A row band exists so a
    /// finger between rows still produces the key it looks like it is on; a slip up or
    /// down (`g`/`t`, `g`/`b`) is exactly the mistake this evidence is for, so the
    /// search spans the whole keyboard.
    ///
    /// `nil` when there is no pitch to measure in, or when the touch is so far from
    /// everything that reporting it would be noise — a lift outside the keyboard, say.
    public func touchEvidence(at point: CGPoint, typed: KeySpec) -> KeyboardTouchEvidence? {
        let pitch = geometry.pitch
        guard let scalar = Self.scalar(of: typed), pitch.width > 0, pitch.height > 0 else {
            return nil
        }
        var ranked = RankedNeighbours()
        var typedDistance: Float?
        for key in characterKeys {
            let distance = self.distance(from: point, toCentreOf: key.frame)
            if key.spec.id == typed.id {
                typedDistance = distance
                continue
            }
            if distance <= KeyboardTouchEvidence.plausibleDistance,
               let scalar = Self.scalar(of: key.spec) {
                ranked.offer(.init(scalar: scalar, distance: distance))
            }
        }
        guard let typedDistance else { return nil }
        return KeyboardTouchEvidence(
            typed: scalar,
            typedDistance: typedDistance,
            first: ranked.first,
            second: ranked.second,
            third: ranked.third
        )
    }

    /// Straight-line distance to a key's centre, measured in pitches. The hit test
    /// measures to the frame's *edge*, which is zero anywhere inside a key and so says
    /// nothing about where in the key the finger landed — which is the whole question
    /// here.
    private func distance(from point: CGPoint, toCentreOf frame: CGRect) -> Float {
        let dx = (point.x - frame.midX) / geometry.pitch.width
        let dy = (point.y - frame.midY) / geometry.pitch.height
        return Float((dx * dx + dy * dy).squareRoot())
    }

    /// The scalar a key's cap stands for, lower-cased, or `nil` for a key that types
    /// nothing a correction could use.
    static func scalar(of key: KeySpec) -> Unicode.Scalar? {
        guard key.role == .character || key.role == .vniModifier else { return nil }
        let lowered = key.label.lowercased().unicodeScalars
        guard lowered.count == 1, let scalar = lowered.first else { return nil }
        return scalar
    }
}

/// The three nearest neighbours, kept by insertion rather than by sorting — no array,
/// no allocation, and the list is too short for anything cleverer to pay.
private struct RankedNeighbours {
    var first: KeyboardTouchEvidence.Alternate?
    var second: KeyboardTouchEvidence.Alternate?
    var third: KeyboardTouchEvidence.Alternate?

    mutating func offer(_ candidate: KeyboardTouchEvidence.Alternate) {
        if first == nil || candidate.distance < first!.distance {
            (first, second, third) = (candidate, first, second)
        } else if second == nil || candidate.distance < second!.distance {
            (second, third) = (candidate, second)
        } else if third == nil || candidate.distance < third!.distance {
            third = candidate
        }
    }
}
