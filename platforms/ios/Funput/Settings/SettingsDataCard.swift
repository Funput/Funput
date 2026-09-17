import SwiftUI

/// Every destructive action on the screen, in one place and away from the switches.
struct SettingsDataCard: View {
    let resetPersonalSuggestions: () -> Void
    let clearClipboard: () -> Void
    let resetSettings: () -> Void

    var body: some View {
        SettingsSectionCard(title: "Dữ liệu & khôi phục", systemImage: "externaldrive") {
            SettingsDestructiveRow(
                title: "Từ đã học", summary: "Giữ từ điển tiếng Anh đi kèm.",
                systemImage: "text.badge.xmark", actionTitle: "Xóa", identifier: "settings.data.suggestions",
                hint: "Xóa từ đã học ở lần mở bàn phím tiếp theo, giữ từ điển tiếng Anh",
                dialogTitle: "Xóa toàn bộ từ đã học?", confirmTitle: "Xóa từ đã học",
                message: "Xóa từ đã học khi Funput mở bàn phím lần tiếp theo. Từ điển tiếng Anh đi kèm vẫn được giữ.",
                action: resetPersonalSuggestions
            )
            SettingsRowDivider()
            SettingsDestructiveRow(
                title: "Lịch sử clipboard", summary: "Kể cả những mục đã ghim.",
                systemImage: "clipboard", actionTitle: "Xoá", identifier: "settings.data.clipboard",
                hint: "Xoá mọi mục trong lịch sử clipboard, kể cả mục đã ghim",
                dialogTitle: "Xoá toàn bộ lịch sử clipboard?", confirmTitle: "Xoá tất cả",
                message: "Xoá ngay lập tức, kể cả những mục đã ghim.",
                action: clearClipboard
            )
            SettingsRowDivider()
            SettingsDestructiveRow(
                title: "Cài đặt bộ gõ", summary: "Đưa về giá trị mặc định của Funput.",
                systemImage: "arrow.counterclockwise", actionTitle: "Khôi phục", identifier: "settings.data.reset",
                hint: "Khôi phục toàn bộ thiết lập bộ gõ về mặc định",
                dialogTitle: "Khôi phục cài đặt mặc định?", confirmTitle: "Khôi phục",
                message: "Các tùy chỉnh bộ gõ hiện tại sẽ bị thay thế.",
                action: resetSettings
            )
        }
    }
}

private struct SettingsDestructiveRow: View {
    let title: String
    let summary: String
    let systemImage: String
    let actionTitle: String
    let identifier: String
    let hint: String
    let dialogTitle: String
    let confirmTitle: String
    let message: String
    let action: () -> Void

    @State private var confirms = false

    var body: some View {
        HStack(spacing: 12) {
            SettingsRowIcon(systemImage: systemImage, tint: .red)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                Text(summary).font(.caption).foregroundStyle(.secondary)
            }
            Spacer(minLength: 12)
            Button(actionTitle, role: .destructive) { confirms = true }
                .settingsActionButtonStyle()
                .accessibilityLabel("\(actionTitle) \(title.lowercased())")
                .accessibilityHint(hint)
                .accessibilityIdentifier(identifier)
                // Anchored to the button rather than to the screen: iOS 26 points a
                // confirmation dialog at whatever presents it, so one hung off the
                // root view surfaces at the top instead of beside its control.
                .confirmationDialog(dialogTitle, isPresented: $confirms, titleVisibility: .visible) {
                    Button(confirmTitle, role: .destructive, action: action)
                } message: {
                    Text(message)
                }
        }
        .padding(.vertical, 10)
        .accessibilityElement(children: .contain)
    }
}
