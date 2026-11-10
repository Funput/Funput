import FunputShared
import SwiftUI

struct KeySoundPicker: View {
    let selection: KeySoundStyle?
    let select: () -> Void

    var body: some View {
        Button(action: select) {
            SettingsRowLabel(
                title: "Âm thanh khi gõ",
                summary: "Chọn kiểu âm và độ to khi chạm phím.",
                systemImage: "speaker.wave.2",
                value: selection?.settingsTitle ?? "Tắt"
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Âm thanh khi gõ, \(selection?.settingsTitle ?? "Tắt")")
        .accessibilityHint("Chạm hai lần để thay đổi")
        .accessibilityIdentifier("settings.sound")
    }
}

extension KeySoundStyle {
    var settingsTitle: String {
        switch self {
        case .system: "Mặc định"
        case .mechanical: "Phím cơ"
        case .wood: "Gỗ"
        case .bubble: "Bong bóng"
        case .typewriter: "Máy chữ"
        case .marimba: "Marimba"
        }
    }

    var settingsSummary: String {
        switch self {
        case .system: "Tiếng click của iOS"
        case .mechanical: "Lách cách như bàn phím cơ"
        case .wood: "Trầm và ấm như gõ lên gỗ"
        case .bubble: "Tròn và vui tai"
        case .typewriter: "Tách tách như máy chữ cổ"
        case .marimba: "Mỗi phím một nốt nhạc gỗ ngắn"
        }
    }

    var settingsSymbol: String {
        switch self {
        case .system: "speaker.wave.2"
        case .mechanical: "keyboard"
        case .wood: "cube"
        case .bubble: "bubbles.and.sparkles"
        case .typewriter: "doc.plaintext"
        case .marimba: "music.note"
        }
    }
}

extension KeySoundVolume {
    var settingsTitle: String {
        switch self {
        case .soft: "Nhỏ"
        case .medium: "Vừa"
        case .loud: "To"
        }
    }
}
