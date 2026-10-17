import FunputShared
import SwiftUI

struct HapticFeedbackPicker: View {
    let selection: HapticFeedbackStrength?
    let select: () -> Void

    var body: some View {
        Button(action: select) {
            SettingsRowLabel(
                title: "Rung khi gõ",
                summary: "Chọn và cảm nhận mức rung của bàn phím.",
                systemImage: "iphone.radiowaves.left.and.right",
                value: selection?.settingsTitle ?? "Tắt"
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Rung khi gõ, \(selection?.settingsTitle ?? "Tắt")")
        .accessibilityHint("Chạm hai lần để thay đổi")
        .accessibilityIdentifier("settings.haptics")
    }
}
