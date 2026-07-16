import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    func handleKeyEvent(_ event: KeyboardKeyEvent) {
        guard event.key.role != .systemInputMode else { return }
        switch event.phase {
        case .released, .repeated:
            break
        case .pressed, .cancelled:
            return
        case .swiped(.toggleLanguage):
            inputCoordinator.toggleLanguage()
            updateInputPresentation()
            return
        }

        if event.key.role == .emoji {
            showEmoji()
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
            showsSystemInputModeKey: configuration.showsGlobeKey,
            showsNumberRow: configuration.showsNumberRow
        )
        let themed = KeyboardPresentationFactory.make(
            from: configuration,
            layout: presentation.layout,
            catalog: themeCatalog
        )
        presentation.sizing = themed.sizing
        presentation.shiftState = state.shiftState
        presentation.language = state.language
        presentation.enterAction = state.enterAction
        presentation.theme = themed.theme
        presentation.isHapticFeedbackEnabled = configuration.isHapticFeedbackEnabled
        presentation.isKeySoundEnabled = configuration.isKeySoundEnabled
        presentation.showsKeyPreviews = !state.editorMode.isPassword && configuration.showsKeyPreviews
        keyboardView.presentation = presentation
        if presentation.layout.toolbar == nil {
            showFunput()
        } else if displayedSurface == .emoji {
            refreshEmojiPresentation()
        }
        updatePreferredHeight()
    }
}
