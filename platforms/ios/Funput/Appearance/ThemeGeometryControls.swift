import SwiftUI
import ThemeSchema

struct ThemeEditorNameCard: View {
    @Binding var name: String

    var body: some View {
        AdaptiveGlassCard {
            Text("Tên theme").font(.headline)
            TextField("Tên theme", text: $name)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("themeEditor.name")
                .onChange(of: name) { _, value in
                    if value.count > 40 { name = String(value.prefix(40)) }
                }
        }
    }
}

struct ThemeGeometryControls: View {
    @Binding var draft: ThemeEditorDraft

    var body: some View {
        AdaptiveGlassCard {
            Text("Hình học phím").font(.headline)
            metric("Chiều cao keycap", value: $draft.customTheme.theme.geometry.keycapHeightScale,
                   range: 0.82...1, step: 0.02, format: .percent)
            metric("Lề ngang", value: $draft.customTheme.theme.geometry.horizontalPadding,
                   range: 2...16, step: 1)
            metric("Khoảng cách ngang", value: $draft.customTheme.theme.geometry.horizontalGap,
                   range: 2...10, step: 1)
            metric("Khoảng cách dọc", value: $draft.customTheme.theme.geometry.verticalGap,
                   range: 3...12, step: 1)
            metric("Corner radius", value: $draft.customTheme.theme.metrics.cornerRadius,
                   range: 0...20, step: 1)
        }
    }

    private func metric(
        _ title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double,
        format: ThemeMetricFormat = .points
    ) -> some View {
        ThemeMetricSlider(
            title: title,
            value: value,
            range: range,
            step: step,
            format: format
        )
    }
}

enum ThemeMetricFormat: Equatable { case points, percent }

struct ThemeMetricSlider: View {
    let title: String
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let format: ThemeMetricFormat

    var body: some View {
        VStack(spacing: 4) {
            HStack {
                Text(title).font(.subheadline)
                Spacer()
                Text(valueLabel).font(.caption.monospacedDigit()).foregroundStyle(.secondary)
            }
            Slider(value: $value, in: range, step: step)
                .accessibilityLabel(title)
                .accessibilityValue(valueLabel)
                .accessibilityIdentifier("themeEditor.\(identifier)")
        }
    }

    private var valueLabel: String {
        format == .percent ? "\(Int((value * 100).rounded()))%" : "\(Int(value.rounded())) pt"
    }

    private var identifier: String {
        title.folding(options: .diacriticInsensitive, locale: .current)
            .lowercased().replacingOccurrences(of: " ", with: "-")
    }
}
