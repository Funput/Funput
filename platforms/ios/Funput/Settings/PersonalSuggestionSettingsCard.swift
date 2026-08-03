import SwiftUI

struct PersonalSuggestionSettingsCard: View {
    @Binding var isEnabled: Bool
    let reset: () -> Void

    @State private var confirms = false

    var body: some View {
        SettingsSectionCard(title: "Gợi ý từ cá nhân", systemImage: "text.badge.star") {
            SettingsToggleRow(
                title: "Hiện gợi ý từ",
                summary: "Chỉ học chữ Funput gõ và lưu trên thiết bị.",
                isOn: $isEnabled
            )
            Button("Xóa từ đã học", role: .destructive) { confirms = true }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 10)
                .accessibilityHint("Xóa lexicon cá nhân ở lần mở bàn phím tiếp theo")
                // Anchored to the button rather than to the screen: iOS 26 points a
                // confirmation dialog at whatever presents it, so one hung off the
                // root view surfaces at the top instead of beside its control.
                .confirmationDialog(
                    "Xóa toàn bộ từ đã học?",
                    isPresented: $confirms,
                    titleVisibility: .visible
                ) {
                    Button("Xóa từ đã học", role: .destructive, action: reset)
                } message: {
                    Text("Lệnh xóa được thực hiện cục bộ khi Funput mở bàn phím lần tiếp theo.")
                }
        }
    }
}
