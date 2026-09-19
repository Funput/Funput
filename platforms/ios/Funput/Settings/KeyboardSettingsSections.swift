import FunputShared
import KeyboardLayout
import SwiftUI

/// "Gõ tiếng Việt": what the keyboard produces — method, tone placement, language, shortcuts.
struct TypingSettingsSection: View {
    @ObservedObject var model: SettingsModel
    @ObservedObject var shortcuts: ShortcutsModel
    let select: (SettingsPicker) -> Void

    var body: some View {
        SettingsSectionCard(title: "Gõ tiếng Việt", systemImage: "character.bubble") {
            SettingsSelectionRow(option: .inputMethod, value: model.inputMethodLabel) { select(.inputMethod) }
            SettingsRowDivider()
            SettingsSelectionRow(option: .toneStyle, value: model.toneStyleLabel) { select(.toneStyle) }
            SettingsRowDivider()
            SettingsSelectionRow(option: .language, value: model.languageLabel) { select(.language) }
            SettingsRowDivider()
            ShortcutsSettingsLink(model: shortcuts)
        }
    }
}

/// "Bố cục bàn phím": how the keyboard looks on screen — keys, number row, height.
struct LayoutSettingsSection: View {
    @ObservedObject var model: SettingsModel
    let select: (SettingsPicker) -> Void

    var body: some View {
        SettingsSectionCard(title: "Bố cục bàn phím", systemImage: "keyboard") {
            SettingsSelectionRow(option: .layoutPreset, value: model.layoutPresetLabel) { select(.layoutPreset) }
            SettingsRowDivider()
            SettingsSelectionRow(option: .keySizing, value: model.keySizingLabel) { select(.keySizing) }
            // VNI types tones with digits, so the row is always on there: nothing to choose.
            if !model.isNumberRowLocked {
                SettingsRowDivider()
                SettingsToggleRow(
                    title: "Hàng phím số",
                    summary: "Hiển thị 0–9 phía trên bàn phím.",
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
struct SmartInputSettingsSection: View {
    @ObservedObject var model: SettingsModel

    var body: some View {
        SettingsSectionCard(
            title: "Thông minh",
            systemImage: "wand.and.stars",
            footer: "Gợi ý chỉ học chữ gõ bằng Funput và lưu trên thiết bị."
        ) {
            toggle("Khôi phục từ thông minh", "Giữ từ gốc khi sửa hoặc xóa dấu.", "arrow.uturn.backward", \.smartRestore)
            SettingsRowDivider()
            toggle("Khôi phục sớm", "Khôi phục từ ngay trong lúc gõ.", "bolt", \.eagerRestore)
            SettingsRowDivider()
            toggle("Kiểm tra chính tả", "Nhận diện từ tiếng Việt chưa hợp lệ.", "checkmark.circle", \.spellCheck)
            SettingsRowDivider()
            toggle("Tự viết hoa", "Viết hoa chữ đầu câu.", "textformat.size.larger", \.autoCapitalize)
            SettingsRowDivider()
            toggle("Hiện gợi ý từ", "Từ đã học và từ điển tiếng Anh.", "text.badge.star", \.personalSuggestionsEnabled)
            SettingsRowDivider()
            toggle(
                "Tự sửa lỗi gõ nhầm phím",
                "Sửa từ gõ trượt sang phím bên cạnh khi kết thúc từ.",
                "hand.point.up.braille",
                \.typoCorrection
            )
            SettingsRowDivider()
            // One switch for every movement-based gesture: someone who dislikes one of
            // them almost always wants all three gone.
            toggle(
                "Cử chỉ thông minh",
                "Chạm đúp phím cách để chấm câu, giữ phím cách để di con trỏ, vuốt trái phím xóa để xóa từ.",
                "hand.draw",
                \.smartGesturesEnabled
            )
        }
    }

    private func toggle(
        _ title: String,
        _ summary: String,
        _ systemImage: String,
        _ keyPath: WritableKeyPath<FunputConfiguration, Bool>
    ) -> some View {
        SettingsToggleRow(title: title, summary: summary, systemImage: systemImage, isOn: model.boolBinding(keyPath))
    }
}
