import FunputShared
import SwiftUI
import ThemeRuntime

#Preview("Cài đặt · Light") {
    NavigationStack {
        SettingsScreen(
            store: PreviewConfigurationStore(),
            customStore: PreviewCustomThemeStore(),
            bootstrap: NoopKeyboardBootstrapSynchronizer()
        )
    }
        .preferredColorScheme(.light)
}

#Preview("Cài đặt · Dark") {
    NavigationStack {
        SettingsScreen(
            store: PreviewConfigurationStore(),
            customStore: PreviewCustomThemeStore(),
            bootstrap: NoopKeyboardBootstrapSynchronizer()
        )
    }
        .preferredColorScheme(.dark)
}
