#if os(iOS) && canImport(FunputCore)
import FunputCore
import KeyboardLayout

extension FunputComposer {
    /// Report where the finger landed for the key about to be sent.
    ///
    /// The engine consumes this on the **next** `process(_:)`, whatever key that turns
    /// out to be, so it has to be called immediately before the keystroke it belongs
    /// to. A word with any key that arrived without one is left uncorrected entirely:
    /// half the evidence would be worse than none.
    ///
    /// Keys are named by the scalar their cap shows, in lower case. The engine matches
    /// the case of the key it actually received, so Shift needs no thought here.
    public func setNextKeyTouch(_ evidence: KeyboardTouchEvidence) {
        var touch = FunputKeyTouch(
            typed: evidence.typed.value,
            typed_distance: evidence.typedDistance,
            alternates: (0, 0, 0),
            distances: (0, 0, 0),
            alternate_count: 0
        )
        withUnsafeMutableBytes(of: &touch.alternates) { keys in
            withUnsafeMutableBytes(of: &touch.distances) { distances in
                let keys = keys.bindMemory(to: UInt32.self)
                let distances = distances.bindMemory(to: Float.self)
                var slot = 0
                evidence.forEachAlternate { alternate in
                    guard slot < keys.count else { return }
                    keys[slot] = alternate.scalar.value
                    distances[slot] = alternate.distance
                    slot += 1
                }
                touch.alternate_count = UInt32(slot)
            }
        }
        withUnsafePointer(to: &touch) { funput_engine_set_next_key_touch(handle, $0) }
    }

    /// Switch typo correction on or off. Off, the engine ignores touch reports
    /// entirely and every keystroke behaves exactly as it did before the feature
    /// existed.
    public func setTypoCorrection(_ enabled: Bool) {
        funput_set_typo_correction(handle, enabled)
    }
}
#endif
