import SwiftUI

/// The keyboard tab: setup status, then one link per settings page — or, while the user
/// searches, the settings that match.
struct SettingsScreen: View {
    @Environment(\.openURL) private var openURL
    @ObservedObject var model: SettingsModel
    @State private var query = ""

    var body: some View {
        AppScreen {
            if query.trimmingCharacters(in: .whitespaces).isEmpty {
                KeyboardSetupCard(hasFullAccess: model.hasFullAccess) {
                    openURL(.funputSystemSettings)
                }
                SettingsNavigationCard(destinations: SettingsDestination.hubRows, model: model)
            } else {
                SettingsSearchResults(query: query)
            }
        }
        .navigationTitle(AppTab.settings.title)
        .searchable(text: $query, prompt: "Tìm cài đặt")
        .settingsDestinations(model: model)
    }
}
