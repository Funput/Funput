import SwiftUI
import ThemeSchema

struct ThemeBackgroundStyleControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        ContentCard {
            Text("Gradient").font(.headline)
            Picker("Hướng gradient", selection: directionBinding) {
                ForEach(ThemeGradientDirection.allCases, id: \.self) { direction in
                    Text(title(for: direction)).tag(direction)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("themeEditor.gradientDirection")
            VStack(spacing: 10) {
                opacitySlider("Opacity màu đầu", stop: .start)
                opacitySlider("Opacity màu cuối", stop: .end)
            }
            .disabled(draft.customTheme.theme.material == .solid)
            if draft.customTheme.theme.material == .solid {
                Text("Solid luôn hiển thị nền với opacity 100%.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var directionBinding: Binding<ThemeGradientDirection> {
        Binding(
            get: { draft.customTheme.theme.gradientDirection },
            set: { draft.setGradientDirection($0) }
        )
    }

    private func opacitySlider(
        _ title: String,
        stop: ThemeBackgroundStop
    ) -> some View {
        ThemeMetricSlider(
            title: "\(title) · \(draft.previewMode.title)",
            value: Binding(
                get: { draft.backgroundOpacity(for: stop) },
                set: { draft.setBackgroundOpacity($0, for: stop) }
            ),
            range: 0...1,
            step: 0.05,
            format: .percent
        )
        .accessibilityIdentifier(
            stop == .start ? "themeEditor.backgroundStartOpacity" : "themeEditor.backgroundEndOpacity"
        )
    }

    private func title(for direction: ThemeGradientDirection) -> String {
        switch direction {
        case .horizontal: "Ngang"
        case .vertical: "Dọc"
        case .diagonalRight: "Chéo ↘"
        case .diagonalLeft: "Chéo ↙"
        }
    }
}
