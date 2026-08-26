import SwiftUI

struct AppShellView: View {
    @State private var selectedTab = AppTab.defaultTab

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab(
                AppTab.settings.title,
                systemImage: AppTab.settings.systemImage,
                value: AppTab.settings
            ) {
                NavigationStack { SettingsScreen() }
            }

            Tab(
                AppTab.appearance.title,
                systemImage: AppTab.appearance.systemImage,
                value: AppTab.appearance
            ) {
                NavigationStack { AppearanceScreen() }
            }

            Tab(
                AppTab.about.title,
                systemImage: AppTab.about.systemImage,
                value: AppTab.about
            ) {
                NavigationStack { AboutScreen() }
            }
        }
        .tint(.accentColor)
    }
}

#Preview("App Shell · Light") {
    AppShellView()
        .preferredColorScheme(.light)
}

#Preview("App Shell · Dark") {
    AppShellView()
        .preferredColorScheme(.dark)
}
