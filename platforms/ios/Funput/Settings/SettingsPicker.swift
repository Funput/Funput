import FunputShared
import KeyboardLayout

enum SettingsPicker: String, Identifiable {
    case inputMethod
    case language
    case toneStyle
    case clipboardExpiry

    var id: String { rawValue }

    var title: String {
        switch self {
        case .inputMethod: "Kiểu gõ"
        case .language: "Ngôn ngữ"
        case .toneStyle: "Kiểu đặt dấu"
        case .clipboardExpiry: "Tự xoá sau"
        }
    }

    var summary: String {
        switch self {
        case .inputMethod: "Cách nhập dấu tiếng Việt"
        case .language: "Ngôn ngữ chính của bàn phím"
        case .toneStyle: "Vị trí dấu trong một số vần"
        case .clipboardExpiry: "Mục đã ghim không bao giờ tự xoá"
        }
    }

    var systemImage: String {
        switch self {
        case .inputMethod: "character.cursor.ibeam"
        case .language: "globe.asia.australia"
        case .toneStyle: "textformat"
        case .clipboardExpiry: "clock.arrow.trianglehead.counterclockwise.rotate.90"
        }
    }
}

struct SettingsChoice: Identifiable {
    let id: String
    let title: String
    let summary: String
    let isSelected: Bool
    let select: () -> Void
}

extension SettingsPicker {
    @MainActor func choices(model: SettingsModel) -> [SettingsChoice] {
        switch self {
        case .inputMethod:
            KeyboardInputMethod.allCases.map { value in
                SettingsChoice(
                    id: value.rawValue, title: value.settingsTitle,
                    summary: value.settingsSummary,
                    isSelected: model.configuration.inputMethod == value,
                    select: { model.update(\.inputMethod, to: value) }
                )
            }
        case .language:
            KeyboardLanguage.allCases.map { value in
                SettingsChoice(
                    id: value.rawValue, title: value.displayLabel,
                    summary: value == .vietnamese ? "Bộ gõ tiếng Việt đầy đủ." : "Bố cục chữ cái tiếng Anh.",
                    isSelected: model.configuration.language == value,
                    select: { model.update(\.language, to: value) }
                )
            }
        case .toneStyle:
            ToneStyleOption.allCases.map { value in
                SettingsChoice(
                    id: value.rawValue, title: value.settingsTitle,
                    summary: value == .traditional ? "Đặt dấu như “hoà”." : "Đặt dấu như “hòa”.",
                    isSelected: model.configuration.toneStyle == value,
                    select: { model.update(\.toneStyle, to: value) }
                )
            }
        case .clipboardExpiry:
            ClipboardExpiry.allCases.map { value in
                SettingsChoice(
                    id: value.rawValue, title: value.title, summary: value.summary,
                    isSelected: model.configuration.clipboardExpiry == value,
                    select: { model.update(\.clipboardExpiry, to: value) }
                )
            }
        }
    }
}
