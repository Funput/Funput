#if os(iOS) && canImport(FunputCore)
import FunputEngine
import Testing

/// The rules live in `funput_core::sentence` and are tested there. These assert the
/// Swift door reaches them — a real call into the linked Rust library, so a broken
/// marshalling or a stale XCFramework shows up here rather than as a keyboard that
/// quietly stops capitalizing.
struct FunputSentenceTests {
    @Test("A terminator needs the whitespace that confirms it")
    func terminatorNeedsWhitespace() {
        #expect(FunputSentence.startsSentence("Xin chào. "))
        #expect(!FunputSentence.startsSentence("Xin chào."))
        #expect(!FunputSentence.startsSentence("Xin chào"))
    }

    @Test("Every terminator ends a sentence", arguments: ["Thật. ", "Thật! ", "Thật? ", "Thật… "])
    func everyTerminator(context: String) {
        #expect(FunputSentence.startsSentence(context))
    }

    @Test("The start of the document and a new line both start a sentence")
    func documentStartAndNewline() {
        #expect(FunputSentence.startsSentence(""))
        #expect(FunputSentence.startsSentence("một\n"))
    }

    @Test("A decimal point is not an ending")
    func decimalPoint() {
        #expect(!FunputSentence.startsSentence("giá 1.5 "))
    }

    @Test("A closing quote does not hide the ending behind it")
    func closersAreTransparent() {
        #expect(FunputSentence.startsSentence("nói \"Xin chào.\" "))
        #expect(FunputSentence.startsSentence("(Xong.) "))
    }

    @Test("A repeated full stop reads as an abbreviation")
    func abbreviationGuard() {
        #expect(!FunputSentence.startsSentence("giấy tờ v.v. "))
        // No earlier dot gives `TS.` away, so it stays indistinguishable from an ending.
        #expect(FunputSentence.startsSentence("TS. "))
    }

    @Test("Words begin after anything that is not a letter or a digit")
    func words() {
        #expect(FunputSentence.startsWord(""))
        #expect(FunputSentence.startsWord("Xin "))
        #expect(!FunputSentence.startsWord("Xin chà"))
        #expect(!FunputSentence.startsWord("3"))
    }

    /// The door only forwards the tail of a long context. A cut can only land on a
    /// letter or a digit mid-word, which closes the scan exactly as the full text
    /// would, so the answer is the same either way.
    @Test("A context longer than the lookback still answers for its tail")
    func longContextIsTruncatedSafely() {
        let filler = String(repeating: "a", count: 400)

        #expect(FunputSentence.startsSentence(filler + ". "))
        #expect(!FunputSentence.startsSentence(filler))
    }
}
#endif
