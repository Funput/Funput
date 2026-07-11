import KeyboardLayout
import SwiftUI

struct KeyboardLabLayoutControls: View {
    @Binding var configuration: KeyboardLabConfiguration

    var body: some View {
        VStack(spacing: 14) {
            Picker("Kiểu gõ", selection: $configuration.inputMethod) {
                ForEach(KeyboardInputMethod.allCases, id: \.self) { method in
                    Text(method.title).tag(method)
                }
            }
            .pickerStyle(.segmented)

            pickerRow("Ngữ cảnh") {
                Picker("Ngữ cảnh", selection: $configuration.editorMode) {
                    ForEach(KeyboardEditorMode.allCases, id: \.self) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
            }

            if configuration.supportsLayoutModeSelection {
                pickerRow("Trang phím") {
                    Picker("Trang phím", selection: $configuration.layoutMode) {
                        ForEach(KeyboardLayoutMode.allCases, id: \.self) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                }
            }

            if configuration.supportsLanguageSwipe {
                Picker("Ngôn ngữ", selection: $configuration.language) {
                    ForEach(KeyboardLanguage.allCases, id: \.self) { language in
                        Text(language.displayLabel).tag(language)
                    }
                }
                .pickerStyle(.segmented)
            }

            pickerRow("Phím Enter") {
                Picker("Phím Enter", selection: $configuration.enterAction) {
                    ForEach(KeyboardLabEnterAction.allCases) { action in
                        Text(action.title).tag(action)
                    }
                }
            }

            Toggle("Hiện nút chuyển bàn phím", isOn: $configuration.showsSystemInputModeKey)
                .disabled(!configuration.supportsSystemInputModePreview)
        }
    }

    private func pickerRow<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        LabeledContent(title) {
            content()
                .pickerStyle(.menu)
                .labelsHidden()
        }
    }
}
