import SwiftUI

struct KeyboardLabControls: View {
    @Binding var previewStyle: KeyboardPreviewStyle
    @Binding var configuration: KeyboardLabConfiguration

    var body: some View {
        VStack(spacing: 18) {
            Picker("Giao diện", selection: $previewStyle) {
                ForEach(KeyboardPreviewStyle.allCases) { style in
                    Text(style.title).tag(style)
                }
            }
            .pickerStyle(.segmented)

            KeyboardLabSlider(
                title: "Chiều cao",
                value: $configuration.heightScale,
                range: 0.85...1.15,
                valueLabel: decimal(configuration.heightScale, digits: 2) + "×"
            )
            KeyboardLabSlider(
                title: "Khoảng cách phím",
                value: $configuration.keyGap,
                range: 3...10,
                valueLabel: decimal(configuration.keyGap) + " pt"
            )
            KeyboardLabSlider(
                title: "Độ bo góc",
                value: $configuration.cornerRadius,
                range: 4...16,
                valueLabel: decimal(configuration.cornerRadius) + " pt"
            )
            KeyboardLabSlider(
                title: "Độ trong suốt",
                value: $configuration.keyOpacity,
                range: 0.25...1,
                valueLabel: percent(configuration.keyOpacity)
            )

            Button("Đặt lại mặc định", systemImage: "arrow.counterclockwise") {
                withAnimation(.snappy) {
                    configuration = .default
                }
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(18)
        .background(.regularMaterial, in: .rect(cornerRadius: 18))
    }

    private func decimal(_ value: Double, digits: Int = 1) -> String {
        value.formatted(.number.precision(.fractionLength(digits)))
    }

    private func percent(_ value: Double) -> String {
        (value * 100).formatted(.number.precision(.fractionLength(0))) + "%"
    }
}
