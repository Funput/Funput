import FunputShared

/// One switch of the "Thông minh" page.
///
/// Declared as data so the page, its summary on the keyboard tab and the settings search
/// all read the same list instead of three copies drifting apart.
struct SmartInputToggle: Identifiable {
    let title: String
    let summary: String
    let systemImage: String
    let keyPath: WritableKeyPath<FunputConfiguration, Bool>

    var id: String { title }

    static let all: [SmartInputToggle] = [
        .init(
            title: "Khôi phục từ thông minh", summary: "Giữ từ gốc khi sửa hoặc xóa dấu.",
            systemImage: "arrow.uturn.backward", keyPath: \.smartRestore
        ),
        .init(
            title: "Khôi phục sớm", summary: "Khôi phục từ ngay trong lúc gõ.",
            systemImage: "bolt", keyPath: \.eagerRestore
        ),
        .init(
            title: "Kiểm tra chính tả", summary: "Nhận diện từ tiếng Việt chưa hợp lệ.",
            systemImage: "checkmark.circle", keyPath: \.spellCheck
        ),
        .init(
            title: "Tự viết hoa", summary: "Viết hoa chữ đầu câu.",
            systemImage: "textformat.size.larger", keyPath: \.autoCapitalize
        ),
        .init(
            title: "Hiện gợi ý từ", summary: "Từ đã học và từ điển tiếng Anh.",
            systemImage: "text.badge.star", keyPath: \.personalSuggestionsEnabled
        ),
        // One switch for every movement-based gesture: someone who dislikes one of
        // them almost always wants all three gone.
        .init(
            title: "Cử chỉ thông minh",
            summary: "Chạm đúp phím cách để chấm câu, giữ phím cách để di con trỏ, vuốt trái phím xóa để xóa từ.",
            systemImage: "hand.draw", keyPath: \.smartGesturesEnabled
        ),
        .init(
            title: "Tự về bảng chữ",
            summary: "Gõ dấu câu ở bảng ký hiệu rồi bấm cách để quay lại bảng chữ cái.",
            systemImage: "abc", keyPath: \.returnsToLettersAfterPunctuation
        ),
    ]
}
