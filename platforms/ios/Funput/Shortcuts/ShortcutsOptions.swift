import FunputShared
import SwiftUI

struct ShortcutsOptions: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Bindable var model: ShortcutsModel

    var body: some View {
        NavigationStack {
            AppScreen {
                if model.loadError != nil { ShortcutsLoadStatus(model: model) }
                ContentCard {
                    SettingsToggleRow(
                        title: "Tự nhận diện hoa/thường",
                        summary: "Gõ vn → việt nam, Vn → Việt Nam, VN → VIỆT NAM. Tắt để chỉ khớp đúng chữ tắt đã lưu.",
                        isOn: model.binding(\.smartCase)
                    )
                    .accessibilityIdentifier("shortcuts.smartCase")
                    Divider()
                    SettingsToggleRow(
                        title: "Gõ tắt khi dùng tiếng Anh",
                        summary: "Vẫn thay chữ tắt khi bàn phím ở chế độ tiếng Anh.",
                        isOn: model.binding(\.inEnglish)
                    )
                    .accessibilityIdentifier("shortcuts.inEnglish")
                }
                .disabled(!model.canWrite)
                Text("Các tuỳ chọn có hiệu lực khi mở lại bàn phím Funput.")
                    .font(.caption).foregroundStyle(.secondary)
            }
            .navigationTitle("Tuỳ chọn gõ tắt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Xong") { dismiss() }
                        .disabled(model.isSaving)
                }
            }
        }
        .presentationDetents(dynamicTypeSize.isAccessibilitySize ? [.large] : [.medium, .large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(model.isSaving)
        .shortcutsSaveAlert(model)
    }
}

#if DEBUG
#Preview("Tuỳ chọn") {
    let model = ShortcutsModel(store: ShortcutsPreviewStore())
    ShortcutsOptions(model: model).task { await model.reload() }
}
#endif
