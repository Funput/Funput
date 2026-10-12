import SwiftUI
import ThemeSchema

/// "Chữ trên phím": how big and how heavy the key labels are, so the text can be fitted to a
/// keyboard made taller or shorter. Only the drawn text changes; keys and touch targets stay put.
struct ThemeLabelControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Chữ trên phím").font(.headline)
            ThemeMetricSlider(
                title: "Cỡ chữ",
                value: Binding(get: { draft.labelScale }, set: { draft.setLabelScale($0) }),
                range: ThemeMetrics.fontScaleRange,
                step: 0.05,
                format: .percent
            )
            .accessibilityIdentifier("themeEditor.labelScale")
            VStack(alignment: .leading, spacing: 6) {
                Text("Độ đậm").font(.subheadline)
                Picker("Độ đậm", selection: weightBinding) {
                    ForEach(ThemeLabelWeight.allCases, id: \.self) { weight in
                        Text(title(for: weight)).tag(weight)
                    }
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("themeEditor.labelWeight")
            }
        }
    }

    private var weightBinding: Binding<ThemeLabelWeight> {
        Binding(
            get: { draft.customTheme.theme.labelWeight },
            set: { draft.setLabelWeight($0) }
        )
    }

    private func title(for weight: ThemeLabelWeight) -> String {
        switch weight {
        case .light: "Mảnh"
        case .regular: "Thường"
        case .medium: "Hơi đậm"
        case .bold: "Đậm"
        }
    }
}
