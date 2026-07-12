import FunputShared
import KeyboardRenderer
import SwiftUI

struct AppearanceScreen: View {
    private let presentation = KeyboardPreviewPresentation.make(configuration: .default)

    var body: some View {
        AppScreen {
            VStack(alignment: .leading, spacing: 8) {
                Text("Xem trước")
                    .font(.headline)
                Text("Bản xem trước dùng chính renderer của bàn phím Funput.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            KeyboardPreview(presentation: presentation)
                .frame(height: previewHeight)
                .clipShape(.rect(cornerRadius: 22))
                .shadow(color: .black.opacity(0.12), radius: 14, y: 7)

            AdaptiveGlassCard {
                VStack(alignment: .leading, spacing: 12) {
                    Label("Bộ sưu tập giao diện", systemImage: "paintpalette.fill")
                        .font(.headline)
                    Text("Chọn theme, màu nền và phong cách phím sẽ được bổ sung tại đây.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .navigationTitle("Giao diện")
    }

    private var previewHeight: CGFloat {
        KeyboardMetrics.phonePortraitHeight(for: presentation.layout)
    }
}

#Preview("Giao diện · Light") {
    NavigationStack { AppearanceScreen() }
        .preferredColorScheme(.light)
}

#Preview("Giao diện · Dark") {
    NavigationStack { AppearanceScreen() }
        .preferredColorScheme(.dark)
}
