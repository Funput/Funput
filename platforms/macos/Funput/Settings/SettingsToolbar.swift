import SwiftUI

struct SettingsToolbar: ToolbarContent {
    @Environment(AppSettings.self) private var settings
    @Environment(UpdaterManager.self) private var updater

    let onImport: () -> Void
    let onExport: () -> Void
    let onAbout: () -> Void

    var body: some ToolbarContent {
        ToolbarItemGroup(placement: .primaryAction) {
            Button(action: toggleVietnamese) {
                Label(
                    settings.vietnameseEnabled ? "VI" : "EN",
                    systemImage: settings.vietnameseEnabled ? "checkmark.circle.fill" : "pause.circle"
                )
            }
            .help(settings.vietnameseEnabled ? "Tạm dừng tiếng Việt" : "Bật tiếng Việt")

            Menu {
                @Bindable var settings = settings
                Picker("Phương thức gõ", selection: $settings.inputMethod) {
                    ForEach(InputMethod.displayCases) { method in
                        Text(method.displayName).tag(method)
                    }
                }
            } label: {
                Label(settings.inputMethod.displayName, systemImage: "keyboard")
            }

            Menu {
                Button("Nhập cấu hình…", systemImage: "square.and.arrow.down", action: onImport)
                Button("Xuất cấu hình…", systemImage: "square.and.arrow.up", action: onExport)
                Divider()
                Button("Kiểm tra cập nhật…", systemImage: "arrow.triangle.2.circlepath") {
                    updater.checkForUpdates()
                }
                .disabled(!updater.canCheckForUpdates)
                Button("Giới thiệu Funput", systemImage: "info.circle", action: onAbout)
            } label: {
                Label("Thêm", systemImage: "ellipsis")
            }
        }
    }

    private func toggleVietnamese() {
        settings.vietnameseEnabled.toggle()
    }
}
