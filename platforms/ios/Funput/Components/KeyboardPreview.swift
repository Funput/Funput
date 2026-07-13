import FunputShared
import KeyboardConfiguration
import KeyboardLayout
import KeyboardRenderer
import SwiftUI
import ThemeSchema

/// Hosts the production ``KeyboardSurfaceView`` for in-app previews.
///
/// The keyboard extension renders glass themes over the system keyboard's host
/// material. The app has no such host, so this view paints the theme's own
/// resolved background behind the surface: clear glass keys then read against the
/// theme's intended backdrop instead of the app's, so a dark theme like Midnight
/// stays legible even while previewing the light interface style.
struct KeyboardPreview: UIViewRepresentable {
    var presentation: KeyboardPresentation
    var interfaceStyle: UIUserInterfaceStyle = .unspecified
    var isInteractive = false

    func makeUIView(context: Context) -> KeyboardPreviewSurface {
        let view = KeyboardPreviewSurface()
        view.surface.onKeyEvent = { _ in }
        return view
    }

    func updateUIView(_ view: KeyboardPreviewSurface, context: Context) {
        view.overrideUserInterfaceStyle = interfaceStyle
        view.apply(presentation: presentation, isInteractive: isInteractive)
    }
}

/// A keyboard surface layered over the theme's resolved background, standing in
/// for the keyboard host so clear glass keys have an in-context backdrop.
final class KeyboardPreviewSurface: UIView {
    let surface = KeyboardSurfaceView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(surface)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        surface.frame = bounds
    }

    func apply(presentation: KeyboardPresentation, isInteractive: Bool) {
        backgroundColor = backdropColor(for: presentation.theme)
        surface.isUserInteractionEnabled = isInteractive
        surface.accessibilityElementsHidden = !isInteractive
        surface.accessibilityElements = isInteractive ? nil : []
        surface.presentation = presentation
    }

    /// The theme's resolved background for the current style, forced opaque so the
    /// glass keys sample a solid in-theme surface (the extension gets this from
    /// the host material). sRGB components stay in range, unlike a cross-fade.
    private func backdropColor(for theme: ResolvedTheme) -> UIColor {
        let rgba = overrideUserInterfaceStyle == .dark
            ? theme.backgroundStart.dark
            : theme.backgroundStart.light
        return UIColor(red: rgba.red, green: rgba.green, blue: rgba.blue, alpha: 1)
    }
}

enum KeyboardPreviewPresentation {
    static func make(
        configuration: FunputConfiguration,
        showsSystemInputModeKey: Bool = true
    ) -> KeyboardPresentation {
        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: configuration.inputMethod,
            mode: .letters,
            showsSystemInputModeKey: showsSystemInputModeKey
        )
        return KeyboardPresentationFactory.make(from: configuration, layout: layout)
    }
}
