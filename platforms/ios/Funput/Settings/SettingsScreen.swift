import FunputShared
import SwiftUI
import UIKit

struct SettingsScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL
    @State private var model: SettingsModel
    @State private var picker: SettingsPicker?
    @State private var confirmsReset = false
    @State private var requestsHapticAccess = false
    @State private var requestsSoundAccess = false
    @State private var confirmsSuggestionReset = false

    init(store: any FunputConfigurationStoring = FunputConfigurationStore()) {
        _model = State(initialValue: SettingsModel(store: store))
    }

    var body: some View {
        AppScreen {
            KeyboardSetupCard(hasFullAccess: model.hasFullAccess) {
                openURL(URL(string: UIApplication.openSettingsURLString)!)
            }
            SettingsSectionCard(title: "Bàn phím", systemImage: "keyboard") {
                SettingsSelectionRow(option: .inputMethod, value: model.inputMethodLabel) { picker = .inputMethod }
                SettingsToggleRow(
                    title: "Hàng phím số",
                    summary: "Hiển thị 0–9 khi dùng Telex. VNI luôn cần hàng số để nhập dấu.",
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
                reset: { confirmsSuggestionReset = true }
            )
            SettingsSectionCard(title: "Phản hồi", systemImage: "hand.tap") {
                SettingsToggleRow(
                    title: "Rung khi gõ",
                    summary: "Yêu cầu Cho phép truy cập đầy đủ khi bật.",
                    isOn: model.fullAccessBinding(\.isHapticFeedbackEnabled) {
                        requestsHapticAccess = true
                    }
                )
                SettingsToggleRow(
                    title: "Âm thanh khi gõ",
                    summary: "Phát tiếng click bàn phím khi chạm phím.",
                    isOn: model.fullAccessBinding(\.isKeySoundEnabled) {
                        requestsSoundAccess = true
                    }
                )
                SettingsToggleRow(
                    title: "Xem trước phím",
                    summary: "Hiện ký tự phóng lớn khi bạn chạm phím.",
                    isOn: model.boolBinding(\.showsKeyPreviews)
                )
            }
            SettingsResetCard { confirmsReset = true }
        }
        .navigationTitle("Cài đặt")
        .sheet(item: $picker) { SettingsSelectionSheet(picker: $0, model: model) }
        .confirmationDialog("Khôi phục cài đặt mặc định?", isPresented: $confirmsReset) {
            Button("Khôi phục", role: .destructive) { model.reset() }
        } message: {
            Text("Các tùy chỉnh bộ gõ hiện tại sẽ bị thay thế.")
        }
        .confirmationDialog("Xóa toàn bộ từ đã học?", isPresented: $confirmsSuggestionReset) {
            Button("Xóa từ đã học", role: .destructive) {
                model.requestPersonalSuggestionReset()
            }
        } message: {
            Text("Lệnh xóa được thực hiện cục bộ khi Funput mở bàn phím lần tiếp theo.")
        }
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
    NavigationStack { SettingsScreen(store: PreviewConfigurationStore()) }
        .preferredColorScheme(.light)
}

#Preview("Cài đặt · Dark") {
    NavigationStack { SettingsScreen(store: PreviewConfigurationStore()) }
        .preferredColorScheme(.dark)
}
