import SwiftUI

/// Rendering of one gõ tắt row, split out of `ShortcutsPane` to keep both files
/// inside the 150-line budget.
extension ShortcutsPane {
    func row(_ shortcut: Binding<TextShortcut>) -> some View {
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

    func field(text: Binding<String>, placeholder: String, monospaced: Bool, invalid: Bool) -> some View {
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
}
