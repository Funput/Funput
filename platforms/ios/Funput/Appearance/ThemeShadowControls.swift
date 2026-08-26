import SwiftUI
import ThemeSchema

struct ThemeShadowControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Shadow").font(.headline)
            if draft.customTheme.theme.material == .glass {
                Toggle("Tùy chỉnh bóng trên Glass", isOn: overrideBinding)
                    .accessibilityIdentifier("themeEditor.glassShadowOverride")
            }
            ThemeMetricSlider(
                title: "Opacity bóng",
                value: opacityBinding,
                range: 0...0.50,
                step: 0.05,
                format: .percent
            )
            .accessibilityIdentifier("themeEditor.shadowOpacity")
            ThemeMetricSlider(
                title: "Độ mờ bóng",
                value: radiusBinding,
                range: 0...12,
                step: 1,
                format: .points
            )
            .accessibilityIdentifier("themeEditor.shadowRadius")
            Text("Màu đen · offset dọc 1 pt")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var overrideBinding: Binding<Bool> {
        Binding(
            get: { draft.customTheme.theme.surfaceEffects.glassShadowOverrideEnabled },
            set: { draft.customTheme.theme.surfaceEffects.glassShadowOverrideEnabled = $0 }
        )
    }

    private var opacityBinding: Binding<Double> {
        Binding(
            get: { draft.customTheme.theme.metrics.shadowOpacity },
            set: { draft.setShadowOpacity($0) }
        )
    }

    private var radiusBinding: Binding<Double> {
        Binding(
            get: { draft.customTheme.theme.metrics.shadowRadius },
            set: { draft.setShadowRadius($0) }
        )
    }
}
