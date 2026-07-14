import SwiftUI

struct FunputLaunchExperience: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let onFinished: () -> Void
    @State private var bloomed = false
    @State private var exiting = false

    var body: some View {
        ZStack {
            LaunchAurora(bloomed: bloomed)
            LaunchLogoBloom(bloomed: bloomed)
        }
        .ignoresSafeArea()
        .opacity(exiting ? 0 : 1)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .task { await play() }
    }

    @MainActor private func play() async {
        await Task.yield()
        if reduceMotion {
            try? await Task.sleep(for: .milliseconds(250))
        } else {
            withAnimation(.spring(duration: 0.55, bounce: 0.12)) {
                bloomed = true
            }
            try? await Task.sleep(for: .milliseconds(650))
        }
        withAnimation(.easeOut(duration: 0.22)) { exiting = true }
        try? await Task.sleep(for: .milliseconds(220))
        onFinished()
    }
}

private struct LaunchLogoBloom: View {
    let bloomed: Bool

    var body: some View {
        ZStack {
            LaunchGlassOrb(bloomed: bloomed)
            Image("FunputLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 160, height: 160)
                .shadow(color: .pink.opacity(bloomed ? 0.28 : 0), radius: 24, x: 12, y: -8)
                .shadow(color: .blue.opacity(bloomed ? 0.24 : 0), radius: 24, x: -12, y: 10)
                .overlay { LaunchLogoSweep(bloomed: bloomed) }
        }
    }
}

private struct LaunchLogoSweep: View {
    let bloomed: Bool

    var body: some View {
        LinearGradient(
            colors: [.clear, .white.opacity(0.72), .clear],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(width: 55, height: 230)
        .rotationEffect(.degrees(18))
        .offset(x: bloomed ? 180 : -180)
        .mask {
            Image("FunputLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 160, height: 160)
        }
    }
}
