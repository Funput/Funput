import Foundation

/// A docs page pinned above the blog posts: the guides a new user needs most, which the
/// feed's newest-first order would otherwise push out of sight.
struct NewsGuide: Identifiable {
    let id: String
    let title: String
    let summary: String
    let systemImage: String
    let url: URL

    static let pinned = [
        NewsGuide(
            id: "setup",
            title: "Hướng dẫn cài đặt",
            summary: "Thêm bàn phím, bật truy cập đầy đủ và gõ thử.",
            systemImage: "book",
            url: URL(string: "https://docs.funput.app/docs/install/ios/")!
        ),
        NewsGuide(
            id: "typing",
            title: "Hướng dẫn gõ tiếng Việt",
            summary: "Bảng phím và ví dụ gõ bằng Telex và VNI.",
            systemImage: "character.book.closed",
            url: URL(string: "https://docs.funput.app/docs/input-methods/vietnamese/")!
        ),
    ]
}
