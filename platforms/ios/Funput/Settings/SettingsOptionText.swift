import FunputShared
import KeyboardLayout

extension KeyboardInputMethod {
    var settingsTitle: String {
        switch self {
        case .telex: "Telex"
        case .telexAdvanced: "Telex nâng cao"
        case .vni: "VNI"
        }
    }

    var settingsSummary: String {
        switch self {
        case .telex: "Dùng tổ hợp chữ để nhập dấu."
        case .telexAdvanced: "Telex nâng cao — [→ư, ]→ơ, w đầu từ→ư."
        case .vni: "Dùng các phím số để nhập dấu."
        }
    }
}

extension ToneStyleOption {
    var settingsTitle: String { self == .traditional ? "Truyền thống" : "Hiện đại" }
}
