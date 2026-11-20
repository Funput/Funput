import KeyboardRenderer
import SwiftUI
import ThemeSchema

/// The in-app keyboard preview, docked at the bottom edge at the real keyboard's
/// width and height. It renders in the app, so it can show a theme that is not
/// applied yet and either interface style, which the keyboard extension cannot.
struct AppearancePreviewDock: View {
    @ObservedObject var model: AppearanceModel
    let maxKeyboardHeight: CGFloat
    let customize: () -> Void
    let close: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            AppearancePreviewDockBar(
                mode: model.previewModeBinding,
                isApplied: model.isPreviewApplied,
                isCustom: model.previewCustomTheme != nil,
                apply: model.applyPreview,
                customize: customize,
                close: close
            )
            KeyboardPreview(
                presentation: presentation,
                backgroundImageData: model.imageData(for: model.previewTheme),
                interfaceStyle: model.previewMode.interfaceStyle,
                isInteractive: true
            )
            // New identity per mode rebuilds the surface instead of cross-fading
            // it, so UIKit never interpolates the glass colors out of range.
            .id(model.previewMode)
            .frame(height: presentation.previewHeight(limit: maxKeyboardHeight))
            .accessibilityLabel("Bản xem trước bàn phím \(model.previewTheme.metadata.name)")
        }
        // App chrome all the way down: the strip under the keyboard belongs to the
        // home indicator, and no flat color matches every theme's backdrop there.
        .background { chrome.fill(.regularMaterial).ignoresSafeArea(edges: .bottom) }
        // A phone keyboard stretched across an iPad would misrepresent the keys.
        .frame(maxWidth: AppScreenMetrics.maxContentWidth)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("appearance.previewDock")
    }

    private var presentation: KeyboardPresentation { model.previewPresentation }

    private var chrome: UnevenRoundedRectangle {
        UnevenRoundedRectangle(topLeadingRadius: 24, topTrailingRadius: 24, style: .continuous)
    }
}
