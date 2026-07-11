import KeyboardLayout
import KeyboardRenderer
import SwiftUI
import ThemeSchema

struct KeyboardLabView: View {
    @State private var previewStyle = PreviewStyle.system
    @State private var heightScale = 1.0
    @State private var keyGap = 5.0
    @State private var cornerRadius = 10.0
    @State private var keyOpacity = 0.72

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                intro
                preview
                controls
            }
            .padding()
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }

    private var intro: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Funput Glass")
                .font(.title2.bold())
            Text("Tinh chỉnh hình học và độ trong suốt bằng chính renderer của Keyboard extension.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var preview: some View {
        KeyboardPreview(
            presentation: presentation,
            interfaceStyle: previewStyle.interfaceStyle
        )
        .frame(height: 280 * heightScale)
        .clipShape(.rect(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18)
                .stroke(.white.opacity(0.15), lineWidth: 0.5)
        }
        .shadow(color: .black.opacity(0.12), radius: 14, y: 7)
    }

    private var controls: some View {
        VStack(spacing: 18) {
            Picker("Giao diện", selection: $previewStyle) {
                ForEach(PreviewStyle.allCases) { style in
                    Text(style.title).tag(style)
                }
            }
            .pickerStyle(.segmented)

            slider(
                title: "Chiều cao",
                value: $heightScale,
                range: 0.85...1.15,
                valueLabel: heightScale.formatted(.number.precision(.fractionLength(2))) + "×"
            )
            slider(
                title: "Khoảng cách phím",
                value: $keyGap,
                range: 3...10,
                valueLabel: keyGap.formatted(.number.precision(.fractionLength(1))) + " pt"
            )
            slider(
                title: "Độ bo góc",
                value: $cornerRadius,
                range: 4...16,
                valueLabel: cornerRadius.formatted(.number.precision(.fractionLength(1))) + " pt"
            )
            slider(
                title: "Độ trong suốt",
                value: $keyOpacity,
                range: 0.25...1,
                valueLabel: (keyOpacity * 100).formatted(.number.precision(.fractionLength(0))) + "%"
            )

            Button("Đặt lại mặc định", systemImage: "arrow.counterclockwise") {
                withAnimation(.snappy) {
                    heightScale = 1
                    keyGap = 5
                    cornerRadius = 10
                    keyOpacity = 0.72
                }
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(18)
        .background(.regularMaterial, in: .rect(cornerRadius: 18))
    }

    private var presentation: KeyboardPresentation {
        var sizing = KeyboardSizingProfile.default
        sizing.heightScale = heightScale
        sizing.horizontalGap = keyGap
        sizing.verticalGap = keyGap + 2

        var theme = KeyboardThemeTokens.funputGlass
        theme.cornerRadius = cornerRadius
        theme.keyOpacity = keyOpacity
        theme.specialKeyOpacity = max(0.25, keyOpacity - 0.14)

        return KeyboardPresentation(
            layout: .funputQWERTY,
            sizing: sizing,
            theme: theme,
            shiftState: .lowercase,
            showsInputModeKey: true
        )
    }

    private func slider(
        title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        valueLabel: String
    ) -> some View {
        VStack(spacing: 6) {
            HStack {
                Text(title)
                Spacer()
                Text(valueLabel)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            Slider(value: value, in: range)
        }
    }
}

private enum PreviewStyle: String, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: Self { self }

    var title: String {
        switch self {
        case .system: "Hệ thống"
        case .light: "Sáng"
        case .dark: "Tối"
        }
    }

    var interfaceStyle: UIUserInterfaceStyle {
        switch self {
        case .system: .unspecified
        case .light: .light
        case .dark: .dark
        }
    }
}
