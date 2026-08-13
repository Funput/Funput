import FunputShared
import ThemeRuntime
import ThemeSchema

/// Guards asynchronous keyboard work against extension activation changes.
@MainActor
public final class KeyboardActivationState {
    public private(set) var generation: UInt64 = 0
    public private(set) var isActive = false

    public init() {}

    @discardableResult
    public func begin() -> UInt64 {
        generation &+= 1
        isActive = true
        return generation
    }

    public func end() {
        isActive = false
    }

    public func accepts(_ generation: UInt64) -> Bool {
        isActive && self.generation == generation
    }
}

/// Resolves one immutable theme snapshot for a keyboard activation.
public enum KeyboardActivationThemeResolver {
    public static func resolve(
        configuration: FunputConfiguration,
        customThemes: [CustomKeyboardTheme]
    ) -> (catalog: ThemeCatalog, selectedTheme: KeyboardTheme) {
        let catalog = ThemeCatalog(customThemes: customThemes)
        let selected = catalog.theme(id: configuration.selectedThemeID)
            ?? BundledThemes.default
        return (catalog, selected)
    }
}
