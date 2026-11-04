import SwiftUI

struct ClipboardSettingsCard: View {
    @Binding var isEnabled: Bool
    let expiryLabel: String
    let selectExpiry: () -> Void

    var body: some View {
        SettingsSectionCard(title: SettingsDestination.clipboard.title, systemImage: "clipboard") {
            SettingsToggleRow(
                title: "Lưu lịch sử clipboard",
                // The one place a user with existing entries will actually read this:
                // the empty state that explains it is, by definition, never shown to them.
                // No permission to grant: the keyboard never reads the clipboard on its
                // own, only what the user pastes through Funput's paste button.
                summary: "Lưu những gì bạn dán bằng Funput, ngay trên thiết bị.",
                systemImage: "doc.on.clipboard",
                isOn: $isEnabled
            )
            SettingsRowDivider()
            SettingsSelectionRow(option: .clipboardExpiry, value: expiryLabel, action: selectExpiry)
        }
    }
}
