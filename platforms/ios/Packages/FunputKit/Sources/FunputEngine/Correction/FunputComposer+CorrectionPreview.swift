#if os(iOS) && canImport(FunputCore)
import FunputCore

/// The engine calls that look at a word before it ends — see ``CorrectionPreviewing``.
extension FunputComposer: CorrectionPreviewing {
    /// Search the word being typed the way the boundary will, and say how many
    /// candidates it reaches. An explicit call: `process(_:)` never runs this
    /// search on its own, so what the bar costs is what the host asks for.
    public func previewCorrection() -> Int {
        Int(funput_engine_preview_correction(handle))
    }

    /// Rank the candidates in hand with the host's prior folded in. Counts no
    /// metric, so the bar may ask as often as it redraws.
    public func rankCorrection(uses: [UInt32], allowed: [Bool]) -> FunputCorrectionRanking {
        let count = max(uses.count, allowed.count)
        var uses = uses
        var allowed = allowed
        uses.append(contentsOf: repeatElement(0, count: count - uses.count))
        allowed.append(contentsOf: repeatElement(true, count: count - allowed.count))
        let raw = uses.withUnsafeBufferPointer { uses in
            allowed.withUnsafeBufferPointer { allowed in
                funput_engine_rank_correction(
                    handle, uses.baseAddress, allowed.baseAddress, UInt(count)
                )
            }
        }
        return FunputCorrectionRanking(
            best: raw.best < 0 ? nil : Int(raw.best),
            second: raw.second < 0 ? nil : Int(raw.second),
            verdict: FunputCorrectionRanking.Verdict(rawValue: raw.verdict) ?? .refused
        )
    }

    /// The user tapped the word exactly as typed: drop the preview, any parked
    /// correction and any armed undo, and do not offer this spelling again the
    /// next time it is typed.
    public func refuseCorrection() {
        funput_engine_refuse_correction(handle)
    }
}
#endif
