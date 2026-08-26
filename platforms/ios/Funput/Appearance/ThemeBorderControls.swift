import SwiftUI
import ThemeSchema

struct ThemeBorderControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Viền").font(.headline)
            if draft.customTheme.theme.material == .glass {
                Toggle("Tùy chỉnh viền trên Glass", isOn: overrideBinding)
                    .accessibilityIdentifier("themeEditor.glassBorderOverride")
            }
            ColorPicker(
                "Màu viền",
                selection: colorBinding,
                supportsOpacity: false
            )
            .accessibilityIdentifier("themeEditor.color.border")
            ThemeMetricSlider(
                title: "Opacity viền · \(draft.previewMode.title)",
                value: opacityBinding,
                range: 0...1,
                step: 0.05,
                format: .percent
            )
            .accessibilityIdentifier("themeEditor.borderOpacity")
            ThemeMetricSlider(
                title: "Độ rộng viền",
                value: widthBinding,
                range: 0...4,
                step: 0.25,
                format: .quarterPoints
            )
            .accessibilityIdentifier("themeEditor.borderWidth")
        }
    }

    private var overrideBinding: Binding<Bool> {
        Binding(
            get: { draft.customTheme.theme.surfaceEffects.glassBorderOverrideEnabled },
            set: { draft.customTheme.theme.surfaceEffects.glassBorderOverrideEnabled = $0 }
        )
    }

    private var colorBinding: Binding<Color> {
        Binding(
            get: { draft.color(for: .border) },
            set: { draft.setColor($0, for: .border) }
        )
    }

    private var opacityBinding: Binding<Double> {
        Binding(get: { draft.borderOpacity }, set: { draft.setBorderOpacity($0) })
    }

    private var widthBinding: Binding<Double> {
        Binding(
            get: { draft.customTheme.theme.metrics.borderWidth },
            set: { draft.setBorderWidth($0) }
        )
    }
}
