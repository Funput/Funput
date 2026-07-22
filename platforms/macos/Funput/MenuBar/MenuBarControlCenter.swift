import AppKit
import SwiftUI

/// A compact control center instead of a traditional menu. All state is read
/// directly from `AppSettings`, keeping the IME, Settings, and menu bar in sync.
struct MenuBarControlCenter: View {
    @Environment(AppSettings.self) private var settings
    @Environment(UpdaterManager.self) private var updater
    @Environment(\.openWindow) private var openWindow

    var body: some View {
        @Bindable var settings = settings

        VStack(spacing: Theme.Spacing.md) {
            statusHeader

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

                GlassMethodSelector(selection: $settings.inputMethod, compact: true)
            }

            shortcutSummary
            actionCluster
        }
        .padding(Theme.Spacing.md)
        .frame(width: 348, height: 408)
        .background(.windowBackground)
    }

    private var statusHeader: some View {
        VietnameseFlowBackground()
            .overlay {
                HStack(spacing: Theme.Spacing.md) {
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

                    Spacer(minLength: 0)

                    Button(action: toggleVietnamese) {
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
                .padding(Theme.Spacing.lg)
            }
            .frame(height: 116)
            .clipShape(RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous))
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
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous))
    }

    private var actionCluster: some View {
        GlassEffectContainer(spacing: Theme.Spacing.sm) {
            HStack(spacing: Theme.Spacing.sm) {
                actionButton("Cài đặt", systemImage: "gearshape") {
                    open(WindowID.settings)
                }
                actionButton("Hướng dẫn", systemImage: "sparkles.rectangle.stack") {
                    open(WindowID.onboarding)
                }
                Menu {
                    Button("Kiểm tra cập nhật…", systemImage: "arrow.triangle.2.circlepath") {
                        updater.checkForUpdates()
                    }
                    .disabled(!updater.canCheckForUpdates)
                    Divider()
                    Button("Thoát Funput", systemImage: "power") {
                        NSApp.terminate(nil)
                    }
                    .keyboardShortcut("q", modifiers: .command)
                } label: {
                    Label("Thêm", systemImage: "ellipsis")
                        .frame(maxWidth: .infinity)
                }
                .menuStyle(.button)
                .buttonStyle(.glass)
            }
        }
    }

    private func actionButton(
        _ title: String,
        systemImage: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.glass)
    }

    private func toggleVietnamese() {
        settings.vietnameseEnabled.toggle()
    }

    private func open(_ id: String) {
        openWindow(id: id)
        NSApp.activate(ignoringOtherApps: true)
    }
}

#Preview {
    MenuBarControlCenter()
        .environment(AppSettings.shared)
        .environment(UpdaterManager())
}
