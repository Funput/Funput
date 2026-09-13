import SwiftUI

struct ClipboardSettingsCard: View {
    @Binding var isEnabled: Bool
    let expiryLabel: String
    let selectExpiry: () -> Void
    let openSettings: () -> Void
    let clear: () -> Void

    @State private var confirmsClear = false

    var body: some View {
        SettingsSectionCard(title: "Lịch sử clipboard", systemImage: "clipboard") {
            SettingsToggleRow(
                title: "Lưu lịch sử clipboard",
                // The one place a user with existing entries will actually read this:
                // the empty state that explains it is, by definition, never shown to them.
                summary: "Tự lưu văn bản đã sao chép khi Funput hoạt động, trên thiết bị. Cần cho phép đọc clipboard.",
                isOn: $isEnabled
            )
            if isEnabled {
                pastePermissionGuide
            }
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

    /// iOS asks for permission once per copied item, so without this setting the
    /// feature works but greets every copy with an alert. There is no API to read the
    /// current choice, so the guidance is always offered rather than hidden once granted.
    private var pastePermissionGuide: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label {
                Text("Để Funput tự lưu mà không hỏi lại sau mỗi lần sao chép, đặt **Dán từ ứng dụng khác** thành **Cho phép**.")
            } icon: {
                Image(systemName: "doc.on.clipboard").foregroundStyle(.tint)
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .fixedSize(horizontal: false, vertical: true)
            Button(action: openSettings) {
                Label("Mở Cài đặt", systemImage: "gear")
            }
            .buttonStyle(.bordered)
            .accessibilityHint("Mở cài đặt của Funput để cho phép dán từ ứng dụng khác")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, 10)
        .accessibilityElement(children: .contain)
    }
}
