import SwiftUI

struct ClipboardSettingsCard: View {
    @Binding var isEnabled: Bool
    let expiryLabel: String
    let selectExpiry: () -> Void
    let clear: () -> Void

    @State private var confirmsClear = false

    var body: some View {
        SettingsSectionCard(title: "Lịch sử clipboard", systemImage: "clipboard") {
            SettingsToggleRow(
                title: "Lưu lịch sử clipboard",
                // The one place a user with existing entries will actually read this:
                // the empty state that explains it is, by definition, never shown to them.
                summary: "Chỉ lưu những gì bạn đã dán qua Funput, trên thiết bị.",
                isOn: $isEnabled
            )
            SettingsSelectionRow(option: .clipboardExpiry, value: expiryLabel, action: selectExpiry)
            Button("Xoá tất cả", role: .destructive) { confirmsClear = true }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 10)
                .accessibilityHint("Xoá mọi mục trong lịch sử clipboard, kể cả mục đã ghim")
                // Attached to the button, not to the screen: iOS 26 anchors a
                // confirmation dialog to whatever presents it, so one hung off the root
                // view pops up at the top of the screen instead of beside its control.
                .confirmationDialog(
                    "Xoá toàn bộ lịch sử clipboard?",
                    isPresented: $confirmsClear,
                    titleVisibility: .visible
                ) {
                    Button("Xoá tất cả", role: .destructive, action: clear)
                } message: {
                    Text("Xoá ngay lập tức, kể cả những mục đã ghim.")
                }
        }
    }
}
