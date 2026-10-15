import SwiftUI

/// The root of one tab: its own navigation stack, so each tab keeps its place when the
/// user switches away and back.
struct AppTabRoot: View {
    let tab: AppTab
    let settings: SettingsModel
    let shortcuts: ShortcutsModel
    let news: NewsModel

    var body: some View {
        NavigationStack {
            switch tab {
            case .settings: SettingsScreen(model: settings)
            case .shortcuts: ShortcutsScreen(model: shortcuts)
            case .appearance: AppearanceScreen()
            case .news: NewsScreen(model: news)
            case .me: MeScreen(settings: settings)
            }
        }
    }
}
