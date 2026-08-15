import FunputShared
import ThemeSchema
@testable import Funput

final class KeyboardBootstrapTestStore: KeyboardBootstrapSynchronizing {
    var acceptsSaves: Bool
    private(set) var saves: [(FunputConfiguration, [CustomKeyboardTheme])] = []

    init(acceptsSaves: Bool = true) {
        self.acceptsSaves = acceptsSaves
    }

    func save(
        configuration: FunputConfiguration,
        customThemes: [CustomKeyboardTheme]
    ) -> Bool {
        guard acceptsSaves else { return false }
        saves.append((configuration, customThemes))
        return true
    }
}
