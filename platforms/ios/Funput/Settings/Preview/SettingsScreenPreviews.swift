import FunputShared
import SwiftUI
import ThemeRuntime

private func previewModel() -> SettingsModel {
    SettingsModel(
        store: PreviewConfigurationStore(),
        customStore: PreviewCustomThemeStore(),
        bootstrap: NoopKeyboardBootstrapSynchronizer()
    )
}

#Preview("Bàn phím · Light") {
    NavigationStack { SettingsScreen(model: previewModel()) }
        .preferredColorScheme(.light)
}

#Preview("Bàn phím · Dark") {
    NavigationStack { SettingsScreen(model: previewModel()) }
        .preferredColorScheme(.dark)
}

#Preview("Gõ tiếng Việt · Light") {
    NavigationStack { SettingsDestinationView(destination: .typing, model: previewModel()) }
        .preferredColorScheme(.light)
}

#Preview("Phản hồi khi chạm · Dark") {
    NavigationStack { SettingsDestinationView(destination: .feedback, model: previewModel()) }
        .preferredColorScheme(.dark)
}
