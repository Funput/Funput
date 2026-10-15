import FunputShared
import SwiftUI

/// The page behind each ``SettingsDestination``, built from the existing section cards.
struct SettingsDestinationView: View {
    @Environment(\.openURL) private var openURL
    let destination: SettingsDestination
    @ObservedObject var model: SettingsModel

    var body: some View {
        SettingsPage(destination, model: model) { select in
            content(select)
        }
    }

    @ViewBuilder private func content(_ select: @escaping (SettingsPicker) -> Void) -> some View {
        switch destination {
        case .typing:
            TypingSettingsSection(model: model, select: select)
            ExtraOnsetsSettingsCard(model: model)
        case .layout:
            LayoutSettingsSection(model: model, select: select)
        case .smartInput:
            SmartInputSettingsSection(model: model)
        case .feedback:
            FeedbackSettingsSection(model: model)
        case .clipboard:
            ClipboardSettingsCard(
                isEnabled: model.boolBinding(\.clipboardEnabled),
                expiryLabel: model.clipboardExpiryLabel,
                selectExpiry: { select(.clipboardExpiry) },
                openSettings: { openURL(.funputSystemSettings) }
            )
        case .data:
            SettingsDataCard(
                resetPersonalSuggestions: { model.requestPersonalSuggestionReset() },
                clearClipboard: { model.clearClipboardHistory() },
                resetSettings: { model.reset() }
            )
        }
    }
}

extension View {
    /// Lets any navigation stack open settings pages, so a page can be linked from more
    /// than one tab without each tab knowing how it is built.
    func settingsDestinations(model: SettingsModel) -> some View {
        navigationDestination(for: SettingsDestination.self) { destination in
            SettingsDestinationView(destination: destination, model: model)
        }
    }
}
