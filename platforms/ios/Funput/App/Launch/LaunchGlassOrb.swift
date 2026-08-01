import SwiftUI

struct LaunchGlassOrb: View {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    let bloomed: Bool

    var body: some View {
        Group {
            if reduceTransparency {
                Circle()
                    .fill(Color(uiColor: .secondarySystemBackground))
                    .frame(width: 220, height: 220)
            } else if #available(iOS 26, *) {
                Color.clear
                    .frame(width: 220, height: 220)
                    .glassEffect(.regular.tint(.white.opacity(0.08)), in: .circle)
            } else {
                Circle()
                    .fill(.ultraThinMaterial)
                    .overlay(Circle().stroke(.white.opacity(0.22), lineWidth: 0.8))
                    .frame(width: 220, height: 220)
            }
        }
        .scaleEffect(bloomed ? 1 : 0.58)
        .opacity(bloomed ? 1 : 0)
    }
}
