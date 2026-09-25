#if os(iOS) && canImport(UIKit)
import KeyboardInput
import Testing
import UIKit

struct KeyboardAutoCapitalizePreferenceTests {
    @Test(
        "The preference silences the convenience modes",
        arguments: [UITextAutocapitalizationType.sentences, .words, .none]
    )
    func disabledSilencesConvenienceModes(type: UITextAutocapitalizationType) {
        #expect(resolve(type, enabled: true).autocapitalization != .none)
        #expect(resolve(type, enabled: false).autocapitalization == .none)
    }

    @Test("A field demanding all caps is honoured whatever the preference says")
    func allCharactersSurvives() {
        #expect(resolve(.allCharacters, enabled: true).autocapitalization == .allCharacters)
        #expect(resolve(.allCharacters, enabled: false).autocapitalization == .allCharacters)
    }

    /// `.none` is common in fields that never meant it — a web input carrying
    /// `autocapitalize="off"`, a composer copied from a search field — so the user's
    /// switch outranks it. Which fields that can reach is `allowsAutoCapitalization`.
    @Test("A field opting out is overruled by the preference")
    func noneIsOverruled() {
        #expect(resolve(.none, enabled: true).autocapitalization == .sentences)
    }

    @Test(
        "Fields that never hold prose are left alone",
        arguments: [
            UIKeyboardType.emailAddress,
            .URL,
            .numberPad,
            .phonePad,
            .decimalPad,
        ]
    )
    func nonProseFieldsStayOut(keyboardType: UIKeyboardType) {
        let context = resolve(.sentences, enabled: true, keyboardType: keyboardType)

        #expect(context.autocapitalization == .none)
    }

    @Test("A secure field is left alone even when it demands all caps")
    func secureFieldStaysOut() {
        #expect(resolve(.allCharacters, enabled: true, isSecure: true).autocapitalization == .none)
    }

    @Test("Search fields hold prose, so they still capitalize")
    func searchFieldsCapitalize() {
        let context = resolve(.none, enabled: true, keyboardType: .webSearch)

        #expect(context.autocapitalization == .sentences)
    }

    private func resolve(
        _ type: UITextAutocapitalizationType,
        enabled: Bool,
        keyboardType: UIKeyboardType = .default,
        isSecure: Bool = false
    ) -> KeyboardInputContext {
        KeyboardInputContextResolver.resolve(
            keyboardType: keyboardType,
            returnKeyType: .default,
            isSecureTextEntry: isSecure,
            autocapitalizationType: type,
            autoCapitalizeEnabled: enabled
        )
    }
}
#endif
