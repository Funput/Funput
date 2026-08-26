import SwiftUI
import ThemeSchema

struct ThemeBackgroundModeControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Kiểu nền").font(.headline)
            Picker("Kiểu nền", selection: mode) {
                Text("Gradient").tag(ThemeBackgroundMode.gradient)
                Text("Ảnh").tag(ThemeBackgroundMode.image)
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("themeEditor.backgroundMode")
        }
    }

    private var mode: Binding<ThemeBackgroundMode> {
        Binding(
            get: { draft.customTheme.theme.backgroundEffects.mode },
            set: { draft.setBackgroundMode($0) }
        )
    }
}
