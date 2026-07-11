import KeyboardRenderer
import SwiftUI

struct KeyboardLabView: View {
    @State private var previewStyle = KeyboardPreviewStyle.system
    @State private var configuration = KeyboardLabConfiguration.default

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                introduction
                keyboardPreview
                KeyboardLabControls(
                    previewStyle: $previewStyle,
                    configuration: $configuration
                )
            }
            .padding()
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .preferredColorScheme(previewStyle.colorScheme)
    }

    private var introduction: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Funput Glass")
                .font(.title2.bold())
            Text("Tinh chỉnh hình học và độ trong suốt bằng chính renderer của Keyboard extension.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var keyboardPreview: some View {
        KeyboardPreview(
            presentation: configuration.presentation,
            interfaceStyle: previewStyle.interfaceStyle
        )
        .frame(
            height: KeyboardMetrics.phonePortraitBaseHeight * CGFloat(configuration.heightScale)
        )
        .clipShape(.rect(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18)
                .stroke(.white.opacity(0.15), lineWidth: 0.5)
        }
        .shadow(color: .black.opacity(0.12), radius: 14, y: 7)
    }
}
