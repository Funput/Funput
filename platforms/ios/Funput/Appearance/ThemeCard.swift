import SwiftUI
import ThemeSchema

struct ThemeCard: View {
    let theme: KeyboardTheme
    let resolvedTheme: ResolvedTheme
    let backgroundImageData: Data?
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

    /// The tap target and the menu are overlays so they take the card's size. As ZStack
    /// siblings, the full-height tap target grew the stack to the whole scroller and the
    /// menu sat at its top corner, out of sight above the card.
    var body: some View {
        InteractiveGlassCard(isSelected: isPreviewed) {
            VStack(alignment: .leading, spacing: 10) {
                ThemeCardThumbnail(
                    themeID: theme.id,
                    theme: resolvedTheme,
                    backgroundImageData: backgroundImageData,
                    interfaceStyle: interfaceStyle
                )
                    .frame(height: 204)
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
        .overlay {
            Button(action: action) {
                Color.clear.contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(theme.metadata.name)
            .accessibilityValue(accessibilityValue)
            .accessibilityHint("Chạm hai lần để xem trước")
            .accessibilityIdentifier("appearance.theme.\(theme.id)")
        }
        .overlay(alignment: .topTrailing) {
            if isCustom { actionsMenu.padding(12) }
        }
        .frame(width: cardWidth)
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
            Button(action: editAction) {
                Label("Chỉnh sửa", systemImage: "slider.horizontal.3")
            }
            Button(role: .destructive, action: deleteAction) {
                Label("Xóa", systemImage: "trash")
            }
                .accessibilityIdentifier("themeEditor.delete")
        } label: {
            Image(systemName: "ellipsis")
                .font(.body.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(Circle().fill(.black.opacity(0.45)))
                .padding(6)
                .contentShape(Circle())
        }
        // With the default menu style iOS 27 drew no label at all, and a material or
        // glass disc draws nothing on top of the glass card, so the disc is a plain fill
        // that reads over any theme or photo.
        .menuStyle(.button)
        .buttonStyle(.plain)
        .accessibilityLabel("Tùy chọn theme \(theme.metadata.name)")
        .accessibilityIdentifier("appearance.theme.\(theme.id).menu")
    }
}
