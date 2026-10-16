#if canImport(UIKit)
import Testing
import UIKit

@testable import KeyboardInput

/// Which fields typo correction may touch, decided from their traits.
struct CorrectionFieldPolicyTests {
    private func allows(
        autocorrection: UITextAutocorrectionType = .default,
        keyboard: UIKeyboardType = .default,
        content: UITextContentType? = nil
    ) -> Bool {
        CorrectionFieldPolicy.allowsAutocorrect(
            autocorrectionType: autocorrection, keyboardType: keyboard, textContentType: content
        )
    }

    @Test("An ordinary text field is corrected")
    func ordinary() {
        #expect(allows())
        #expect(allows(autocorrection: .yes, content: .name))
    }

    @Test("A field that asked for no autocorrect gets none")
    func declined() {
        #expect(!allows(autocorrection: .no))
    }

    @Test("ASCII-only fields hold codes and handles, not words")
    func asciiOnly() {
        #expect(!allows(keyboard: .asciiCapable))
        #expect(!allows(keyboard: .asciiCapableNumberPad))
        #expect(allows(keyboard: .twitter), "other keyboard types are judged by their content")
    }

    @Test("Credentials and addresses are never rewritten")
    func protectedContent() {
        for content in [UITextContentType.username, .password, .newPassword, .oneTimeCode, .URL, .emailAddress] {
            #expect(!allows(content: content), "\(content.rawValue)")
        }
    }
}
#endif
