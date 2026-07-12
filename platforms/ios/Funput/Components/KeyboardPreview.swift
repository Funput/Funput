import FunputShared
import KeyboardConfiguration
import KeyboardLayout
import KeyboardRenderer
import SwiftUI

struct KeyboardPreview: UIViewRepresentable {
    var presentation: KeyboardPresentation
    var interfaceStyle: UIUserInterfaceStyle = .unspecified

    func makeUIView(context: Context) -> KeyboardSurfaceView {
        let view = KeyboardSurfaceView(presentation: presentation)
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ view: KeyboardSurfaceView, context: Context) {
        view.overrideUserInterfaceStyle = interfaceStyle
        view.presentation = presentation
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
