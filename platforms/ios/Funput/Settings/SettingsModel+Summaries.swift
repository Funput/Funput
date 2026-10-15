import FunputShared
import KeyboardLayout

extension SettingsModel {
    /// The sentence under a page link: where that page stands, in plain Vietnamese, so
    /// the keyboard tab still answers "what is set?" without opening anything.
    func summary(for destination: SettingsDestination) -> String? {
        switch destination {
        case .typing: typingSummary
        case .layout: layoutSummary
        case .smartInput: smartInputSummary
        case .feedback: feedbackSummary
        case .clipboard: clipboardSummary
        case .data: nil
        }
    }

    // The input method sits on the keyboard tab itself, so this line covers the rest.
    private var typingSummary: String {
        let tones = configuration.toneStyle == .modern ? "Đặt dấu kiểu mới" : "Đặt dấu kiểu cũ"
        let language = configuration.languageToggleEnabled
            ? "đổi ngôn ngữ ở phím cách" : "luôn gõ tiếng Việt"
        return "\(tones), \(language)"
    }

    private var layoutSummary: String {
        let layout = configuration.layoutPreset == .funput ? "Bố cục Funput" : "Bố cục giống iOS"
        guard configuration.keySizing == .funput else { return "\(layout), kích thước như iOS" }
        return "\(layout), cao \(Int((configuration.heightScale * 100).rounded()))%"
    }

    private var smartInputSummary: String {
        let total = SmartInputToggle.all.count
        let enabled = SmartInputToggle.all.filter { configuration[keyPath: $0.keyPath] }.count
        switch enabled {
        case 0: return "Đang tắt tất cả"
        case total: return "Đang bật tất cả"
        default: return "Đang bật \(enabled) trên \(total) tính năng"
        }
    }

    private var feedbackSummary: String {
        let enabled = [
            (configuration.isHapticFeedbackEnabled, "rung"),
            (configuration.isKeySoundEnabled, "âm thanh"),
            (configuration.showsKeyPreviews, "xem trước phím"),
        ].filter { $0.0 }.map { $0.1 }
        guard let last = enabled.last else { return "Đang tắt tất cả" }
        let list = enabled.count == 1 ? last : "\(enabled.dropLast().joined(separator: ", ")) và \(last)"
        return "Đang bật \(list)"
    }

    private var clipboardSummary: String {
        configuration.clipboardEnabled
            ? "Lưu lịch sử trong \(clipboardExpiryLabel)" : "Không lưu lịch sử"
    }
}
