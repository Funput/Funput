import AppKit
import Foundation
import Observation
import UniformTypeIdentifiers

@MainActor
@Observable
final class SettingsCommandHandler {
    var alertMessage: String?

    func exportConfig(from settings: AppSettings) {
        let panel = NSSavePanel()
        panel.allowedContentTypes = [.json]
        panel.canCreateDirectories = true
        panel.nameFieldStringValue = "Funput-config-\(Self.dateStamp).json"
        guard panel.runModal() == .OK, let url = panel.url else { return }

        do {
            try settings.exportData().write(to: url)
            alertMessage = "Đã xuất cấu hình Funput."
        } catch {
            alertMessage = "Không xuất được cấu hình: \(error.localizedDescription)"
        }
    }

    func importConfig(into settings: AppSettings) {
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
        var lines = ["Đã áp các tùy chọn gõ."]
        if summary.shortcutsAdded > 0 || summary.shortcutsUpdated > 0 {
            lines.append("Gõ tắt: thêm \(summary.shortcutsAdded), cập nhật \(summary.shortcutsUpdated).")
        } else {
            lines.append("Không có gõ tắt mới.")
        }
        if summary.appliedMacPlatform {
            lines.append("Đã áp phím tắt và danh sách app bỏ qua.")
        }
        if summary.newerVersion {
            lines.append("Tệp được tạo bởi phiên bản mới hơn; một số mục có thể bị bỏ qua.")
        }
        return lines.joined(separator: "\n")
    }
}
