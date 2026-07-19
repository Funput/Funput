import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import os
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
            clearPersonalSuggestions()
            updateInputPresentation()
            return
        }

        if event.key.role == .emoji {
            showEmoji()
            return
        }

        let signpostID = OSSignpostID(log: KeyboardControllerSignpost.log)
        os_signpost(
            .begin,
            log: KeyboardControllerSignpost.log,
            name: "KeyHandler",
            signpostID: signpostID
        )
        defer {
            os_signpost(
                .end,
                log: KeyboardControllerSignpost.log,
                name: "KeyHandler",
                signpostID: signpostID
            )
        }
        let previousState = inputCoordinator.state
        let document = TextDocumentProxyAdapter(proxy: textDocumentProxy)
        inputCoordinator.handle(event.key, document: document)
        publishPersonalSuggestionUpdate()

        if inputCoordinator.state != previousState {
            updateInputPresentation()
        }
    }

    func updateInputPresentation() {
        let signpostID = OSSignpostID(log: KeyboardControllerSignpost.log)
        os_signpost(
            .begin,
            log: KeyboardControllerSignpost.log,
            name: "PresentationUpdate",
            signpostID: signpostID
        )
        defer {
            os_signpost(
                .end,
                log: KeyboardControllerSignpost.log,
                name: "PresentationUpdate",
                signpostID: signpostID
            )
        }
        let state = inputCoordinator.state
        var presentation = keyboardView.presentation
        presentation.layout = KeyboardLayoutResolver.resolve(
            inputMethod: state.inputMethod,
            mode: state.layoutMode,
            editorMode: state.editorMode,
            showsSystemInputModeKey: configuration.showsGlobeKey,
            showsNumberRow: configuration.showsNumberRow
        )
        let themed = configuredThemedPresentation()
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

    private func configuredThemedPresentation() -> KeyboardPresentation {
        if cachedPresentationConfiguration == configuration,
           let cachedThemedPresentation {
            return cachedThemedPresentation
        }
        let value = KeyboardPresentationFactory.make(
            from: configuration,
            catalog: themeCatalog
        )
        cachedPresentationConfiguration = configuration
        cachedThemedPresentation = value
        return value
    }
}

private enum KeyboardControllerSignpost {
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Controller")
}
