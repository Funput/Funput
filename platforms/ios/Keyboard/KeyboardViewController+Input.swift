import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func handleKeyEvent(_ event: KeyboardKeyEvent) {
        switch event.phase {
        case .released, .repeated:
            break
        case .pressed, .cancelled:
            return
        }

        let previousState = inputCoordinator.state
        let document = TextDocumentProxyAdapter(proxy: textDocumentProxy)
        inputCoordinator.handle(event.key, document: document)

        if inputCoordinator.state != previousState {
            updateInputPresentation()
        }
    }

    func updateInputPresentation() {
        let state = inputCoordinator.state
        var presentation = keyboardView.presentation
        presentation.layout = KeyboardLayoutResolver.resolve(
            inputMethod: state.inputMethod,
            mode: state.layoutMode,
            editorMode: state.editorMode,
            showsSystemInputModeKey: needsInputModeSwitchKey
        )
        presentation.shiftState = state.shiftState
        presentation.language = .vietnamese
        presentation.enterAction = state.enterAction
        presentation.showsKeyPreviews = !state.editorMode.isPassword
        keyboardView.presentation = presentation
        updatePreferredHeight()
    }
}
