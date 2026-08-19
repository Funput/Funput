import FunputShared
import SwiftUI
import ThemeRuntime
import UIKit

struct SettingsScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL
    @State private var model: SettingsModel
    @State private var picker: SettingsPicker?
    @State private var requestsHapticAccess = false
    @State private var requestsSoundAccess = false

    init(
        store: any FunputConfigurationStoring = FunputConfigurationStore(),
        customStore: any CustomThemeStoring = CustomThemeStore(),
        bootstrap: any KeyboardBootstrapSynchronizing = KeyboardBootstrapSynchronizer()
    ) {
        _model = State(initialValue: SettingsModel(
            store: store,
            customStore: customStore,
            bootstrap: bootstrap
        ))
    }

    var body: some View {
        AppScreen {
            KeyboardSetupCard(hasFullAccess: model.hasFullAccess) {
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
            SettingsSectionCard(title: "Bàn phím", systemImage: "keyboard") {
                SettingsSelectionRow(option: .inputMethod, value: model.inputMethodLabel) { picker = .inputMethod }
                SettingsSelectionRow(option: .layoutPreset, value: model.layoutPresetLabel) { picker = .layoutPreset }
                SettingsToggleRow(
                    title: "Hàng phím số",
                    summary: "Hiển thị 0–9 với các kiểu Telex. VNI luôn cần hàng số để nhập dấu.",
                    isOn: model.numberRowBinding
                )
                .disabled(model.isNumberRowLocked)
                SettingsSelectionRow(option: .language, value: model.languageLabel) { picker = .language }
                SettingsSelectionRow(option: .toneStyle, value: model.toneStyleLabel) { picker = .toneStyle }
                SettingsHeightRow(value: model.heightBinding)
            }
            SettingsSectionCard(title: "Nhập liệu thông minh", systemImage: "wand.and.stars") {
                SettingsToggleRow(
                    title: "Khôi phục từ thông minh",
                    summary: "Giữ từ gốc khi chỉnh sửa hoặc xóa dấu.",
                    isOn: model.boolBinding(\.smartRestore)
                )
                SettingsToggleRow(
                    title: "Khôi phục sớm",
                    summary: "Nhận biết và khôi phục từ ngay trong lúc gõ.",
                    isOn: model.boolBinding(\.eagerRestore)
                )
                SettingsToggleRow(
                    title: "Kiểm tra chính tả",
                    summary: "Hỗ trợ nhận diện các từ tiếng Việt chưa hợp lệ.",
                    isOn: model.boolBinding(\.spellCheck)
                )
                SettingsToggleRow(
                    title: "Tự viết hoa",
                    summary: "Viết hoa ký tự đầu câu khi phù hợp.",
                    isOn: model.boolBinding(\.autoCapitalize)
                )
            }
            PersonalSuggestionSettingsCard(
                isEnabled: model.boolBinding(\.personalSuggestionsEnabled),
                reset: { model.requestPersonalSuggestionReset() }
            )
            ClipboardSettingsCard(
                isEnabled: model.boolBinding(\.clipboardEnabled),
                expiryLabel: model.clipboardExpiryLabel,
                selectExpiry: { picker = .clipboardExpiry },
                clear: { model.clearClipboardHistory() }
            )
            FeedbackSettingsCard(
                haptics: model.fullAccessBinding(\.isHapticFeedbackEnabled) {
                    requestsHapticAccess = true
                },
                sound: model.fullAccessBinding(\.isKeySoundEnabled) {
                    requestsSoundAccess = true
                },
                keyPreviews: model.boolBinding(\.showsKeyPreviews)
            )
            GestureSettingsCard(isEnabled: model.boolBinding(\.smartGesturesEnabled))
            SettingsResetCard { model.reset() }
        }
        .navigationTitle("Cài đặt")
        .sheet(item: $picker) { SettingsSelectionSheet(picker: $0, model: model) }
        .alert("Không thể lưu cài đặt", isPresented: model.saveErrorBinding) {
            Button("Đóng", role: .cancel) {}
        } message: {
            Text("Funput đã giữ nguyên giá trị trước đó. Vui lòng thử lại.")
        }
        .alert("Cho phép truy cập đầy đủ", isPresented: $requestsHapticAccess) {
            Button("Hủy", role: .cancel) {}
            Button("Mở Cài đặt") {
                model.update(\.isHapticFeedbackEnabled, to: true)
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
        } message: {
            Text("Để rung hoạt động, hãy mở Funput trong Cài đặt, chọn Bàn phím và bật Cho phép truy cập đầy đủ.")
        }
        .alert("Cho phép truy cập đầy đủ", isPresented: $requestsSoundAccess) {
            Button("Hủy", role: .cancel) {}
            Button("Mở Cài đặt") {
                model.update(\.isKeySoundEnabled, to: true)
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
        } message: {
            Text("Để phát âm thanh khi gõ, hãy mở Funput trong Cài đặt, chọn Bàn phím và bật Cho phép truy cập đầy đủ.")
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.reload() }
        }
    }
}

#Preview("Cài đặt · Light") {
    NavigationStack {
        SettingsScreen(
            store: PreviewConfigurationStore(),
            customStore: PreviewCustomThemeStore(),
            bootstrap: NoopKeyboardBootstrapSynchronizer()
        )
    }
        .preferredColorScheme(.light)
}

#Preview("Cài đặt · Dark") {
    NavigationStack {
        SettingsScreen(
            store: PreviewConfigurationStore(),
            customStore: PreviewCustomThemeStore(),
            bootstrap: NoopKeyboardBootstrapSynchronizer()
        )
    }
        .preferredColorScheme(.dark)
}
