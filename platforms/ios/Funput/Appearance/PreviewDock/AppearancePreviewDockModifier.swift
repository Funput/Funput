import SwiftUI

extension View {
    /// Docks the keyboard preview at the bottom edge of the appearance screen, where
    /// the real keyboard sits, and leaves a corner button to bring it back.
    func appearancePreviewDock(
        model: AppearanceModel,
        isPresented: Binding<Bool>,
        customize: @escaping () -> Void
    ) -> some View {
        modifier(AppearancePreviewDockModifier(
            model: model,
            isPresented: isPresented,
            customize: customize
        ))
    }
}

private struct AppearancePreviewDockModifier: ViewModifier {
    @ObservedObject var model: AppearanceModel
    @Binding var isPresented: Bool
    let customize: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var containerHeight = CGFloat.infinity

    /// The share of the screen the keyboard may take, so a short screen (a phone on
    /// its side) keeps part of the gallery in view.
    private static let keyboardHeightShare: CGFloat = 0.55

    func body(content: Content) -> some View {
        ScrollViewReader { scroll in
            content
                .overlay(alignment: .bottomTrailing) { openButton }
                .safeAreaInset(edge: .bottom, spacing: 0) { dock }
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { containerHeight = $0 }
                .animation(animation, value: isPresented)
                // The dock covers the lower half of the screen, so the previewed
                // theme's row is brought to the top. The row changes while the dock
                // is open when the editor saves a system theme as a custom one.
                .task(id: scrollTarget) {
                    guard let scrollTarget else { return }
                    withAnimation(animation) { scroll.scrollTo(scrollTarget, anchor: .top) }
                }
        }
        // The dock takes the tab bar's place, as the real keyboard would.
        .toolbar(isPresented ? .hidden : .automatic, for: .tabBar)
    }

    @ViewBuilder private var openButton: some View {
        if !isPresented {
            AppearancePreviewOpenButton { isPresented = true }
                .padding(18)
                .transition(reduceMotion ? .opacity : .scale.combined(with: .opacity))
        }
    }

    @ViewBuilder private var dock: some View {
        if isPresented {
            AppearancePreviewDock(
                model: model,
                maxKeyboardHeight: containerHeight * Self.keyboardHeightShare,
                customize: customize,
                close: { isPresented = false }
            )
            .transition(reduceMotion ? .opacity : .move(edge: .bottom))
        }
    }

    private var scrollTarget: String? {
        isPresented ? model.previewSection.scrollID : nil
    }

    private var animation: Animation {
        reduceMotion ? .easeInOut(duration: 0.2) : .spring(response: 0.36, dampingFraction: 0.88)
    }
}
