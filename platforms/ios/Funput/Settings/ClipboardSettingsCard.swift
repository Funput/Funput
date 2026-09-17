import SwiftUI

struct ClipboardSettingsCard: View {
    @Binding var isEnabled: Bool
    let expiryLabel: String
    let selectExpiry: () -> Void
    let openSettings: () -> Void

    var body: some View {
        SettingsSectionCard(title: "Clipboard", systemImage: "clipboard") {
            SettingsToggleRow(
                title: "Lưu lịch sử clipboard",
                // The one place a user with existing entries will actually read this:
                // the empty state that explains it is, by definition, never shown to them.
                summary: "Tự lưu văn bản đã sao chép khi Funput hoạt động, trên thiết bị. Cần cho phép đọc clipboard.",
                systemImage: "doc.on.clipboard",
                isOn: $isEnabled
            )
            if isEnabled {
                pastePermissionGuide
            }
            SettingsRowDivider()
            SettingsSelectionRow(option: .clipboardExpiry, value: expiryLabel, action: selectExpiry)
        }
    }

    /// iOS asks for permission once per copied item, so without this setting the
    /// feature works but greets every copy with an alert. There is no API to read the
    /// current choice, so the guidance is always offered rather than hidden once granted.
    private var pastePermissionGuide: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Để không bị hỏi lại sau mỗi lần sao chép, đặt **Dán từ ứng dụng khác** thành **Cho phép**.")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Button(action: openSettings) {
                Label("Mở Cài đặt", systemImage: "gear")
            }
            .settingsActionButtonStyle()
            .accessibilityHint("Mở cài đặt của Funput để cho phép dán từ ứng dụng khác")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.leading, 34)
        .padding(.bottom, 10)
        .accessibilityElement(children: .contain)
    }
}
