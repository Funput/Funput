import SwiftUI

struct AboutPane: View {
    // Optional so SwiftUI previews (which don't inject the manager) still render.
    @Environment(UpdaterManager.self) private var updater: UpdaterManager?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: Theme.Spacing.lg) {
            HStack {
                Text("Giới thiệu Funput")
                    .font(.headline)

                Spacer()

                Button(action: close) {
                    Image(systemName: "xmark")
                        .frame(width: 18, height: 18)
                }
                .buttonStyle(.glass)
                .keyboardShortcut("w", modifiers: .command)
                .help("Đóng")
                .accessibilityLabel("Đóng giới thiệu Funput")
            }

            SettingsSurface {
                VStack(spacing: Theme.Spacing.md) {
                    AppLogo(size: 92)
                        .padding(.bottom, Theme.Spacing.xs)

                    VStack(spacing: Theme.Spacing.xs) {
                        Text("Funput")
                            .font(.largeTitle.bold())
                        Text("Bộ gõ tiếng Việt — miễn phí, mã nguồn mở.")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }

                    Text("Phiên bản \(appVersion)")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, Theme.Spacing.md)
                        .padding(.vertical, Theme.Spacing.xs)
                        .background(.quaternary, in: .capsule)

                    GlassEffectContainer(spacing: Theme.Spacing.sm) {
                        VStack(spacing: Theme.Spacing.sm) {
                            if let updater {
                                Button { updater.checkForUpdates() } label: {
                                    Label("Kiểm tra cập nhật", systemImage: "arrow.triangle.2.circlepath")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.glassProminent)
                                .controlSize(.large)
                                .disabled(!updater.canCheckForUpdates)
                            }

                            HStack(spacing: Theme.Spacing.sm) {
                                linkButton(
                                    "GitHub",
                                    systemImage: "chevron.left.forwardslash.chevron.right",
                                    url: "https://github.com/Funput/Funput"
                                )
                                linkButton("Website", systemImage: "globe", url: "https://funput.app/")
                            }
                        }
                        .padding(.top, Theme.Spacing.sm)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, Theme.Spacing.md)
            }
        }
        .onExitCommand(perform: close)
    }

    /// A secondary glass link button with a leading icon, sized to share its row.
    private func linkButton(_ title: String, systemImage: String, url: String) -> some View {
        Link(destination: URL(string: url)!) {
            Label(title, systemImage: systemImage)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.glass)
        .controlSize(.large)
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private func close() {
        dismiss()
    }
}

#Preview {
    AboutPane()
        .environment(AppSettings.shared)
        .padding(Theme.Spacing.xl)
        .frame(width: 520)
}
