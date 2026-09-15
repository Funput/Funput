import KeyboardLayout

// Kept out of `SettingsModel` so that file stays under the LOC gate; it also puts the
// label next to the other settings copy rather than in the model.
extension KeyboardLayoutPreset {
    var settingsTitle: String {
        switch self {
        case .funput: "Funput"
        case .system: "Giống hệ thống"
        }
    }

    var settingsSummary: String {
        switch self {
        case .funput: "Bố cục mặc định, có phím phẩy và chấm."
        case .system: "Sắp phím theo bàn phím tiếng Việt của iOS."
        }
    }
}

extension KeyboardKeySizing {
    var settingsTitle: String {
        switch self {
        case .funput: "Funput"
        case .system: "Giống hệ thống"
        }
    }

    var settingsSummary: String {
        switch self {
        case .funput: "Phím cao, khoảng cách hẹp, chỉnh được chiều cao."
        case .system: "Chiều cao hàng và khoảng cách như bàn phím iOS."
        }
    }
}
