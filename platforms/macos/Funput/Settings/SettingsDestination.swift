import SwiftUI

enum SettingsDestination: String, CaseIterable, Identifiable, Hashable {
    case overview
    case typing
    case keyboardShortcuts
    case textShortcuts
    case applications

    var id: String { rawValue }

    static let general: [Self] = [.overview]
    static let vietnameseTyping: [Self] = [.typing, .keyboardShortcuts]
    static let automation: [Self] = [.textShortcuts, .applications]

    var title: String {
        switch self {
        case .overview: "Tổng quan"
        case .typing: "Cách gõ"
        case .keyboardShortcuts: "Phím tắt"
        case .textShortcuts: "Gõ tắt"
        case .applications: "Ứng dụng"
        }
    }

    var subtitle: String {
        switch self {
        case .overview: "Trạng thái và thiết lập chung của Funput."
        case .typing: "Phương thức gõ, cách đặt dấu và xử lý thông minh."
        case .keyboardShortcuts: "Điều khiển Funput mà không rời bàn phím."
        case .textShortcuts: "Mở rộng chuỗi gõ thành nội dung thường dùng."
        case .applications: "Chọn ứng dụng luôn sử dụng tiếng Anh."
        }
    }

    var systemImage: String {
        switch self {
        case .overview: "square.grid.2x2"
        case .typing: "character.cursor.ibeam"
        case .keyboardShortcuts: "command"
        case .textShortcuts: "text.append"
        case .applications: "app.badge"
        }
    }
}

enum SettingsPresentation: String, Identifiable {
    case about

    var id: String { rawValue }
}
