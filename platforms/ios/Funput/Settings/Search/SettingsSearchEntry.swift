/// One setting the keyboard tab's search can find, and the page that holds it.
///
/// Pickers and the smart-input switches are already declared as data, so their entries
/// are derived; the remaining rows are listed here with the words people search by,
/// accent-free and English included ("rung", "haptic").
struct SettingsSearchEntry: Identifiable {
    let title: String
    let systemImage: String
    let keywords: [String]
    let destination: SettingsDestination

    var id: String { "\(destination.rawValue).\(title)" }

    static let all: [SettingsSearchEntry] = pickers + smartInput + rows

    private static let pickerKeywords: [(SettingsPicker, [String], SettingsDestination)] = [
        (.inputMethod, ["telex", "vni", "input method"], .typing),
        (.toneStyle, ["dau", "tone", "hoa", "oa"], .typing),
        (.language, ["tieng anh", "english", "language"], .typing),
        (.layoutPreset, ["layout", "preset", "qwerty"], .layout),
        (.keySizing, ["size", "co phim"], .layout),
        (.clipboardExpiry, ["het han", "expiry", "clipboard"], .clipboard),
    ]

    private static let pickers = pickerKeywords.map { picker, keywords, destination in
        SettingsSearchEntry(
            title: picker.title,
            systemImage: picker.systemImage,
            keywords: [picker.summary] + keywords,
            destination: destination
        )
    }

    private static let smartInput = SmartInputToggle.all.map { toggle in
        SettingsSearchEntry(
            title: toggle.title,
            systemImage: toggle.systemImage,
            keywords: [toggle.summary],
            destination: .smartInput
        )
    }

    private static let rows: [SettingsSearchEntry] = [
        .init(title: "Đổi ngôn ngữ ở phím cách", systemImage: "arrow.left.arrow.right",
              keywords: ["phim cach", "space", "vuot", "english"], destination: .typing),
        .init(title: "Phụ âm đầu mở rộng", systemImage: "character.cursor.ibeam",
              keywords: ["z f w j", "teencode", "zo", "fai"], destination: .typing),
        .init(title: "Thanh công cụ", systemImage: "rectangle.topthird.inset.filled",
              keywords: ["toolbar", "emoji", "goi y"], destination: .layout),
        .init(title: "Hàng phím số", systemImage: "textformat.123",
              keywords: ["number row", "123", "so"], destination: .layout),
        .init(title: "Chiều cao bàn phím", systemImage: "arrow.up.and.down",
              keywords: ["height", "cao", "thap"], destination: .layout),
        .init(title: "Rung khi gõ", systemImage: "iphone.radiowaves.left.and.right",
              keywords: ["haptic", "vibration"], destination: .feedback),
        .init(title: "Âm thanh khi gõ", systemImage: "speaker.wave.2",
              keywords: ["sound", "click", "tieng"], destination: .feedback),
        .init(title: "Xem trước phím", systemImage: "character.magnify",
              keywords: ["preview", "phong to", "popup"], destination: .feedback),
        .init(title: "Lưu lịch sử clipboard", systemImage: "doc.on.clipboard",
              keywords: ["copy", "sao chep", "dan", "paste"], destination: .clipboard),
        .init(title: "Xoá từ đã học", systemImage: "text.badge.xmark",
              keywords: ["xoa", "goi y", "learned"], destination: .data),
        .init(title: "Xoá lịch sử clipboard", systemImage: "clipboard",
              keywords: ["xoa", "clipboard", "ghim"], destination: .data),
        .init(title: "Khôi phục cài đặt mặc định", systemImage: "arrow.counterclockwise",
              keywords: ["reset", "mac dinh"], destination: .data),
    ]
}
