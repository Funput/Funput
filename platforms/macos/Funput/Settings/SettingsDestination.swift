import SwiftUI

enum SettingsDestination: String, CaseIterable, Identifiable, Hashable {
    case overview
    case typing
    case automation
    case shortcuts

    var id: String { rawValue }

    var title: String {
        switch self {
        case .overview: "Tổng quan"
        case .typing: "Nhập liệu"
        case .automation: "Tự động hóa"
        case .shortcuts: "Phím tắt"
        }
    }

    var subtitle: String {
        switch self {
        case .overview: "Trạng thái và thiết lập chung của Funput."
        case .typing: "Phương thức gõ, cách đặt dấu và xử lý thông minh."
        case .automation: "Gõ tắt và quy tắc riêng cho từng ứng dụng."
        case .shortcuts: "Điều khiển Funput mà không rời bàn phím."
        }
    }

    var systemImage: String {
        switch self {
        case .overview: "square.grid.2x2"
        case .typing: "character.cursor.ibeam"
        case .automation: "wand.and.sparkles"
        case .shortcuts: "command"
        }
    }
}

enum SettingsPresentation: String, Identifiable {
    case about

    var id: String { rawValue }
}
