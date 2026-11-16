import SwiftUI

struct AppShellView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab = AppTab.defaultTab
    // Owned here rather than by a tab: "Bàn phím" and "Tôi" both edit the configuration,
    // and two copies would let one tab save over what the other just changed.
    @StateObject private var settings = SettingsModel.makeForApp()
    @StateObject private var shortcuts = ShortcutsModel(store: ShortcutsStoreFactory.make())
    @StateObject private var news = NewsModel.makeForApp()
    @State private var fullAccessRefresh: FullAccessRefresh?

    var body: some View {
        tabs
            .tint(.accentColor)
            .onAppear { syncWithKeyboard(for: scenePhase) }
            .background {
                if #available(iOS 17, *) {
                    Color.clear
                        .onChange(of: scenePhase) { _, phase in syncWithKeyboard(for: phase) }
                        .onChange(of: selectedTab) { settings.reload() }
                } else {
                    Color.clear
                        .onChange(of: scenePhase) { phase in syncWithKeyboard(for: phase) }
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
        AppTabRoot(tab: tab, settings: settings, shortcuts: shortcuts, news: news)
    }

    /// The keyboard and the Appearance tab write the same configuration, and only the
    /// keyboard can confirm Full Access, so the copy held here follows both while active.
    private func syncWithKeyboard(for phase: ScenePhase) {
        if fullAccessRefresh == nil {
            fullAccessRefresh = FullAccessRefresh(
                reloadSettings: { settings.reload() },
                refreshAccess: { settings.refreshFullAccess() }
            )
        }
        if phase == .active { fullAccessRefresh?.start() } else { fullAccessRefresh?.stop() }
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
