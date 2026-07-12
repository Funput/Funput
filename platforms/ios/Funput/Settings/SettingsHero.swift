import SwiftUI

struct SettingsHero: View {
    var body: some View {
        AdaptiveGlassCard(cornerRadius: 28) {
            FunputIdentityView(hero: true)
                .padding(.vertical, 12)
        }
        .background(alignment: .top) {
            Circle()
                .fill(Color.accentColor.opacity(0.18))
                .frame(width: 210, height: 210)
                .offset(y: -34)
                .blur(radius: 44)
                .accessibilityHidden(true)
        }
        .accessibilityElement(children: .combine)
    }
}
