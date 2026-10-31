import Foundation
import PersonalDictionary

/// The words the page uses for the store's own terms, in one place so the header,
/// the menus and VoiceOver never say the same thing two ways.
extension DictionaryFilter {
    var title: String {
        switch self {
        case .learned: "Đã học"
        case .added: "Tự thêm"
        case .blocked: "Đã chặn"
        }
    }

    var accessibilityID: String {
        switch self {
        case .learned: "learned"
        case .added: "added"
        case .blocked: "blocked"
        }
    }
}

extension DictionarySort {
    var title: String {
        switch self {
        case .mostUsed: "Dùng nhiều nhất"
        case .recent: "Mới dùng"
        case .alphabetical: "Theo bảng chữ cái"
        }
    }

    var systemImage: String {
        switch self {
        case .mostUsed: "chart.bar"
        case .recent: "clock"
        case .alphabetical: "textformat.abc"
        }
    }
}

extension DictionaryWord {
    /// The line under the word.
    var detail: String {
        switch origin {
        case .learned: "Dùng \(uses.formatted()) lần"
        case .added: "Bạn tự thêm"
        case .blocked: "Không bao giờ được gợi ý"
        }
    }

    var accessibilityLabel: String {
        "\(text), \(detail.lowercased())"
    }
}
