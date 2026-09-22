import KeyboardLayout

public enum KeyboardAutocapitalizationMode: Equatable, Sendable {
    case none
    case words
    case sentences
    case allCharacters
}

public struct KeyboardInputContext: Equatable, Sendable {
    public let editorMode: KeyboardEditorMode
    public let enterAction: KeyboardEnterAction
    public let initialLayoutMode: KeyboardLayoutMode
    public let autocapitalization: KeyboardAutocapitalizationMode

    public init(
        editorMode: KeyboardEditorMode,
        enterAction: KeyboardEnterAction,
        initialLayoutMode: KeyboardLayoutMode = .letters,
        autocapitalization: KeyboardAutocapitalizationMode = .sentences
    ) {
        self.editorMode = editorMode
        self.enterAction = enterAction
        self.initialLayoutMode = initialLayoutMode
        self.autocapitalization = autocapitalization
    }
}

#if canImport(UIKit)
import UIKit

public enum KeyboardInputContextResolver {
    /// - Parameter autoCapitalizeEnabled: the user's "Tự viết hoa" preference. It
    ///   belongs here rather than deeper in the coordinator because the effective
    ///   capitalization mode is exactly what this type resolves; the coordinator then
    ///   stays a state machine driven by one context.
    public static func resolve(
        keyboardType: UIKeyboardType,
        returnKeyType: UIReturnKeyType,
        isSecureTextEntry: Bool,
        autocapitalizationType: UITextAutocapitalizationType = .sentences,
        autoCapitalizeEnabled: Bool = true
    ) -> KeyboardInputContext {
        let mode = editorMode(keyboardType: keyboardType, isSecure: isSecureTextEntry)
        return KeyboardInputContext(
            editorMode: mode,
            enterAction: enterAction(returnKeyType),
            initialLayoutMode: .letters,
            autocapitalization: autocapitalization(
                autocapitalizationType,
                editorMode: mode,
                enabled: autoCapitalizeEnabled
            )
        )
    }

    private static func editorMode(
        keyboardType: UIKeyboardType,
        isSecure: Bool
    ) -> KeyboardEditorMode {
        if isSecure {
            return keyboardType == .numberPad || keyboardType == .asciiCapableNumberPad
                ? .pin
                : .password
        }
        return switch keyboardType {
        case .URL: .url
        case .numbersAndPunctuation: .numberSignedDecimal
        case .numberPad, .asciiCapableNumberPad: .number
        case .phonePad, .namePhonePad: .phone
        case .emailAddress: .email
        case .decimalPad: .numberDecimal
        case .webSearch: .search
        default: .text
        }
    }

    private static func enterAction(_ type: UIReturnKeyType) -> KeyboardEnterAction {
        switch type {
        case .go: .go
        case .google: .custom("Google")
        case .join: .custom("Join")
        case .next: .next
        case .route: .custom("Route")
        case .search: .search
        case .send: .send
        case .yahoo: .custom("Yahoo")
        case .done: .done
        case .emergencyCall: .custom("Emergency")
        case .continue: .custom("Continue")
        default: .newLine
        }
    }

    /// Reading the four branches in order:
    ///
    /// 1. A field that is never prose — a password, an address, a number pad — is
    ///    left alone whatever anyone asked for.
    /// 2. A field demanding upper case wins even against a disabled preference: that
    ///    is a statement about what the field holds, not a convenience being offered.
    /// 3. The user's "Tự viết hoa" switch silences the two convenience modes.
    /// 4. Otherwise sentences, **including when the field asked for `.none`**.
    ///
    /// Branch 4 outranks the field on purpose, and it is the one place Funput goes
    /// further than the system keyboard. `.none` is common in text views that never
    /// meant it — a web input carrying `autocapitalize="off"`, a chat composer copied
    /// from a search field — and a user who turned the switch on is asking for prose
    /// to be capitalized in exactly those places. Branch 1 is what keeps that from
    /// reaching a field where it would be wrong; Android resolves the same way.
    private static func autocapitalization(
        _ type: UITextAutocapitalizationType,
        editorMode: KeyboardEditorMode,
        enabled: Bool
    ) -> KeyboardAutocapitalizationMode {
        guard editorMode.allowsAutoCapitalization else { return .none }
        if type == .allCharacters { return .allCharacters }
        guard enabled else { return .none }
        return type == .words ? .words : .sentences
    }
}
#endif
