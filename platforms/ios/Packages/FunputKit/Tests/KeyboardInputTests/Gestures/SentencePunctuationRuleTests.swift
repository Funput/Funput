#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import Testing

struct SentencePunctuationRuleTests {
    @Test(
        "A trailing space after a word or a digit is punctuable",
        arguments: [
            ("xin chao ", true),
            ("chào ", true),
            ("3 ", true),
            ("xin chao", false),
            ("xin chao  ", false),
            ("xin chao. ", false),
            ("xin chao, ", false),
            ("( ", false),
            ("\n", false),
            ("", false),
        ]
    )
    func rule(context: String, expected: Bool) {
        #expect(SentencePunctuationRule.appliesTo(contextBeforeInput: context) == expected)
    }

    @Test("An unknown context never punctuates")
    func nilContext() {
        #expect(!SentencePunctuationRule.appliesTo(contextBeforeInput: nil))
    }
}
#endif
