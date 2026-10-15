/// The app's tabs, in tab-bar order.
///
/// iOS folds a sixth tab into "More", so five is the ceiling: features that come later
/// (an account, sync) belong inside ``me`` rather than beside it.
enum AppTab: String, CaseIterable, Hashable, Identifiable {
    case settings
    case shortcuts
    case appearance
    case news
    case me

    static let defaultTab = AppTab.settings

    var id: Self { self }

    var title: String {
        switch self {
        case .settings: "Bàn phím"
        case .shortcuts: "Gõ tắt"
        case .appearance: "Giao diện"
        case .news: "Tin tức"
        case .me: "Tôi"
        }
    }

    var systemImage: String {
        switch self {
        case .settings: "keyboard"
        case .shortcuts: "text.append"
        case .appearance: "paintpalette"
        case .news: "newspaper"
        case .me: "person.crop.circle"
        }
    }
}
