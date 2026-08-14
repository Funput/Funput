import KeyboardInput
import UIKit

extension KeyboardDocumentWriter {
    func inputContext(autoCapitalizeEnabled: Bool) -> KeyboardInputContext {
        KeyboardInputContextResolver.resolve(
            keyboardType: proxy.keyboardType ?? .default,
            returnKeyType: proxy.returnKeyType ?? .default,
            isSecureTextEntry: proxy.isSecureTextEntry ?? false,
            autocapitalizationType: proxy.autocapitalizationType ?? .sentences,
            autoCapitalizeEnabled: autoCapitalizeEnabled
        )
    }
}
