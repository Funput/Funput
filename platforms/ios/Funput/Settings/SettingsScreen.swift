import SwiftUI

/// The keyboard tab: setup status, then one link per settings page.
struct SettingsScreen: View {
    @Environment(\.openURL) private var openURL
    @ObservedObject var model: SettingsModel

    var body: some View {
        AppScreen {
            KeyboardSetupCard(hasFullAccess: model.hasFullAccess) {
                openURL(.funputSystemSettings)
            }
            SettingsNavigationCard(destinations: SettingsDestination.hubRows, model: model)
        }
        .navigationTitle(AppTab.settings.title)
        .settingsDestinations(model: model)
    }
}
