import FunputShared
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
        guard applyInputPresentation() else { return }
        updatePreferredHeight()
    }

    @discardableResult
    func applyInputPresentation() -> Bool {
        guard let themed = cachedThemedPresentation else { return false }
        let presentation = makeInputPresentation(
            configuration: configuration,
            themed: themed
        )
        currentPresentation = presentation
        applyPresentationToSurfaces(presentation)
        return true
    }

    func makeInputPresentation(
        configuration: FunputConfiguration,
        themed: KeyboardPresentation
    ) -> KeyboardPresentation {
        let identifier = OSSignpostID(log: KeyboardControllerSignpost.log)
        os_signpost(
            .begin, log: KeyboardControllerSignpost.log,
            name: "PresentationUpdate", signpostID: identifier
        )
        defer {
            os_signpost(
                .end, log: KeyboardControllerSignpost.log,
                name: "PresentationUpdate", signpostID: identifier
            )
        }
        let state = inputCoordinator.state
        var presentation = themed
        presentation.layout = KeyboardLayoutResolver.resolve(
            inputMethod: state.inputMethod,
            mode: state.layoutMode,
            editorMode: state.editorMode,
            showsNumberRow: configuration.showsNumberRow,
            preset: configuration.layoutPreset
        )
        presentation.shiftState = state.shiftState
        presentation.language = state.language
        presentation.enterAction = state.enterAction
        presentation.showsKeyPreviews = !state.editorMode.isPassword
            && configuration.showsKeyPreviews
        return presentation
    }

    func applyPresentationToSurfaces(_ presentation: KeyboardPresentation) {
        keyboardView.presentation = presentation
        applyPresentationToSupplementarySurfaces(presentation)
    }

    func applyPresentationToSupplementarySurfaces(
        _ presentation: KeyboardPresentation
    ) {
        if presentation.layout.toolbar == nil {
            showFunput()
        } else if displayedSurface == .emoji {
            refreshEmojiPresentation()
        } else if displayedSurface == .kaomoji {
            refreshKaomojiPresentation()
        } else if displayedSurface == .clipboard {
            refreshClipboardPanel()
        }
    }
}

enum KeyboardControllerSignpost {
    static let log = OSLog(subsystem: "app.funput.keyboard", category: "Controller")
}
