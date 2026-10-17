import FunputShared
import SwiftUI

/// The "Phản hồi khi chạm" card: how the keyboard answers a touch.
struct FeedbackSettingsCard: View {
    let hapticStrength: HapticFeedbackStrength?
    let selectHapticStrength: () -> Void
    @Binding var sound: Bool
    @Binding var keyPreviews: Bool

    var body: some View {
        SettingsSectionCard(
            title: SettingsDestination.feedback.title,
            systemImage: "hand.tap",
            footer: "Rung và âm thanh cần Cho phép truy cập đầy đủ."
        ) {
            HapticFeedbackPicker(selection: hapticStrength, select: selectHapticStrength)
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
