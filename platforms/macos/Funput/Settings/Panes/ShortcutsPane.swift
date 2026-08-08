import SwiftUI

/// Manage text-expansion shortcuts (gõ tắt): type a short trigger, then a space or
/// punctuation, and Funput expands it (`vn` → `Việt Nam`). Rows are edited inline.
struct ShortcutsPane: View {
    @Environment(AppSettings.self) private var settings
    @FocusState private var focusedTrigger: UUID?

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

    // MARK: - Row

    private func row(_ shortcut: Binding<TextShortcut>) -> some View {
        let isDuplicate = !shortcut.wrappedValue.trigger.isEmpty
            && duplicateTriggers.contains(shortcut.wrappedValue.trigger)

        return VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            HStack(spacing: Theme.Spacing.md) {
                field(text: shortcut.trigger, placeholder: "vn", monospaced: true, invalid: isDuplicate)
                    .frame(width: 130)
                    .focused($focusedTrigger, equals: shortcut.wrappedValue.id)

                Image(systemName: "arrow.right")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                field(text: shortcut.expansion, placeholder: "Việt Nam", monospaced: false, invalid: false)
                    .frame(maxWidth: .infinity)

                Button {
                    settings.removeShortcut(shortcut.wrappedValue.id)
                } label: {
                    Image(systemName: "trash")
                }
                .buttonStyle(.borderless)
                .foregroundStyle(.secondary)
                .help("Xoá gõ tắt này")
            }

            if isDuplicate {
                Text("Trùng trigger — dòng dưới sẽ được dùng.")
                    .font(.caption2)
                    .foregroundStyle(Theme.accent)
            }
        }
        .padding(.vertical, Theme.Spacing.xs)
    }

    private func field(text: Binding<String>, placeholder: String, monospaced: Bool, invalid: Bool) -> some View {
        TextField("", text: text, prompt: Text(placeholder))
            .labelsHidden()
            .textFieldStyle(.plain)
            .font(.system(.body, design: monospaced ? .monospaced : .default))
            .padding(.horizontal, Theme.Spacing.sm)
            .padding(.vertical, 6)
            .background(.quaternary, in: RoundedRectangle(cornerRadius: Theme.Radius.control))
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.control)
                    .strokeBorder(invalid ? Theme.accent : .clear, lineWidth: 1)
            )
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
    private var duplicateTriggers: Set<String> {
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
