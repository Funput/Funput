import SwiftUI

struct MenuBarStatusHeader: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        VietnameseFlowBackground()
            .overlay {
                HStack(spacing: Theme.Spacing.md) {
                    status
                    Spacer(minLength: 0)
                    toggle
                }
                .padding(Theme.Spacing.lg)
            }
            .frame(height: 116)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
    }

    private var status: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            Text("FUNPUT")
                .font(.caption.weight(.bold))
                .tracking(1.4)
                .foregroundStyle(.white.opacity(0.72))
            Text(settings.vietnameseEnabled ? "Tiếng Việt đang bật" : "Đang tạm dừng")
                .font(.title2.bold())
                .foregroundStyle(.white)
            Text(settings.vietnameseEnabled ? "Sẵn sàng gõ ở mọi ứng dụng" : "Chọn VI để tiếp tục gõ")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.74))
        }
    }

    private var toggle: some View {
        Button {
            settings.vietnameseEnabled.toggle()
        } label: {
            Text(settings.vietnameseEnabled ? "VI" : "EN")
                .font(.title3.bold())
                .frame(width: 44, height: 32)
        }
        .buttonStyle(.glassProminent)
        .tint(settings.vietnameseEnabled ? Theme.accent : .gray)
        .keyboardShortcut(settings.toggleShortcut.keyboardShortcut)
        .help(settings.vietnameseEnabled ? "Tạm dừng tiếng Việt" : "Bật tiếng Việt")
        .accessibilityLabel(settings.vietnameseEnabled ? "Tắt gõ tiếng Việt" : "Bật gõ tiếng Việt")
    }
}
