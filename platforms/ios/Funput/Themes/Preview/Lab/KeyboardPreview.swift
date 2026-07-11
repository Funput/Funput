import KeyboardRenderer
import SwiftUI

struct KeyboardPreview: UIViewRepresentable {
    var presentation: KeyboardPresentation
    var interfaceStyle: UIUserInterfaceStyle

    func makeUIView(context: Context) -> KeyboardSurfaceView {
        let view = KeyboardSurfaceView(presentation: presentation)
        view.onKeyEvent = { _ in }
        return view
    }

    func updateUIView(_ view: KeyboardSurfaceView, context: Context) {
        view.overrideUserInterfaceStyle = interfaceStyle
        view.presentation = presentation
    }
}
