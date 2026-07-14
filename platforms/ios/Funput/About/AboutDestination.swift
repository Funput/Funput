import SwiftUI

struct AboutDestination: Identifiable {
    let id: String
    let title: String
    let summary: String
    let systemImage: String
    let url: URL
    let tint: Color
}

extension AboutDestination {
    static let discovery = [
        item("website", "Website", "Tin tức và thông tin về Funput", "globe", "https://funput.app/", .blue),
        item("github", "GitHub", "Xem mã nguồn Funput/Funput", "chevron.left.forwardslash.chevron.right", "https://github.com/Funput/Funput", .primary),
    ]

    static let support = [
        item("issues", "Báo lỗi", "Gửi phản hồi trên GitHub Issues", "exclamationmark.bubble.fill", "https://github.com/Funput/Funput/issues", .orange),
        item("email", "Liên hệ", "hello@funput.app", "envelope.fill", "mailto:hello@funput.app", .green),
    ]

    static let legal = [
        item("privacy", "Chính sách quyền riêng tư", "Cách Funput bảo vệ dữ liệu của bạn", "lock.shield.fill", "https://funput.app/privacy", .purple),
    ]

    private static func item(
        _ id: String,
        _ title: String,
        _ summary: String,
        _ systemImage: String,
        _ url: String,
        _ tint: Color
    ) -> AboutDestination {
        AboutDestination(
            id: id,
            title: title,
            summary: summary,
            systemImage: systemImage,
            url: URL(string: url)!,
            tint: tint
        )
    }
}
