#if os(iOS) && canImport(FunputCore)
import FunputCore

/// Where a sentence begins, answered by the rules every Funput platform shares.
///
/// Stateless, so unlike ``FunputComposer`` there is no handle to own and nothing to
/// confine to an actor. The keyboard asks on every caret move rather than keeping a
/// running answer: a caret also moves for a paste, a tap elsewhere, or a field that
/// opened with text already in it, none of which a keystroke explains.
public enum FunputSentence {
    /// Whether a caret sitting after `before` starts a sentence.
    ///
    /// A repeated full stop reads as an abbreviation, so `giấy tờ v.v. ` does not
    /// start one while `TS. ` does. A keyboard commits its capital before the user
    /// can see it, which is why it takes that guard and the bulk case transform in
    /// the Chuyển mã window does not.
    public static func startsSentence(_ before: String) -> Bool {
        ask(before, funput_starts_sentence)
    }

    /// Whether a caret sitting after `before` starts a word: true at the start of
    /// the document and after anything that is not a letter or a digit.
    public static func startsWord(_ before: String) -> Bool {
        ask(before, funput_starts_word)
    }

    /// The rules never read further back than this, and the caret context a proxy
    /// hands over can be a whole paragraph. Truncating is safe because the scan only
    /// stays open for a sentence across characters that are not letters or digits,
    /// so a cut landing mid-word gives the same answer as the full text would.
    private static let lookback = 256

    private static func ask(
        _ before: String,
        _ door: (UnsafePointer<UInt32>?, UInt) -> Bool
    ) -> Bool {
        let scalars = before.unicodeScalars.suffix(lookback).map(\.value)
        return scalars.withUnsafeBufferPointer { buffer in
            door(buffer.baseAddress, UInt(buffer.count))
        }
    }
}
#endif
