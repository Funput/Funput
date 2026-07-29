import SwiftUI

struct AppShellView: View {
    @State private var selectedTab = AppTab.defaultTab

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                SettingsScreen()
            }
            .tabItem { tabLabel(.settings) }
            .tag(AppTab.settings)

            NavigationStack {
                AppearanceScreen()
            }
            .tabItem { tabLabel(.appearance) }
            .tag(AppTab.appearance)

            NavigationStack {
                AboutScreen()
            }
            .tabItem { tabLabel(.about) }
            .tag(AppTab.about)
        }
        .tint(.accentColor)
    }

    private func tabLabel(_ tab: AppTab) -> some View {
        Label(tab.title, systemImage: tab.systemImage)
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
