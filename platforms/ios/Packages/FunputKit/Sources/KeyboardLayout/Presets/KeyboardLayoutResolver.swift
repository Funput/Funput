public enum KeyboardLayoutResolver {
    public static func resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = .text,
        showsSystemInputModeKey: Bool = false,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        let usesCompactLayout = editorMode == .text
            && inputMethod.isTelexFamily
            && !showsNumberRow
        let layout = switch mode {
        case .letters:
            EditorKeyboardLayouts.resolve(
                inputMethod,
                editorMode: editorMode,
                showsNumberRow: showsNumberRow
            )
        case .symbolsPrimary:
            usesCompactLayout
                ? CompactSymbolKeyboardLayouts.primary(inputMethod)
                : SymbolKeyboardLayouts.primary(inputMethod, secure: editorMode.isPassword)
        case .symbolsSecondary:
            usesCompactLayout
                ? CompactSymbolKeyboardLayouts.secondary(inputMethod)
                : SymbolKeyboardLayouts.secondary(inputMethod, secure: editorMode.isPassword)
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
            toolbar: .withSystemInputMode,
            rows: rows
        )
    }
}
