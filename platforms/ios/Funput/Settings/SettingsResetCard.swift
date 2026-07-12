import SwiftUI

struct SettingsResetCard: View {
    let action: () -> Void

    var body: some View {
        AdaptiveGlassCard {
            VStack(alignment: .leading, spacing: 10) {
                Label("Bắt đầu lại", systemImage: "arrow.counterclockwise")
                    .font(.headline)
                Text("Khôi phục toàn bộ thiết lập bộ gõ về giá trị mặc định của Funput.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                resetButton
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder private var resetButton: some View {
        if #available(iOS 26, *) {
            Button("Khôi phục cài đặt", role: .destructive, action: action)
                .buttonStyle(.glass)
        } else {
            Button("Khôi phục cài đặt", role: .destructive, action: action)
                .buttonStyle(.bordered)
        }
    }
}
