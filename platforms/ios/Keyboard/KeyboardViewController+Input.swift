import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func handleKeyEvent(_ event: KeyboardKeyEvent) {
        guard event.phase == .released else { return }

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
        keyboardView.presentation = presentation
        updatePreferredHeight()
    }
}
