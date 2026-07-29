import SwiftUI

struct LaunchAurora: View {
    let bloomed: Bool

    var body: some View {
        ZStack {
            Color("LaunchBackground")
            glow(.orange, size: 340, x: 130, y: -180)
            glow(.pink, size: 390, x: -150, y: -40)
            glow(.purple, size: 420, x: 150, y: 170)
            glow(.blue, size: 360, x: -130, y: 280)
        }
        .compositingGroup()
    }

    private func glow(_ color: Color, size: CGFloat, x: CGFloat, y: CGFloat) -> some View {
        Circle()
            .fill(color.opacity(0.28))
            .frame(width: size, height: size)
            .blur(radius: 72)
            .scaleEffect(bloomed ? 1 : 0.55)
            .offset(x: bloomed ? x : x * 0.35, y: bloomed ? y : y * 0.35)
            .opacity(bloomed ? 1 : 0)
    }
}
