import SwiftUI

struct AboutHero: View {
    let versionLabel: String

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Ellipse()
                    .fill(
                        LinearGradient(
                            colors: [
                                .orange.opacity(0.20),
                                .pink.opacity(0.22),
                                .purple.opacity(0.18),
                                .blue.opacity(0.18),
                            ],
                            startPoint: .topTrailing,
                            endPoint: .bottomLeading
                        )
                    )
                    .frame(width: 270, height: 210)
                    .blur(radius: 30)
                    .accessibilityHidden(true)

                Image("FunputLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 180, height: 180)
                    .shadow(color: .pink.opacity(0.18), radius: 22, x: 10, y: -8)
                    .shadow(color: .blue.opacity(0.18), radius: 22, x: -10, y: 10)
            }
            Text(versionLabel)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Funput, \(versionLabel)")
        .accessibilityIdentifier("about.hero")
    }
}

#Preview {
    AboutHero(versionLabel: "Phiên bản 1.0 (42)")
        .padding()
}
