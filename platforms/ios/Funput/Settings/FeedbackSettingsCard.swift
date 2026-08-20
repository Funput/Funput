import SwiftUI

/// The "Phản hồi" card: how the keyboard answers a touch.
///
/// Haptics and sound need Full Access, so their bindings arrive already wrapped by
/// ``SettingsModel/fullAccessBinding(_:onDenied:)`` — this view only lays them out.
struct FeedbackSettingsCard: View {
    @Binding var haptics: Bool
    @Binding var sound: Bool
    @Binding var keyPreviews: Bool

    var body: some View {
        SettingsSectionCard(title: "Phản hồi", systemImage: "hand.tap") {
            SettingsToggleRow(
                title: "Rung khi gõ",
                summary: "Yêu cầu Cho phép truy cập đầy đủ khi bật.",
                isOn: $haptics
            )
            SettingsToggleRow(
                title: "Âm thanh khi gõ",
                summary: "Phát tiếng click bàn phím khi chạm phím.",
                isOn: $sound
            )
            SettingsToggleRow(
                title: "Xem trước phím",
                summary: "Hiện ký tự phóng lớn khi bạn chạm phím.",
                isOn: $keyPreviews
            )
        }
    }
}
