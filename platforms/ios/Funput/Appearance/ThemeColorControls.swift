import SwiftUI

struct ThemeBackgroundControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ThemeColorCard(
            title: "Nền bàn phím",
            options: [
                ThemeColorOption("Màu bắt đầu", .backgroundStart),
                ThemeColorOption("Màu kết thúc", .backgroundEnd),
            ],
            draft: $draft
        )
    }
}

struct ThemeKeyTextControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ThemeColorCard(
            title: "Màu phím",
            options: [
                ThemeColorOption("Phím thường", .characterKey),
                ThemeColorOption("Phím chức năng", .specialKey),
            ],
            draft: $draft
        )
        ThemeColorCard(
            title: "Chữ và icon",
            options: [
                ThemeColorOption("Nội dung chính", .label),
                ThemeColorOption("Nội dung phụ", .secondaryLabel),
                ThemeColorOption("Accent / Enter", .accent),
            ],
            draft: $draft
        )
    }
}

private struct ThemeColorOption: Identifiable {
    let title: String
    let role: ThemeColorRole
    var id: String { role.rawValue }

    init(_ title: String, _ role: ThemeColorRole) {
        self.title = title
        self.role = role
    }
}

private struct ThemeColorCard: View {
    let title: String
    let options: [ThemeColorOption]
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        AdaptiveGlassCard {
            Text(title).font(.headline)
            ForEach(options) { option in
                ColorPicker(
                    option.title,
                    selection: colorBinding(option.role),
                    supportsOpacity: false
                )
                .accessibilityIdentifier("themeEditor.color.\(option.role.rawValue)")
            }
        }
    }

    private func colorBinding(_ role: ThemeColorRole) -> Binding<Color> {
        Binding(
            get: { draft.color(for: role) },
            set: { draft.setColor($0, for: role) }
        )
    }
}
