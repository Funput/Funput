import Foundation
import SwiftUI

/// Brand artwork in the content layer. System glass samples these colors from
/// the sidebar and controls without turning the artwork itself into glass.
struct VietnameseFlowBackground: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.controlActiveState) private var controlActiveState

    var animated = true

    var body: some View {
        TimelineView(.animation(
            minimumInterval: 1.0 / 20.0,
            paused: !animated || reduceMotion || controlActiveState == .inactive
        )) { context in
            artwork(at: context.date.timeIntervalSinceReferenceDate)
        }
        .accessibilityHidden(true)
    }

    private func artwork(at time: TimeInterval) -> some View {
        GeometryReader { proxy in
            let drift = animated && !reduceMotion ? sin(time * 0.34) : 0
            ZStack {
                LinearGradient(
                    colors: [
                        Color(red: 0.99, green: 0.34, blue: 0.06),
                        Color(red: 0.78, green: 0.13, blue: 0.31),
                        Color(red: 0.25, green: 0.14, blue: 0.50),
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                Circle()
                    .fill(Color.orange.opacity(0.68))
                    .frame(width: proxy.size.width * 0.55)
                    .blur(radius: 58)
                    .offset(
                        x: -proxy.size.width * 0.28 + drift * 18,
                        y: -proxy.size.height * 0.32
                    )

                Circle()
                    .fill(Color.cyan.opacity(0.34))
                    .frame(width: proxy.size.width * 0.48)
                    .blur(radius: 64)
                    .offset(
                        x: proxy.size.width * 0.38 - drift * 22,
                        y: proxy.size.height * 0.38
                    )

                Text("Ă  Â  Ê  Ô  Ơ  Ư  Đ")
                    .font(.system(size: 76, weight: .black, design: .rounded))
                    .foregroundStyle(.white.opacity(0.075))
                    .lineLimit(1)
                    .rotationEffect(.degrees(-7 + drift))
                    .offset(x: proxy.size.width * 0.08, y: proxy.size.height * 0.14)

                LinearGradient(
                    colors: [.black.opacity(0.02), .black.opacity(0.26)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .clipped()
    }
}

struct OverviewHero: View {
    @Environment(AppSettings.self) private var settings

    private var rawExample: String {
        settings.inputMethod == .vni ? "To6i ye6u tie61ng Vie65t" : "Tooi yeeu tieengs Vieetj"
    }

    var body: some View {
        VietnameseFlowBackground()
            .backgroundExtensionEffect()
            .overlay {
                HStack(spacing: Theme.Spacing.xl) {
                    VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                        Text("FUNPUT CONTROL CENTER")
                            .font(.caption.weight(.bold))
                            .tracking(1.5)
                            .foregroundStyle(.white.opacity(0.7))
                        Text("Gõ là \"Fun\",\nInput là chuẩn.")
                            .font(.system(size: 30, weight: .bold, design: .rounded))
                            .foregroundStyle(.white)
                        Label(
                            settings.vietnameseEnabled ? "Funput đang bật" : "Funput đang tạm dừng",
                            systemImage: settings.vietnameseEnabled ? "checkmark.circle.fill" : "pause.circle.fill"
                        )
                        .font(.callout.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.82))
                    }

                    Spacer(minLength: Theme.Spacing.md)

                    VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                        Text(rawExample)
                            .font(.system(.callout, design: .monospaced, weight: .medium))
                            .foregroundStyle(.white.opacity(0.75))
                        Divider().overlay(.white.opacity(0.16))
                        Text("Tôi yêu tiếng Việt")
                            .font(.title3.bold())
                            .foregroundStyle(.white)
                    }
                    .padding(Theme.Spacing.md)
                    .frame(width: 250, alignment: .leading)
                    .background(.black.opacity(0.18), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(.white.opacity(0.14), lineWidth: 0.5)
                    }
                }
                .padding(.horizontal, Theme.Spacing.xl)
                .padding(.vertical, Theme.Spacing.lg)
            }
            .frame(height: 184)
    }
}
