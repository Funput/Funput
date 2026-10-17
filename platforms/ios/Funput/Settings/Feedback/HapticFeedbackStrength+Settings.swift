import FunputShared

extension HapticFeedbackStrength {
    var settingsTitle: String {
        switch self {
        case .light: "Nhẹ"
        case .medium: "Trung bình"
        case .strong: "Mạnh"
        }
    }

    var settingsSummary: String {
        switch self {
        case .light: "Nhẹ nhàng, ít gây chú ý"
        case .medium: "Cân bằng khi gõ hằng ngày"
        case .strong: "Rõ ràng ở mỗi lần chạm"
        }
    }
}
