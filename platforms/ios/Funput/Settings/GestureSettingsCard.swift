import SwiftUI

/// One switch for every gesture that reads intent from movement rather than from a tap.
///
/// They ship as a group because they share a failure mode: a gesture that fires when the
/// user only meant to type is worse than not having it, and someone who dislikes one of
/// them almost always wants all three gone.
struct GestureSettingsCard: View {
    @Binding var isEnabled: Bool

    var body: some View {
        SettingsSectionCard(title: "Cử chỉ", systemImage: "hand.draw") {
            SettingsToggleRow(
                title: "Cử chỉ thông minh",
                summary: "Gõ đúp phím cách để chấm câu, giữ phím cách rồi kéo để di chuyển "
                    + "con trỏ mọi hướng, vuốt trái phím xóa để xóa cả từ.",
                isOn: $isEnabled
            )
        }
    }
}
