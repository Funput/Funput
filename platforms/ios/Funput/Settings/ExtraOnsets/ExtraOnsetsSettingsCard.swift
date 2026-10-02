import FunputShared
import KeyboardLayout
import SwiftUI

struct ExtraOnsetsSettingsCard: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ObservedObject var model: SettingsModel

    var body: some View {
        SettingsSectionCard(
            title: "Phụ âm đầu mở rộng",
            systemImage: "character.cursor.ibeam",
            footer: footer
        ) {
            SettingsToggleRow(
                title: "Cho phép z, f, w, j đầu từ",
                summary: "Gõ teencode, từ mượn, tên riêng: zô, fải, wá, jờ.",
                systemImage: "character.cursor.ibeam",
                isOn: model.extraOnsetsEnabledBinding
            )
            if !model.configuration.extraOnsets.isEmpty {
                ForEach(ExtraOnsetLetters.letters) { letter in
                    SettingsRowDivider()
                    SettingsToggleRow(
                        title: "Chữ \(letter.symbol)",
                        summary: letter.examples,
                        systemImage: "\(letter.symbol).square",
                        isOn: model.extraOnsetBinding(letter.member)
                    )
                }
            }
        }
        .animation(expansionAnimation, value: model.configuration.extraOnsets.isEmpty)
    }

    private var footer: String {
        var text = "Từ tiếng Anh có vần tiếng Việt cũng được bỏ dấu (fast → fát) — "
            + "gõ đúp phím dấu (fasst) để giữ tiếng Anh."
        if model.configuration.inputMethod == .telexAdvanced,
           model.configuration.extraOnsets.contains(.w) {
            text += "\nTelex nâng cao: w vẫn là ư — gõ ww để có phụ âm w."
        }
        return text
    }

    private var expansionAnimation: Animation? {
        guard !reduceMotion else { return nil }
        if #available(iOS 17, *) { return .snappy }
        return .easeInOut(duration: 0.2)
    }
}
