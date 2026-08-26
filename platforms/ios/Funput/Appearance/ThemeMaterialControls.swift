import SwiftUI
import ThemeSchema

struct ThemeMaterialControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Material").font(.headline)
            Picker("Material", selection: $draft.customTheme.theme.material) {
                ForEach(materials, id: \.self) { material in
                    Text(title(for: material)).tag(material)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("themeEditor.material")
            Text("Material được dùng chung cho chế độ sáng và tối.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private let materials: [KeyboardMaterial] = [.solid, .translucent, .glass]

    private func title(for material: KeyboardMaterial) -> String {
        switch material {
        case .solid: "Solid"
        case .translucent: "Trong mờ"
        case .glass: "Liquid Glass"
        }
    }
}
