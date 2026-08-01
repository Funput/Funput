enum AppTab: String, CaseIterable, Hashable, Identifiable {
    case settings
    case appearance
    case about

    static let defaultTab = AppTab.settings

    var id: Self { self }

    var title: String {
        switch self {
        case .settings: "Cài đặt"
        case .appearance: "Giao diện"
        case .about: "Giới thiệu"
        }
    }

    var systemImage: String {
        switch self {
        case .settings: "gearshape"
        case .appearance: "paintpalette"
        case .about: "info.circle"
        }
    }
}
