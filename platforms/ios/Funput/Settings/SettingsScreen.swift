import FunputShared
import SwiftUI

struct SettingsScreen: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: SettingsModel
    @State private var picker: SettingsPicker?
    @State private var confirmsReset = false

    init(store: any FunputConfigurationStoring = FunputConfigurationStore()) {
        _model = State(initialValue: SettingsModel(store: store))
    }

    var body: some View {
        AppScreen {
            SettingsHero()
            KeyboardSetupCard()
            SettingsSectionCard(title: "Bàn phím", systemImage: "keyboard") {
                SettingsSelectionRow(option: .inputMethod, value: model.inputMethodLabel) { picker = .inputMethod }
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
            SettingsSectionCard(title: "Phản hồi", systemImage: "hand.tap") {
                SettingsToggleRow(
                    title: "Rung khi gõ",
                    summary: "Tạo phản hồi xúc giác nhẹ khi chạm phím.",
                    isOn: model.boolBinding(\.isHapticFeedbackEnabled)
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
        .alert("Không thể lưu cài đặt", isPresented: model.saveErrorBinding) {
            Button("Đóng", role: .cancel) {}
        } message: {
            Text("Funput đã giữ nguyên giá trị trước đó. Vui lòng thử lại.")
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
