import SwiftUI
import ThemeSchema

struct ThemeKeyOpacityControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Độ trong phím").font(.headline)
            ThemeMetricSlider(
                title: "Phím thường",
                value: opacityBinding(special: false),
                range: 0.30...1,
                step: 0.05,
                format: .percent
            )
            ThemeMetricSlider(
                title: "Phím chức năng",
                value: opacityBinding(special: true),
                range: 0.30...1,
                step: 0.05,
                format: .percent
            )
            if draft.customTheme.theme.material == .glass {
                Text("Với Liquid Glass, giá trị này điều khiển cường độ lớp tint độc lập.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func opacityBinding(special: Bool) -> Binding<Double> {
        Binding(
            get: {
                special
                    ? draft.customTheme.theme.metrics.specialKeyOpacity
                    : draft.customTheme.theme.metrics.keyOpacity
            },
            set: { draft.setKeyOpacity($0, special: special) }
        )
    }
}
