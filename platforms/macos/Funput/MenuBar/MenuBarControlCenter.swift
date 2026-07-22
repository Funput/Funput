import SwiftUI

struct MenuBarControlCenter: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings
        VStack(spacing: Theme.Spacing.md) {
            MenuBarStatusHeader()
            methodSelector(selection: $settings.inputMethod)
            shortcutSummary
            MenuBarActionCluster()
        }
        .padding(Theme.Spacing.md)
        .frame(minWidth: 348, idealWidth: 348)
        .fixedSize(horizontal: true, vertical: false)
        .background(.windowBackground)
    }

    private var shortcutSummary: some View {
        HStack(spacing: Theme.Spacing.md) {
            Image(systemName: "command")
                .font(.title3)
                .foregroundStyle(Theme.accent)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text("Chuyển Việt / Anh")
                    .font(.callout.weight(.semibold))
                Text("Dùng được cả khi cửa sổ Funput đóng")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()
            ShortcutCaps(caps: settings.toggleShortcut.keyCaps)
        }
        .padding(Theme.Spacing.md)
        .background(
            .regularMaterial,
            in: RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous)
        )
    }

    private func methodSelector(selection: Binding<InputMethod>) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            HStack {
                Text("Phương thức gõ")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer()
                Text(settings.inputMethod.blurb)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            GlassMethodSelector(selection: selection, compact: true)
        }
    }
}

#Preview {
    MenuBarControlCenter()
        .environment(AppSettings.shared)
        .environment(UpdaterManager())
}
