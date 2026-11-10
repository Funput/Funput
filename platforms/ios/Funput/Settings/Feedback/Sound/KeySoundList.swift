import FunputShared
import SwiftUI

/// A single-choice list of sound styles, with off first.
struct KeySoundList: View {
    let selection: KeySoundStyle?
    let select: (KeySoundStyle?) -> Void
    private let options: [KeySoundStyle?] = [nil] + KeySoundStyle.allCases

    var body: some View {
        FeedbackOptionStack(options: options) { style in
            let selected = selection == style
            FeedbackOptionRow(
                title: style?.settingsTitle ?? "Tắt",
                summary: style?.settingsSummary ?? "Không phát âm thanh khi gõ",
                selected: selected,
                hint: style == nil ? "Tắt âm thanh của bàn phím" : "Chọn và nghe thử kiểu âm",
                identifier: "settings.sound.\(style?.rawValue ?? "off")",
                select: { select(style) }
            ) {
                Image(systemName: style?.settingsSymbol ?? "speaker.slash")
                    .font(.body.weight(.medium))
                    .foregroundStyle(selected ? Color.accentColor : Color.secondary)
            }
        }
    }
}
