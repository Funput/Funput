import SwiftUI

struct AppShellView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab = AppTab.defaultTab
    // Owned here rather than by a tab: "Bàn phím" and "Tôi" both edit the configuration,
    // and two copies would let one tab save over what the other just changed.
    @StateObject private var settings = SettingsModel.makeForApp()
    @StateObject private var shortcuts = ShortcutsModel(store: ShortcutsStoreFactory.make())

    var body: some View {
        tabs
            .tint(.accentColor)
            .background {
                if #available(iOS 17, *) {
                    Color.clear
                        .onChange(of: scenePhase) { _, phase in reloadIfActive(phase) }
                        .onChange(of: selectedTab) { settings.reload() }
                } else {
                    Color.clear
                        .onChange(of: scenePhase) { phase in reloadIfActive(phase) }
                        .onChange(of: selectedTab) { _ in settings.reload() }
                }
            }
    }

    @ViewBuilder private var tabs: some View {
        if #available(iOS 18, *) {
            TabView(selection: $selectedTab) {
                ForEach(AppTab.allCases) { tab in
                    Tab(tab.title, systemImage: tab.systemImage, value: tab) { root(tab) }
                }
            }
        } else {
            TabView(selection: $selectedTab) {
                ForEach(AppTab.allCases) { tab in
                    root(tab)
                        .tabItem { Label(tab.title, systemImage: tab.systemImage) }
                        .tag(tab)
                }
            }
        }
    }

    private func root(_ tab: AppTab) -> some View {
        AppTabRoot(tab: tab, settings: settings, shortcuts: shortcuts)
    }

    /// The keyboard and the Appearance tab write the same configuration, so the copy
    /// held here is refreshed whenever it may have changed underneath it.
    private func reloadIfActive(_ phase: ScenePhase) {
        if phase == .active { settings.reload() }
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
