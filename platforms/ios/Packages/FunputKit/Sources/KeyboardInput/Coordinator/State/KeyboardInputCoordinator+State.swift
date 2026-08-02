#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

extension KeyboardInputCoordinator {
    /// Applies all traits resolved from the current UIKit text input.
    public func updateContext(_ context: KeyboardInputContext) {
        let editorChanged = state.editorMode != context.editorMode
        let layoutChanged = state.layoutMode != context.initialLayoutMode
        let capitalizationChanged = state.autocapitalization != context.autocapitalization
        let inputContextChanged = editorChanged || layoutChanged || capitalizationChanged
        guard inputContextChanged || state.enterAction != context.enterAction else { return }

        if inputContextChanged {
            composer.clear()
            shiftController.resetTapSequence()
            documentSynchronizer.invalidate()
            resetPersonalSuggestionTracking()
        }
        replaceState(
            shiftState: inputContextChanged ? .lowercase : state.shiftState,
            layoutMode: context.initialLayoutMode,
            editorMode: context.editorMode,
            enterAction: context.enterAction,
            autocapitalization: context.autocapitalization
        )
        if inputContextChanged {
            composer.setEnabled(state.usesVietnameseComposition)
        }
    }

    func consumeOneShotShift() {
        guard state.shiftState == .uppercase else { return }
        shiftController.resetTapSequence()
        replaceState(shiftState: .lowercase)
    }

    func toggleShift() {
        replaceState(shiftState: shiftController.toggle(from: state.shiftState))
    }

    func toggleInputMethod() {
        let next: KeyboardInputMethod
        if state.inputMethod == .vni {
            next = preferredTelexMethod
        } else {
            preferredTelexMethod = state.inputMethod
            next = .vni
        }
        composer.clear()
        composer.setInputMethod(next.engineMethod)
        documentSynchronizer.invalidate()
        resetPersonalSuggestionTracking()
        replaceState(inputMethod: next)
    }

    public func toggleLanguage() {
        guard state.editorMode.supportsVietnameseComposition else { return }
        composer.clear()
        resetPersonalSuggestionTracking()
        let next: KeyboardLanguage = state.language == .vietnamese ? .english : .vietnamese
        replaceState(language: next)
        composer.setEnabled(state.usesVietnameseComposition)
    }

    func updateLayoutMode(_ mode: KeyboardLayoutMode) {
        guard !state.editorMode.usesKeypad, state.layoutMode != mode else { return }
        replaceState(layoutMode: mode)
    }

    func replaceState(
        inputMethod: KeyboardInputMethod? = nil,
        shiftState: ShiftState? = nil,
        layoutMode: KeyboardLayoutMode? = nil,
        editorMode: KeyboardEditorMode? = nil,
        enterAction: KeyboardEnterAction? = nil,
        language: KeyboardLanguage? = nil,
        autocapitalization: KeyboardAutocapitalizationMode? = nil
    ) {
        state = KeyboardInputState(
            inputMethod: inputMethod ?? state.inputMethod,
            shiftState: shiftState ?? state.shiftState,
            layoutMode: layoutMode ?? state.layoutMode,
            editorMode: editorMode ?? state.editorMode,
            enterAction: enterAction ?? state.enterAction,
            language: language ?? state.language,
            autocapitalization: autocapitalization ?? state.autocapitalization
        )
        suggestionTrackingActive = personalSuggestionsEnabled
            && state.language == .vietnamese
            && state.editorMode.supportsVietnameseComposition
    }
}
#endif
