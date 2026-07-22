import SwiftUI

/// Consistent step layout for onboarding content.
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
