import SwiftUI

struct SettingsScreen: View {
    var body: some View {
        AppScreen {
            AdaptiveGlassCard {
                VStack(alignment: .leading, spacing: 12) {
                    FunputIdentityView(compact: true)
                    Text("Tùy chỉnh cách Funput nhập liệu và phản hồi khi bạn gõ.")
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            FeatureGroupCard(
                title: "Bàn phím",
                systemImage: "keyboard",
                summary: "Các thiết lập nền tảng cho trải nghiệm gõ tiếng Việt.",
                features: ["Kiểu gõ và đặt dấu", "Kích thước bàn phím"]
            )
            FeatureGroupCard(
                title: "Nhập liệu thông minh",
                systemImage: "wand.and.stars",
                summary: "Kiểm soát cách Funput hỗ trợ và khôi phục nội dung.",
                features: ["Kiểm tra chính tả", "Smart Restore"]
            )
            FeatureGroupCard(
                title: "Phản hồi",
                systemImage: "hand.tap",
                summary: "Tinh chỉnh phản hồi trực quan và xúc giác.",
                features: ["Rung khi gõ", "Xem trước phím"]
            )
        }
        .navigationTitle("Cài đặt")
    }
}

#Preview("Cài đặt · Light") {
    NavigationStack { SettingsScreen() }
        .preferredColorScheme(.light)
}

#Preview("Cài đặt · Dark") {
    NavigationStack { SettingsScreen() }
        .preferredColorScheme(.dark)
}
