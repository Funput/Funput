#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

extension KeyboardInputCoordinator {
    public func updateContext(
        editorMode: KeyboardEditorMode,
        enterAction: KeyboardEnterAction,
        initialLayoutMode: KeyboardLayoutMode = .letters
    ) {
        let editorChanged = state.editorMode != editorMode
        let layoutChanged = state.layoutMode != initialLayoutMode
        guard editorChanged || layoutChanged || state.enterAction != enterAction else { return }

        let inputContextChanged = editorChanged || layoutChanged
        if inputContextChanged {
            composer.clear()
            shiftController.resetTapSequence()
        }
        replaceState(
            shiftState: inputContextChanged ? .lowercase : state.shiftState,
            layoutMode: initialLayoutMode,
            editorMode: editorMode,
            enterAction: enterAction
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
        let next: KeyboardInputMethod = state.inputMethod == .vni ? .telex : .vni
        composer.clear()
        composer.setInputMethod(next.engineMethod)
        replaceState(inputMethod: next)
    }

    public func toggleLanguage() {
        guard state.editorMode.supportsVietnameseComposition else { return }
        composer.clear()
        let next: KeyboardLanguage = state.language == .vietnamese ? .english : .vietnamese
        replaceState(language: next)
        composer.setEnabled(state.usesVietnameseComposition)
    }

    public func prepareForSystemInputModeChange() {
        composer.clear()
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
        language: KeyboardLanguage? = nil
    ) {
        state = KeyboardInputState(
            inputMethod: inputMethod ?? state.inputMethod,
            shiftState: shiftState ?? state.shiftState,
            layoutMode: layoutMode ?? state.layoutMode,
            editorMode: editorMode ?? state.editorMode,
            enterAction: enterAction ?? state.enterAction,
            language: language ?? state.language
        )
    }
}
#endif
