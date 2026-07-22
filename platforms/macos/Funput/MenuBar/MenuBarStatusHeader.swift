import SwiftUI

struct MenuBarStatusHeader: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        VietnameseFlowBackground()
            .overlay {
                // The EN/VI toggle used to live here, but `shortcutSummary` in
                // `MenuBarControlCenter` now has its own `GlassLanguageToggle`,
                // so this header stays a pure status readout.
                status
                    .frame(maxWidth: .infinity, alignment: .leading)
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
}
