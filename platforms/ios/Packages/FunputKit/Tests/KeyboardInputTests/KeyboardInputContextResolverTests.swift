#if os(iOS) && canImport(UIKit)
import KeyboardInput
import KeyboardLayout
import Testing
import UIKit

struct KeyboardInputContextResolverTests {
    @Test("UIKit keyboard types map to editor and initial layout modes")
    func keyboardTypes() {
        let cases: [(UIKeyboardType, KeyboardEditorMode, KeyboardLayoutMode)] = [
            (.default, .text, .letters),
            (.asciiCapable, .text, .letters),
            (.numbersAndPunctuation, .numberSignedDecimal, .letters),
            (.URL, .url, .letters),
            (.numberPad, .number, .letters),
            (.phonePad, .phone, .letters),
            (.namePhonePad, .phone, .letters),
            (.emailAddress, .email, .letters),
            (.decimalPad, .numberDecimal, .letters),
            (.twitter, .text, .letters),
            (.webSearch, .search, .letters),
            (.asciiCapableNumberPad, .number, .letters),
        ]

        for (keyboardType, editorMode, layoutMode) in cases {
            let context = resolve(keyboardType: keyboardType)
            #expect(context.editorMode == editorMode)
            #expect(context.initialLayoutMode == layoutMode)
        }
    }

    @Test("Secure traits select password or PIN policies")
    func secureTypes() {
        #expect(resolve(keyboardType: .default, secure: true).editorMode == .password)
        #expect(resolve(keyboardType: .numberPad, secure: true).editorMode == .pin)
    }

    @Test("Autocapitalization traits are preserved for later state synchronization")
    func autocapitalization() {
        let context = KeyboardInputContextResolver.resolve(
            keyboardType: .default,
            returnKeyType: .default,
            isSecureTextEntry: false,
            autocapitalizationType: .allCharacters
        )

        #expect(context.autocapitalization == .allCharacters)
    }

    @Test("UIKit return key types map to presentation actions")
    func returnKeys() {
        let cases: [(UIReturnKeyType, KeyboardEnterAction)] = [
            (.default, .newLine), (.go, .go), (.google, .custom("Google")),
            (.join, .custom("Join")), (.next, .next), (.route, .custom("Route")),
            (.search, .search), (.send, .send), (.yahoo, .custom("Yahoo")),
            (.done, .done), (.emergencyCall, .custom("Emergency")),
            (.continue, .custom("Continue")),
        ]

        for (returnKeyType, action) in cases {
            #expect(resolve(returnKeyType: returnKeyType).enterAction == action)
        }
    }

    private func resolve(
        keyboardType: UIKeyboardType = .default,
        returnKeyType: UIReturnKeyType = .default,
        secure: Bool = false
    ) -> KeyboardInputContext {
        KeyboardInputContextResolver.resolve(
            keyboardType: keyboardType,
            returnKeyType: returnKeyType,
            isSecureTextEntry: secure
        )
    }
}
#endif
