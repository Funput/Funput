#if os(iOS) && canImport(UIKit)
import KeyboardInput
import Testing
import UIKit

struct KeyboardAutoCapitalizePreferenceTests {
    @Test(
        "The preference silences the convenience modes",
        arguments: [UITextAutocapitalizationType.sentences, .words]
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

    @Test("A field opting out stays opted out when the preference is on")
    func noneStaysNone() {
        #expect(resolve(.none, enabled: true).autocapitalization == .none)
    }

    private func resolve(
        _ type: UITextAutocapitalizationType,
        enabled: Bool
    ) -> KeyboardInputContext {
        KeyboardInputContextResolver.resolve(
            keyboardType: .default,
            returnKeyType: .default,
            isSecureTextEntry: false,
            autocapitalizationType: type,
            autoCapitalizeEnabled: enabled
        )
    }
}
#endif
