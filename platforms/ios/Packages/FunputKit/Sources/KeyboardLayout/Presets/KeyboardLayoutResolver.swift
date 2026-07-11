public enum KeyboardLayoutResolver {
    public static func resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = .text,
        showsSystemInputModeKey: Bool = false
    ) -> KeyboardLayout {
        let layout = switch mode {
        case .letters:
            EditorKeyboardLayouts.resolve(inputMethod, editorMode: editorMode)
        case .symbolsPrimary:
            SymbolKeyboardLayouts.primary(inputMethod, secure: editorMode.isPassword)
        case .symbolsSecondary:
            SymbolKeyboardLayouts.secondary(inputMethod, secure: editorMode.isPassword)
        }
        return layout.withSystemInputModeKey(showsSystemInputModeKey)
    }
}

private extension KeyboardLayout {
    func withSystemInputModeKey(_ visible: Bool) -> KeyboardLayout {
        guard visible, toolbar != nil else { return self }
        return KeyboardLayout(
            id: "\(id)-system-switcher",
            inputMethod: inputMethod,
            toolbar: .withSystemInputMode(for: inputMethod),
            rows: rows
        )
    }
}
