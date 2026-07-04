import AppKit
import ServiceManagement
import SwiftUI
import UniformTypeIdentifiers

struct GeneralPane: View {
    @Environment(AppSettings.self) private var settings
    @State private var alertMessage: String?

    var body: some View {
        @Bindable var settings = settings

        VStack(alignment: .leading, spacing: Theme.Spacing.lg) {
            GlassCard {
                VStack(spacing: Theme.Spacing.md) {
                    SettingsRow(
                        title: "Khởi động cùng máy",
                        subtitle: "Tự chạy Funput khi đăng nhập",
                        systemImage: "power"
                    ) {
                        Toggle("", isOn: $settings.launchAtLogin)
                            .labelsHidden()
                            .toggleStyle(.switch)
                            .onChange(of: settings.launchAtLogin) { _, enabled in
                                LoginItem.setEnabled(enabled)
                            }
                    }
                    Divider()
                    SettingsRow(
                        title: "Hiện biểu tượng thanh menu",
                        subtitle: "Đổi nhanh Telex/VNI và mở cài đặt",
                        systemImage: "menubar.rectangle"
                    ) {
                        Toggle("", isOn: $settings.showMenuBarIcon)
                            .labelsHidden()
                            .toggleStyle(.switch)
                    }
                }
            }

            GlassCard {
                VStack(spacing: Theme.Spacing.md) {
                    SettingsRow(
                        title: "Xuất cấu hình",
                        subtitle: "Lưu gõ tắt và tuỳ chọn ra tệp .json",
                        systemImage: "square.and.arrow.up"
                    ) {
                        Button("Xuất…", action: exportConfig)
                            .buttonStyle(.glass)
                    }
                    Divider()
                    SettingsRow(
                        title: "Nhập cấu hình",
                        subtitle: "Gộp gõ tắt và áp tuỳ chọn từ tệp .json",
                        systemImage: "square.and.arrow.down"
                    ) {
                        Button("Nhập…", action: importConfig)
                            .buttonStyle(.glass)
                    }
                }
            }
        }
        .alert(
            "Cấu hình",
            isPresented: Binding(get: { alertMessage != nil },
                                 set: { if !$0 { alertMessage = nil } })
        ) {
            Button("OK", role: .cancel) {}
        } message: {
            if let alertMessage { Text(alertMessage) }
        }
    }

    // MARK: - Export / Import

    private func exportConfig() {
        let panel = NSSavePanel()
        panel.allowedContentTypes = [.json]
        panel.canCreateDirectories = true
        panel.nameFieldStringValue = "Funput-config-\(Self.dateStamp).json"
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            try settings.exportData().write(to: url)
        } catch {
            alertMessage = "Không xuất được cấu hình: \(error.localizedDescription)"
        }
    }

    private func importConfig() {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = [.json]
        panel.allowsMultipleSelection = false
        guard panel.runModal() == .OK, let url = panel.url else { return }
        do {
            let summary = try settings.importData(Data(contentsOf: url))
            alertMessage = Self.importMessage(summary)
        } catch {
            alertMessage = error.localizedDescription
        }
    }

    private static var dateStamp: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }

    private static func importMessage(_ summary: ConfigImportSummary) -> String {
        var lines = ["Đã áp các tuỳ chọn gõ."]
        if summary.shortcutsAdded > 0 || summary.shortcutsUpdated > 0 {
            lines.append("Gõ tắt: thêm \(summary.shortcutsAdded), cập nhật \(summary.shortcutsUpdated).")
        } else {
            lines.append("Không có gõ tắt mới.")
        }
        if summary.appliedMacPlatform {
            lines.append("Đã áp phím tắt và danh sách app bỏ qua.")
        }
        if summary.newerVersion {
            lines.append("Lưu ý: tệp từ phiên bản mới hơn — một số mục có thể bị bỏ qua.")
        }
        return lines.joined(separator: "\n")
    }
}

/// Register/unregister Funput as a login item (best-effort; silent in unsigned dev builds).
private enum LoginItem {
    static func setEnabled(_ enabled: Bool) {
        do {
            if enabled {
                try SMAppService.mainApp.register()
            } else {
                try SMAppService.mainApp.unregister()
            }
        } catch {
            // Ignore — typically only fails for unsigned/dev builds.
        }
    }
}

#Preview {
    GeneralPane()
        .environment(AppSettings.shared)
        .padding(Theme.Spacing.xl)
        .frame(width: 520)
}
