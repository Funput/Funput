import SwiftUI

/// The "Phản hồi khi chạm" card: how the keyboard answers a touch.
///
/// Haptics and sound need Full Access, so their bindings arrive already wrapped by
/// ``SettingsModel/fullAccessBinding(_:onDenied:)`` — this view only lays them out.
struct FeedbackSettingsCard: View {
    @Binding var haptics: Bool
    @Binding var sound: Bool
    @Binding var keyPreviews: Bool

    var body: some View {
        SettingsSectionCard(
            title: "Phản hồi khi chạm",
            systemImage: "hand.tap",
            footer: "Rung và âm thanh cần Cho phép truy cập đầy đủ."
        ) {
            SettingsToggleRow(
                title: "Rung khi gõ",
                summary: "Rung nhẹ mỗi lần chạm phím.",
                systemImage: "iphone.radiowaves.left.and.right",
                isOn: $haptics
            )
            SettingsRowDivider()
            SettingsToggleRow(
                title: "Âm thanh khi gõ",
                summary: "Phát tiếng click khi chạm phím.",
                systemImage: "speaker.wave.2",
                isOn: $sound
            )
            SettingsRowDivider()
            SettingsToggleRow(
                title: "Xem trước phím",
                summary: "Hiện ký tự phóng lớn khi chạm phím.",
                systemImage: "character.magnify",
                isOn: $keyPreviews
            )
        }
    }
}
