import SwiftUI

struct OnboardingFooter: View {
    @Binding var step: Int
    let stepCount: Int
    let onComplete: () -> Void

    var body: some View {
        GlassEffectContainer(spacing: Theme.Spacing.md) {
            HStack {
                if step > 0 {
                    Button("Quay lại") { step -= 1 }
                        .buttonStyle(.glass)
                }

                Spacer()
                progress
                Spacer()

                Button(step < stepCount - 1 ? "Tiếp tục" : "Bắt đầu dùng") {
                    if step < stepCount - 1 {
                        step += 1
                    } else {
                        onComplete()
                    }
                }
                .buttonStyle(.glassProminent)
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding(Theme.Spacing.lg)
    }

    private var progress: some View {
        HStack(spacing: 6) {
            ForEach(0..<stepCount, id: \.self) { index in
                Circle()
                    .fill(index == step ? AnyShapeStyle(.tint) : AnyShapeStyle(.quaternary))
                    .frame(width: 7, height: 7)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Bước \(step + 1) trên \(stepCount)")
    }
}
