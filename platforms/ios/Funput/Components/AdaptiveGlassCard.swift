import SwiftUI

struct AdaptiveGlassCard<Content: View>: View {
    private let content: Content
    private let cornerRadius: CGFloat

    init(cornerRadius: CGFloat = 22, @ViewBuilder content: () -> Content) {
        self.cornerRadius = cornerRadius
        self.content = content()
    }

    var body: some View {
        if #available(iOS 26, *) {
            content
                .padding(18)
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .padding(18)
                .background(
                    .regularMaterial,
                    in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                )
        }
    }
}
