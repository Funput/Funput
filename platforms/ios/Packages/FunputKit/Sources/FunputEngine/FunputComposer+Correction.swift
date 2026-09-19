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

    /// Whether the last keystroke ended a word on a correction waiting for an answer.
    public var hasPendingCorrection: Bool {
        funput_engine_has_pending_correction(handle)
    }

    /// How many characters applying a candidate will delete: the word as the app
    /// shows it, plus the boundary character the host has already echoed.
    public var pendingCorrectionBackspace: Int {
        Int(funput_engine_pending_correction_backspace(handle))
    }

    /// The words the parked correction can reach, best touch score first.
    public func correctionCandidates() -> [FunputCorrectionCandidateText] {
        var raw = [FunputCorrectionCandidate](
            repeating: FunputCorrectionCandidate(),
            count: Int(CORRECTION_CAP)
        )
        let count = raw.withUnsafeMutableBufferPointer { buffer in
            funput_engine_correction_candidates(handle, buffer.baseAddress, UInt(buffer.count))
        }
        return raw.prefix(Int(count)).map(FunputCorrectionCandidateText.init(decoding:))
    }

    /// Rank the parked candidates and name the winner, or `nil` when the top two are
    /// too close to call — a pair to offer rather than an edit to make.
    ///
    /// `allowed` says which candidates the host's dictionary recognizes. A refused
    /// candidate cannot win, but it still competes for the confidence margin, so a
    /// dictionary that knows too little corrects less rather than corrects badly.
    public func chooseCorrection(uses: [UInt32], allowed: [Bool]) -> Int? {
        let count = max(uses.count, allowed.count)
        var uses = uses
        var allowed = allowed
        uses.append(contentsOf: repeatElement(0, count: count - uses.count))
        allowed.append(contentsOf: repeatElement(true, count: count - allowed.count))
        let chosen = uses.withUnsafeBufferPointer { uses in
            allowed.withUnsafeBufferPointer { allowed in
                funput_engine_choose_correction(
                    handle, uses.baseAddress, allowed.baseAddress, UInt(count)
                )
            }
        }
        return chosen < 0 ? nil : Int(chosen)
    }

    /// Answer the parked correction. Passing `nil` declines it — which is not always
    /// a no-op: it is the word boundary finishing the English restore it deferred.
    public func applyCorrection(_ index: Int?) -> FunputCompositionResult {
        let result = funput_engine_apply_correction(handle, index.map(Int32.init) ?? -1)
        return FunputCompositionResult(
            action: FunputCompositionAction(rawValue: result.action) ?? .none,
            deleteCount: Int(result.backspace),
            text: FunputResultDecoder.output(from: result)
        )
    }

    /// Whether Backspace would undo the last correction rather than delete a
    /// character. A host that passes the key through must ask before it does.
    public var hasCorrectionUndo: Bool {
        funput_engine_has_correction_undo(handle)
    }

    /// What typo correction has done this session. Counts only — never a word.
    public var correctionMetrics: FunputCorrectionCounts {
        let raw = funput_engine_correction_metrics(handle)
        return FunputCorrectionCounts(
            applied: Int(raw.applied),
            reverted: Int(raw.reverted),
            skippedAmbiguous: Int(raw.skipped_ambiguous),
            candidatesMax: Int(raw.candidates_max),
            microsecondsMax: Int(raw.microseconds_max)
        )
    }

    /// Switch typo correction on or off. Off, the engine ignores touch reports
    /// entirely and every keystroke behaves exactly as it did before the feature
    /// existed.
    public func setTypoCorrection(_ enabled: Bool) {
        funput_set_typo_correction(handle, enabled)
    }
}

extension FunputCorrectionCandidateText {
    /// Decode one candidate out of the C struct, whose `chars` arrives as a tuple.
    init(decoding candidate: FunputCorrectionCandidate) {
        var candidate = candidate
        let text = withUnsafePointer(to: &candidate.chars) { pointer in
            pointer.withMemoryRebound(to: UInt32.self, capacity: Int(CORRECTION_CHARS_CAP)) {
                chars in
                String(
                    String.UnicodeScalarView(
                        UnsafeBufferPointer(start: chars, count: Int(candidate.count))
                            .compactMap(Unicode.Scalar.init)
                    )
                )
            }
        }
        self.init(text: text, touchScore: candidate.touch_score, edits: Int(candidate.edits))
    }
}
#endif
