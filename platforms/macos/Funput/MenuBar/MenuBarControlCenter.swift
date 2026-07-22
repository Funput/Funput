import SwiftUI

struct MenuBarControlCenter: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        @Bindable var settings = settings
        VStack(spacing: Theme.Spacing.md) {
            MenuBarStatusHeader()
            methodSelector(selection: $settings.inputMethod)
            shortcutSummary(isVietnameseEnabled: $settings.vietnameseEnabled)
            MenuBarActionCluster()
        }
        .padding(Theme.Spacing.md)
        .frame(minWidth: 348, idealWidth: 348)
        .fixedSize(horizontal: true, vertical: false)
        .background(.windowBackground)
    }

    // Trailing control reuses the same Settings-sidebar `GlassLanguageToggle`
    // (instead of a passive `ShortcutCaps` display) so the panel offers a real
    // EN/VI switch here too. The shortcut itself moves into the subtitle text.
    private func shortcutSummary(isVietnameseEnabled: Binding<Bool>) -> some View {
        HStack(spacing: Theme.Spacing.md) {
            Image(systemName: "command")
                .font(.title3)
                .foregroundStyle(Theme.accent)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text("Chuyển tiếng Việt / tiếng Anh")
                    .font(.callout.weight(.semibold))
                Text("Có thể dùng phím tắt \(settings.toggleShortcut.keyCaps.joined(separator: " + "))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()
            GlassLanguageToggle(isVietnameseEnabled: isVietnameseEnabled)
        }
        .padding(Theme.Spacing.md)
        .background(
            .regularMaterial,
            in: RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous)
        )
    }

    private func methodSelector(selection: Binding<InputMethod>) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            Text("Phương thức gõ")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
            GlassMethodSelector(selection: selection, compact: true)
        }
        // The blurb HStack used to have a trailing Spacer that stretched this
        // section to the panel's full width; now that it's gone, the VStack
        // shrinks to its content and the parent's default `.center` alignment
        // centers it. Claim full width explicitly and keep leading alignment.
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#Preview {
    MenuBarControlCenter()
        .environment(AppSettings.shared)
        .environment(UpdaterManager())
}
