import SwiftUI

struct ThemeGalleryEmptyCard: View {
    var body: some View {
        AdaptiveGlassCard {
            ContentUnavailableView(
                "Chưa có theme tùy chỉnh",
                systemImage: "paintbrush",
                description: Text("Tạo bản tùy chỉnh từ một theme hệ thống.")
            )
            .frame(maxWidth: .infinity)
        }
        .frame(width: 340, height: 150)
        .accessibilityIdentifier("appearance.custom.empty")
    }
}
