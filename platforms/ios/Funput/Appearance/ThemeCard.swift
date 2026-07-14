import KeyboardRenderer
import SwiftUI
import ThemeSchema

struct ThemeCard: View {
    let theme: KeyboardTheme
    let presentation: KeyboardPresentation
    let interfaceStyle: UIUserInterfaceStyle
    let isPreviewed: Bool
    let isApplied: Bool
    let isCustom: Bool
    let editAction: () -> Void
    let deleteAction: () -> Void
    let action: () -> Void

    /// Fixed width so the horizontal gallery snaps per card and the mini keyboard
    /// keeps roughly the real phone-portrait aspect ratio.
    private let cardWidth: CGFloat = 340

    var body: some View {
        ZStack {
            InteractiveGlassCard(isSelected: isPreviewed) {
                VStack(alignment: .leading, spacing: 10) {
                    KeyboardPreview(presentation: presentation, interfaceStyle: interfaceStyle)
                        .id(interfaceStyle)
                        .frame(height: 204)
                        .clipShape(.rect(cornerRadius: 16))
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                    HStack(spacing: 6) {
                        Text(theme.metadata.name)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.primary)
                            .lineLimit(1)
                        Spacer(minLength: 4)
                        statusIcon
                    }
                    Text(theme.metadata.author)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Button(action: action) {
                Color.clear
                    .contentShape(.rect)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(theme.metadata.name)
            .accessibilityValue(accessibilityValue)
            .accessibilityHint("Chạm hai lần để xem trước")
            .accessibilityIdentifier("appearance.theme.\(theme.id)")
        }
        .frame(width: cardWidth)
        .overlay(alignment: .topTrailing) {
            if isCustom { actionsMenu.padding(12) }
        }
    }

    @ViewBuilder private var statusIcon: some View {
        if isApplied {
            Label("Đang dùng", systemImage: "checkmark.circle.fill")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.tint)
        } else if isPreviewed {
            Label("Xem trước", systemImage: "eye.fill")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.tint)
        }
    }

    private var accessibilityValue: String {
        if isApplied { return "Đang sử dụng" }
        if isPreviewed { return "Đang xem trước" }
        return "Chưa chọn"
    }

    private var actionsMenu: some View {
        Menu {
            Button("Chỉnh sửa", systemImage: "slider.horizontal.3", action: editAction)
            Button("Xóa", systemImage: "trash", role: .destructive, action: deleteAction)
                .accessibilityIdentifier("themeEditor.delete")
        } label: {
            Image(systemName: "ellipsis.circle.fill")
                .font(.title3)
                .symbolRenderingMode(.palette)
                .foregroundStyle(.primary, .ultraThinMaterial)
                .padding(8)
                .contentShape(.circle)
        }
        .accessibilityLabel("Tùy chọn theme \(theme.metadata.name)")
        .accessibilityIdentifier("appearance.theme.\(theme.id).menu")
    }
}
