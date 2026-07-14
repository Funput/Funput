import SwiftUI
import ThemeSchema

struct ThemePressedControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        AdaptiveGlassCard {
            Text("Trạng thái nhấn").font(.headline)
            Toggle("Màu nhấn tùy chỉnh", isOn: overlayBinding)
                .accessibilityIdentifier("themeEditor.pressedOverlayEnabled")
            ColorPicker(
                "Màu overlay",
                selection: colorBinding,
                supportsOpacity: false
            )
            .disabled(!draft.customTheme.theme.colorEffects.pressedOverlayEnabled)
            .accessibilityIdentifier("themeEditor.color.pressedOverlay")
            ThemeMetricSlider(
                title: "Độ co khi nhấn",
                value: $draft.customTheme.theme.metrics.pressedScale,
                range: 0.90...1,
                step: 0.01,
                format: .percent
            )
            .accessibilityIdentifier("themeEditor.pressedScale")
        }
    }

    private var overlayBinding: Binding<Bool> {
        Binding(
            get: { draft.customTheme.theme.colorEffects.pressedOverlayEnabled },
            set: { draft.customTheme.theme.colorEffects.pressedOverlayEnabled = $0 }
        )
    }

    private var colorBinding: Binding<Color> {
        Binding(
            get: { draft.color(for: .pressedOverlay) },
            set: { draft.setColor($0, for: .pressedOverlay) }
        )
    }
}
