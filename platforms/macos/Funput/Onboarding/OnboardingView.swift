import SwiftUI

/// First-run welcome flow: introduce Funput, help enable it, pick a method.
struct OnboardingView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var step = 0

    private let stepCount = 4

    var body: some View {
        ZStack {
            Rectangle().fill(.windowBackground)
            VietnameseFlowBackground()
                .opacity(step == 0 ? 1 : 0)

            VStack(spacing: 0) {
                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding(Theme.Spacing.xl)
                footer
            }
        }
        .frame(width: 580, height: 500)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.28), value: step == 0)
    }

    @ViewBuilder private var content: some View {
        switch step {
        case 0:
            welcomeStep
        case 1:
            EnableInputSourceStep()
        case 2:
            methodStep
        default:
            OnboardingStep(icon: "checkmark.seal.fill", title: "Sẵn sàng!",
                           subtitle: "Chọn Funput từ menu bàn phím trên thanh menu và bắt đầu gõ.") {
                readySummary
            }
        }
    }

    /// First step — leads with the Funput logo to anchor the brand.
    private var welcomeStep: some View {
        VStack(spacing: Theme.Spacing.lg) {
            Spacer(minLength: 0)
            AppLogo(size: 104)
            VStack(spacing: Theme.Spacing.sm) {
                Text("Chào mừng đến Funput")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                Text("Gõ tiếng Việt ở mọi nơi trên máy Mac — miễn phí, mã nguồn mở.")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.78))
                    .multilineTextAlignment(.center)
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: 440)
        .frame(maxWidth: .infinity)
    }

    private var methodStep: some View {
        @Bindable var settings = settings
        return OnboardingStep(icon: "keyboard", title: "Chọn phương thức gõ",
                              subtitle: "Có thể đổi bất cứ lúc nào trong Cài đặt.") {
            VStack(spacing: Theme.Spacing.md) {
                GlassMethodSelector(selection: $settings.inputMethod)
                Text(settings.inputMethod.blurb)
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: 420)
        }
    }

    private var readySummary: some View {
        HStack(spacing: Theme.Spacing.lg) {
            Label(settings.inputMethod.displayName, systemImage: "keyboard")
            Divider().frame(height: 22)
            Label("VI / EN", systemImage: "globe")
            ShortcutCaps(caps: settings.toggleShortcut.keyCaps)
        }
        .font(.callout.weight(.semibold))
        .padding(.horizontal, Theme.Spacing.lg)
        .padding(.vertical, Theme.Spacing.md)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous))
    }

    private var footer: some View {
        GlassEffectContainer(spacing: Theme.Spacing.md) {
            HStack {
                if step > 0 {
                    Button("Quay lại") { step -= 1 }
                        .buttonStyle(.glass)
                }
                Spacer()
                HStack(spacing: 6) {
                    ForEach(0..<stepCount, id: \.self) { i in
                        Circle()
                            .fill(i == step ? AnyShapeStyle(.tint) : AnyShapeStyle(.quaternary))
                            .frame(width: 7, height: 7)
                    }
                }
                Spacer()
                Button(step < stepCount - 1 ? "Tiếp tục" : "Bắt đầu dùng") {
                    if step < stepCount - 1 {
                        step += 1
                    } else {
                        settings.hasCompletedOnboarding = true
                        dismiss()
                    }
                }
                .buttonStyle(.glassProminent)
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding(Theme.Spacing.lg)
    }
}

/// Consistent step layout: hero icon + title + subtitle + custom content.
struct OnboardingStep<Content: View>: View {
    let icon: String
    let title: String
    let subtitle: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: Theme.Spacing.lg) {
            Spacer(minLength: 0)
            Image(systemName: icon)
                .font(.system(size: 72))
                .foregroundStyle(.tint)
                .symbolRenderingMode(.hierarchical)
            VStack(spacing: Theme.Spacing.sm) {
                Text(title).font(.largeTitle.bold())
                Text(subtitle)
                    .font(.title3)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            content
            Spacer(minLength: 0)
        }
        .frame(maxWidth: 440)
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    OnboardingView()
        .environment(AppSettings.shared)
}
