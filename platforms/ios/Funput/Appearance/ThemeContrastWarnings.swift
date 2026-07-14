import SwiftUI
import ThemeRuntime
import ThemeSchema

struct ThemeContrastWarnings: View {
    let draft: ThemeEditorDraft

    var body: some View {
        if !warnings.isEmpty {
            AdaptiveGlassCard {
                Label("Tương phản màu thấp", systemImage: "exclamationmark.triangle.fill")
                    .font(.headline)
                    .foregroundStyle(.orange)
                ForEach(warnings, id: \.message) { issue in
                    Text(localized(issue.message))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Text("Bạn vẫn có thể lưu theme này.")
                    .font(.caption.weight(.semibold))
            }
            .accessibilityIdentifier("themeEditor.contrastWarning")
        }
    }

    private var warnings: [ThemeIssue] {
        guard draft.customTheme.theme.palette != draft.baseTheme.palette else { return [] }
        let mode = draft.previewMode == .light ? "light" : "dark"
        let baseMessages = Set(ThemeValidator.validate(draft.baseTheme).map(\.message))
        return ThemeValidator.validate(draft.customTheme.theme).filter {
            $0.kind == .lowContrast && $0.message.contains(mode)
                && (!baseMessages.contains($0.message) || draft.hasChanged(roles(for: $0.message)))
        }
    }

    private func localized(_ message: String) -> String {
        let key = message.components(separatedBy: " contrast").first ?? ""
        let role = roleTitle(key)
        return "Cặp màu \(role) khó đọc ở chế độ \(draft.previewMode.title.lowercased())."
    }

    private func roles(for message: String) -> [ThemeColorRole] {
        if message.hasPrefix("label/character-key") { return [.label, .characterKey] }
        if message.hasPrefix("label/special-key") { return [.label, .specialKey] }
        if message.hasPrefix("secondary-label") { return [.secondaryLabel, .characterKey] }
        return [.accent, .specialKey]
    }

    private func roleTitle(_ key: String) -> String {
        switch key {
        case "label/character-key": "chữ chính / phím thường"
        case "label/special-key": "chữ chính / phím chức năng"
        case "secondary-label/character-key": "chữ phụ / phím thường"
        case "accent/special-key": "accent / phím Enter"
        default: "chữ và phím"
        }
    }
}
