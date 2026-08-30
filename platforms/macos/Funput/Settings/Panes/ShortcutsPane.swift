import SwiftUI

/// Manage text-expansion shortcuts (gõ tắt): type a short trigger, then a space or
/// punctuation, and Funput expands it (`vn` → `Việt Nam`). Rows are edited inline.
struct ShortcutsPane: View {
    @Environment(AppSettings.self) var settings
    @FocusState var focusedTrigger: UUID?

    var body: some View {
        @Bindable var settings = settings

        Group {
            Section("Danh sách gõ tắt") {
                VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                    SettingsRow(
                        title: "Gõ tắt",
                        subtitle: "Gõ chuỗi tắt rồi dấu cách để bung — ví dụ vn → Việt Nam",
                        systemImage: "text.append"
                    ) {
                        Button(action: addRow) {
                            Label("Thêm", systemImage: "plus")
                        }
                        .disabled(!canAddRow)
                        .help(
                            canAddRow
                                ? "Thêm gõ tắt mới"
                                : "Điền đầy đủ trigger và nội dung của dòng hiện tại trước khi thêm dòng mới"
                        )
                    }

                    if settings.shortcuts.isEmpty {
                        Divider()
                        Text("Chưa có gõ tắt nào. Bấm “Thêm” để tạo — ví dụ vn → Việt Nam, kg → không.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    } else {
                        ForEach($settings.shortcuts) { $shortcut in
                            Divider()
                            row($shortcut)
                        }
                    }
                }
            }

            Section("Mẹo") {
                VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                    Text("Trigger tự nhận diện hoa/thường — gõ `vn`, `Vn` hay `VN` đều bung, expansion tự viết hoa tương ứng. Gõ tắt được ưu tiên hơn tự động khôi phục tiếng Anh.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    // MARK: - Helpers

    /// "Thêm" is only enabled when the list is empty, or the last row already
    /// has both a trigger and an expansion — avoids piling up empty/half-filled
    /// rows before finishing the current one.
    private var canAddRow: Bool {
        guard let last = settings.shortcuts.last else { return true }
        return !last.trigger.isEmpty && !last.expansion.isEmpty
    }

    /// Triggers (non-empty) that appear on more than one row — flagged so the user
    /// knows the engine map keeps only the last one.
    var duplicateTriggers: Set<String> {
        var seen = Set<String>()
        var duplicates = Set<String>()
        for shortcut in settings.shortcuts where !shortcut.trigger.isEmpty {
            if !seen.insert(shortcut.trigger).inserted {
                duplicates.insert(shortcut.trigger)
            }
        }
        return duplicates
    }

    private func addRow() {
        settings.addShortcut()
        focusedTrigger = settings.shortcuts.last?.id
    }
}

#Preview {
    let settings = AppSettings.shared
    settings.shortcuts = [
        TextShortcut(trigger: "vn", expansion: "Việt Nam"),
        TextShortcut(trigger: "kg", expansion: "không"),
    ]
    return ShortcutsPane()
        .environment(settings)
        .padding(Theme.Spacing.xl)
        .frame(width: 560)
}
