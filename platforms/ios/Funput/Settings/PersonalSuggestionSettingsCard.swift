import SwiftUI

struct PersonalSuggestionSettingsCard: View {
    @Binding var isEnabled: Bool
    let reset: () -> Void

    var body: some View {
        SettingsSectionCard(title: "Gợi ý từ cá nhân", systemImage: "text.badge.star") {
            SettingsToggleRow(
                title: "Hiện gợi ý từ",
                summary: "Chỉ học chữ Funput gõ và lưu trên thiết bị.",
                isOn: $isEnabled
            )
            Button("Xóa từ đã học", role: .destructive, action: reset)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 10)
                .accessibilityHint("Xóa lexicon cá nhân ở lần mở bàn phím tiếp theo")
        }
    }
}
