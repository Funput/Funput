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
        KeyboardInputContext(
            editorMode: editorMode(
                keyboardType: keyboardType,
                isSecure: isSecureTextEntry
            ),
            enterAction: enterAction(returnKeyType),
            initialLayoutMode: .letters,
            autocapitalization: autocapitalization(
                autocapitalizationType,
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

    /// A disabled preference silences the convenience modes but not `allCharacters`:
    /// a field that insists on upper case is stating a requirement about its content,
    /// not offering something the user asked to turn off.
    private static func autocapitalization(
        _ type: UITextAutocapitalizationType,
        enabled: Bool
    ) -> KeyboardAutocapitalizationMode {
        let mode: KeyboardAutocapitalizationMode = switch type {
        case .none: .none
        case .words: .words
        case .sentences: .sentences
        case .allCharacters: .allCharacters
        @unknown default: .sentences
        }
        guard !enabled else { return mode }
        return mode == .allCharacters ? .allCharacters : .none
    }
}
#endif
