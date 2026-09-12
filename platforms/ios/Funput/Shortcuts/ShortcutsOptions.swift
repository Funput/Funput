#if DEBUG
import SwiftUI

struct ShortcutsOptions: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Bindable var model: ShortcutsModel

    var body: some View {
        NavigationStack {
            AppScreen {
                ContentCard {
                    SettingsToggleRow(
                        title: "Tự nhận diện hoa/thường",
                        summary: "Gõ vn → việt nam, Vn → Việt Nam, VN → VIỆT NAM. Tắt để chỉ khớp đúng chữ tắt đã lưu.",
                        isOn: $model.smartCase
                    )
                    .accessibilityIdentifier("shortcuts.smartCase")
                    Divider()
                    SettingsToggleRow(
                        title: "Gõ tắt khi dùng tiếng Anh",
                        summary: "Vẫn thay chữ tắt khi bàn phím ở chế độ tiếng Anh.",
                        isOn: $model.inEnglish
                    )
                    .accessibilityIdentifier("shortcuts.inEnglish")
                }
                Text("Các tuỳ chọn chỉ thay đổi trong bản xem trước.")
                    .font(.caption).foregroundStyle(.secondary)
            }
            .navigationTitle("Tuỳ chọn gõ tắt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Xong") { dismiss() }
                }
            }
        }
        .presentationDetents(dynamicTypeSize.isAccessibilitySize ? [.large] : [.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

#Preview("Tuỳ chọn") {
    ShortcutsOptions(model: ShortcutsModel())
}
#endif
