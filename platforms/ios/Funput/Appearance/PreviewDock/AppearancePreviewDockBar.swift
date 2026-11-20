import SwiftUI

/// The strip above the docked preview: interface style, the previewed theme's
/// actions, and the button that puts the preview away.
struct AppearancePreviewDockBar: View {
    @Binding var mode: AppearancePreviewMode
    let isApplied: Bool
    let isCustom: Bool
    let apply: () -> Void
    let customize: () -> Void
    let close: () -> Void

    var body: some View {
        // The compact row drops the text labels, for narrow phones and large type.
        ViewThatFits(in: .horizontal) {
            row(isCompact: false)
            row(isCompact: true)
        }
        // At least this tall, so the keyboard does not shift when "Áp dụng"
        // replaces the status label.
        .frame(minHeight: 44)
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
    }

    @ViewBuilder
    private func row(isCompact: Bool) -> some View {
        let row = HStack(spacing: 10) {
            modePicker
            Spacer(minLength: 4)
            if isApplied {
                Label("Đang dùng", systemImage: "checkmark.circle.fill")
                    .dockLabelStyle(isCompact: isCompact)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .accessibilityIdentifier("appearance.appliedStatus")
            } else {
                styled(
                    Button(action: apply) {
                        Label("Áp dụng", systemImage: "paintbrush.fill")
                            .dockLabelStyle(isCompact: isCompact)
                    }
                    .accessibilityIdentifier("appearance.apply"),
                    isProminent: true
                )
            }
            styled(
                Button(action: customize) {
                    Image(systemName: isCustom ? "pencil" : "slider.horizontal.3")
                }
                .accessibilityLabel(isCustom ? "Chỉnh sửa" : "Tùy chỉnh")
                .accessibilityIdentifier("appearance.customize")
            )
            styled(
                Button(action: close) { Image(systemName: "keyboard.chevron.compact.down") }
                    .accessibilityLabel("Ẩn bàn phím xem trước")
                    .accessibilityIdentifier("appearance.hidePreview")
            )
        }
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: 10) { row }
        } else {
            row
        }
    }

    private var modePicker: some View {
        Picker("Chế độ xem trước", selection: $mode) {
            ForEach(AppearancePreviewMode.allCases) { mode in
                Text(mode.title).tag(mode)
            }
        }
        .pickerStyle(.segmented)
        .frame(maxWidth: 128)
        .accessibilityIdentifier("appearance.previewMode")
    }

    @ViewBuilder
    private func styled(_ button: some View, isProminent: Bool = false) -> some View {
        if #available(iOS 26, *) {
            if isProminent {
                button.buttonStyle(.glassProminent)
            } else {
                button.buttonStyle(.glass)
            }
        } else if isProminent {
            button.buttonStyle(.borderedProminent)
        } else {
            button.buttonStyle(.bordered)
        }
    }
}

private extension View {
    /// Keeps the title for VoiceOver either way, which hiding the text would not.
    @ViewBuilder
    func dockLabelStyle(isCompact: Bool) -> some View {
        if isCompact {
            labelStyle(.iconOnly)
        } else {
            labelStyle(.titleAndIcon)
        }
    }
}
