import SwiftUI

struct AboutPane: View {
    // Optional so SwiftUI previews (which don't inject the manager) still render.
    @Environment(UpdaterManager.self) private var updater: UpdaterManager?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            hero
            actions
        }
        .background(.windowBackground)
        .onExitCommand(perform: close)
    }

    /// Full-bleed brand backdrop (same `VietnameseFlowBackground` + `AppLogo`
    /// pairing as the Onboarding welcome step) instead of a plain card. The
    /// close button floats on top as Liquid Glass — a floating control over
    /// content-layer artwork is exactly the case the material is designed for,
    /// unlike the flat header row it sat in before.
    private var hero: some View {
        VietnameseFlowBackground()
            .overlay {
                VStack(spacing: Theme.Spacing.sm) {
                    AppLogo(size: 84)
                    Text("Funput")
                        .font(.largeTitle.bold())
                        .foregroundStyle(.white)
                    Text("Bộ gõ tiếng Việt — miễn phí, mã nguồn mở.")
                        .font(.callout)
                        .foregroundStyle(.white.opacity(0.78))
                        .multilineTextAlignment(.center)
                    Text("Phiên bản \(appVersion)")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(0.85))
                        .padding(.horizontal, Theme.Spacing.md)
                        .padding(.vertical, Theme.Spacing.xs)
                        .background(.black.opacity(0.22), in: .capsule)
                }
                .padding(Theme.Spacing.xl)
            }
            .overlay(alignment: .topTrailing) {
                closeButton
                    .padding(Theme.Spacing.md)
            }
            .frame(height: 260)
    }

    private var closeButton: some View {
        Button(action: close) {
            Image(systemName: "xmark")
                .frame(width: 18, height: 18)
        }
        .buttonStyle(.glass)
        .keyboardShortcut("w", modifiers: .command)
        .help("Đóng")
        .accessibilityLabel("Đóng giới thiệu Funput")
    }

    private var actions: some View {
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
        }
        .padding(Theme.Spacing.lg)
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
        .frame(width: 460)
}
