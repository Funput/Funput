import SwiftUI

/// The "Gõ không gạch chân" switch, shared by the Typing pane and the quick controls on the
/// Overview pane so both always read the same.
struct DirectCompositionRow: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings

        SettingsRow(
            title: "Gõ không gạch chân",
            subtitle: "Chữ đang gõ hiện như chữ thường, Google Docs và nơi không hỗ trợ vẫn có gạch chân",
            systemImage: "underline"
        ) {
            Toggle("Gõ không gạch chân", isOn: $settings.directComposition)
                .labelsHidden()
                .toggleStyle(.switch)
                .tint(Theme.accent)
        }
    }
}
