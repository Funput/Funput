import SwiftUI

struct AppearanceResetCard: View {
    let isDefault: Bool
    let action: () -> Void

    var body: some View {
        AdaptiveGlassCard {
            Label("Funput Glass", systemImage: "sparkles")
                .font(.headline)
            Text("Khôi phục theme mặc định mà không thay đổi thiết lập bộ gõ của bạn.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            resetButton
        }
    }

    @ViewBuilder private var resetButton: some View {
        if #available(iOS 26, *) {
            button.buttonStyle(.glass)
        } else {
            button.buttonStyle(.bordered)
        }
    }

    private var button: some View {
        Button("Khôi phục Funput Glass", role: .destructive, action: action)
            .disabled(isDefault)
            .accessibilityIdentifier("appearance.reset")
    }
}
