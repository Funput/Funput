import KeyboardInput
import KeyboardLayout
import UIKit

typealias ResolvedTextInputTraits = KeyboardInputContext

@MainActor
enum TextInputTraitsResolver {
    static func resolve(_ proxy: any UITextDocumentProxy) -> ResolvedTextInputTraits {
        KeyboardInputContextResolver.resolve(
            keyboardType: proxy.keyboardType ?? .default,
            returnKeyType: proxy.returnKeyType ?? .default,
            isSecureTextEntry: proxy.isSecureTextEntry ?? false,
            autocapitalizationType: proxy.autocapitalizationType ?? .sentences
        )
    }
}
