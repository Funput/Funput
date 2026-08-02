import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import os
import UIKit

extension KeyboardViewController {
    func handleKeyEvent(_ event: KeyboardKeyEvent) {
        let alternate: KeyAlternate?
        switch event.phase {
        case .released, .repeated:
            alternate = nil
        case let .alternateSelected(value):
            alternate = value
        case .pressed, .cancelled:
            return
        case .swiped(.toggleLanguage):
            inputCoordinator.toggleLanguage()
            applyPostCommitEffects(
                .init(
                    presentationChanged: true,
                    suggestionsChanged: true
                )
            )
            return
        }

        if event.key.role == .emoji {
            showEmoji()
            return
        }

        if event.key.role == .clipboard {
            showClipboardPanel()
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
        let writer = makeDocumentWriter()
        let effects: KeyboardPostCommitEffects
        if let alternate {
            effects = inputCoordinator.handleAlternate(
                alternate,
                from: event.key,
                writer: writer
            )
        } else {
            effects = inputCoordinator.handle(event.key, writer: writer)
        }
        applyPostCommitEffects(effects)
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
            showsNumberRow: configuration.showsNumberRow
        )
        let themed = configuredThemedPresentation()
        presentation.sizing = themed.sizing
        presentation.shiftState = state.shiftState
        presentation.language = state.language
        presentation.enterAction = state.enterAction
        presentation.theme = themed.theme
        presentation.blendsSystemEdge = themed.blendsSystemEdge
        presentation.isHapticFeedbackEnabled = configuration.isHapticFeedbackEnabled
        presentation.isKeySoundEnabled = configuration.isKeySoundEnabled
        presentation.showsKeyPreviews = !state.editorMode.isPassword && configuration.showsKeyPreviews
        keyboardView.presentation = presentation
        if presentation.layout.toolbar == nil {
            showFunput()
        } else if displayedSurface == .emoji {
            refreshEmojiPresentation()
        } else if displayedSurface == .kaomoji {
            refreshKaomojiPresentation()
        } else if displayedSurface == .clipboard {
            refreshClipboardPanel()
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
