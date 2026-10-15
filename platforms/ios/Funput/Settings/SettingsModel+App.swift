import FunputShared
import ThemeRuntime

extension SettingsModel {
    /// The model the app shell owns.
    ///
    /// UI tests run against throwaway stores, so a test run never rewrites the
    /// configuration the installed keyboard reads.
    static func makeForApp() -> SettingsModel {
#if DEBUG
        if ShortcutsStoreFactory.isUITesting {
            return SettingsModel(
                store: PreviewConfigurationStore(),
                customStore: PreviewCustomThemeStore(),
                bootstrap: NoopKeyboardBootstrapSynchronizer()
            )
        }
#endif
        return SettingsModel(
            store: FunputConfigurationStore(),
            customStore: CustomThemeStore(),
            bootstrap: KeyboardBootstrapSynchronizer()
        )
    }
}
