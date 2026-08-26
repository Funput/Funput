import SwiftUI

struct AppScreen<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(uiColor: .systemGroupedBackground),
                    Color.accentColor.opacity(0.08),
                    Color(uiColor: .systemGroupedBackground),
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                screenContent
            }
        }
    }

    @ViewBuilder private var screenContent: some View {
        if #available(iOS 26, *) {
            // Rendering the screen's custom glass surfaces as one group avoids a
            // separate sampling pass per card while the native tab bar is moving.
            GlassEffectContainer(spacing: 0) {
                cardStack
            }
        } else {
            cardStack
        }
    }

    private var cardStack: some View {
        // These are short forms. Keeping every glass card mounted avoids
        // an iOS 26 LazyVStack/glass layout loop while scrolling.
        VStack(alignment: .leading, spacing: 18) {
            content
        }
        .frame(maxWidth: 720)
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
    }
}
