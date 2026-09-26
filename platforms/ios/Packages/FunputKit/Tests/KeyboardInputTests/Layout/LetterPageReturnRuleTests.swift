#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import Testing

struct LetterPageReturnRuleTests {
    @Test(
        "Sentence and clause punctuation, and closing marks, lead back to a word",
        arguments: [
            ("Chào bạn!", true),
            ("Thật sao?", true),
            ("xong.", true),
            ("ví dụ,", true),
            ("gồm:", true),
            ("chờ…", true),
            ("(có)", true),
            ("\"trích\"", true),
            ("«ừ»", true),
        ]
    )
    func triggers(context: String, expected: Bool) {
        #expect(LetterPageReturnRule.appliesTo(contextBeforeInput: context) == expected)
    }

    @Test(
        "Digits, letters and opening marks keep the symbol page",
        arguments: ["10", "8:30", "xin chao", "(", "#", "@", "100%", "a ", ""]
    )
    func keepsPage(context: String) {
        #expect(!LetterPageReturnRule.appliesTo(contextBeforeInput: context))
    }

    @Test("An unknown context never switches pages")
    func nilContext() {
        #expect(!LetterPageReturnRule.appliesTo(contextBeforeInput: nil))
    }
}
#endif
