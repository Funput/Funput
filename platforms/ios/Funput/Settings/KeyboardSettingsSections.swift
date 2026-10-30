import FunputShared
import KeyboardLayout
import SwiftUI

/// "Gõ tiếng Việt": what the keyboard produces — method, tone placement, language.
struct TypingSettingsSection: View {
    @ObservedObject var model: SettingsModel
    let select: (SettingsPicker) -> Void

    var body: some View {
        SettingsSectionCard(title: SettingsDestination.typing.title, systemImage: "character.bubble") {
            SettingsSelectionRow(option: .inputMethod, value: model.inputMethodLabel) { select(.inputMethod) }
            SettingsRowDivider()
            SettingsSelectionRow(option: .toneStyle, value: model.toneStyleLabel) { select(.toneStyle) }
            SettingsRowDivider()
            // A language to choose between only exists while the space bar can switch.
            if !model.isLanguageLocked {
                SettingsSelectionRow(option: .language, value: model.languageLabel) { select(.language) }
                SettingsRowDivider()
            }
            SettingsToggleRow(
                title: "Đổi ngôn ngữ ở phím cách",
                summary: "Vuốt ngang phím cách để đổi Tiếng Việt và Tiếng Anh. Tắt thì Funput luôn gõ tiếng Việt.",
                systemImage: "arrow.left.arrow.right",
                isOn: model.languageToggleBinding
            )
        }
    }
}

/// "Bố cục bàn phím": how the keyboard looks on screen — keys, number row, height.
struct LayoutSettingsSection: View {
    @ObservedObject var model: SettingsModel
    let select: (SettingsPicker) -> Void

    var body: some View {
        SettingsSectionCard(title: SettingsDestination.layout.title, systemImage: "keyboard") {
            SettingsSelectionRow(option: .layoutPreset, value: model.layoutPresetLabel) { select(.layoutPreset) }
            SettingsRowDivider()
            SettingsSelectionRow(option: .keySizing, value: model.keySizingLabel) { select(.keySizing) }
            SettingsRowDivider()
            ToolbarSettingsRow(model: model)
            // VNI types tones with digits, so the row is always on there: nothing to choose.
            if !model.isNumberRowLocked {
                SettingsRowDivider()
                SettingsToggleRow(
                    title: "Hàng phím số",
                    summary: "Hiển thị các số từ 0 đến 9 phía trên bàn phím.",
                    systemImage: "textformat.123",
                    isOn: model.numberRowBinding
                )
            }
            // System sizing uses Apple's row heights, so there is nothing to scale.
            if model.configuration.keySizing == .funput {
                SettingsRowDivider()
                SettingsHeightRow(value: model.heightBinding)
            }
        }
    }
}

/// "Thông minh": every feature that reads intent — restore, spelling, suggestions, gestures.
///
/// The suggestion bar's switches get a card of their own, with the way into the words
/// it has learned; everything that acts while typing shares the other.
struct SmartInputSettingsSection: View {
    @ObservedObject var model: SettingsModel

    var body: some View {
        SettingsSectionCard(
            title: "Gợi ý từ",
            systemImage: "text.badge.star",
            footer: "Gợi ý chỉ học chữ gõ bằng Funput và lưu trên thiết bị."
        ) {
            toggles(SmartInputToggle.suggestions)
            SettingsRowDivider()
            SettingsNavigationRow(destination: .personalDictionary, summary: "Xem, thêm và chặn từ")
        }
        SettingsSectionCard(title: "Khi gõ", systemImage: "keyboard") {
            toggles(SmartInputToggle.typing)
        }
    }

    private func toggles(_ toggles: [SmartInputToggle]) -> some View {
        ForEach(toggles) { toggle in
            if toggle.id != toggles.first?.id {
                SettingsRowDivider()
            }
            SettingsToggleRow(
                title: toggle.title,
                summary: toggle.summary,
                systemImage: toggle.systemImage,
                isOn: model.boolBinding(toggle.keyPath)
            )
        }
    }
}
