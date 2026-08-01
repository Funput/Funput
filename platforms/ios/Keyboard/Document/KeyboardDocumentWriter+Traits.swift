import KeyboardInput
import UIKit

extension KeyboardDocumentWriter {
    var inputContext: KeyboardInputContext {
        KeyboardInputContextResolver.resolve(
            keyboardType: proxy.keyboardType ?? .default,
            returnKeyType: proxy.returnKeyType ?? .default,
            isSecureTextEntry: proxy.isSecureTextEntry ?? false,
            autocapitalizationType: proxy.autocapitalizationType ?? .sentences
        )
    }
}
