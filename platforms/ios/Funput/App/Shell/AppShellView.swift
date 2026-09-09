import SwiftUI

struct AppShellView: View {
    @State private var selectedTab = AppTab.defaultTab

    var body: some View {
        if #available(iOS 18, *) {
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
        } else {
            TabView(selection: $selectedTab) {
                NavigationStack { SettingsScreen() }
                    .tabItem {
                        Label(AppTab.settings.title, systemImage: AppTab.settings.systemImage)
                    }
                    .tag(AppTab.settings)
                NavigationStack { AppearanceScreen() }
                    .tabItem {
                        Label(AppTab.appearance.title, systemImage: AppTab.appearance.systemImage)
                    }
                    .tag(AppTab.appearance)
                NavigationStack { AboutScreen() }
                    .tabItem {
                        Label(AppTab.about.title, systemImage: AppTab.about.systemImage)
                    }
                    .tag(AppTab.about)
            }
            .tint(.accentColor)
        }
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
