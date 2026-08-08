public enum KeyboardLayoutResolver {
    public static func resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = .text,
        showsNumberRow: Bool = true,
        preset: KeyboardLayoutPreset = .funput
    ) -> KeyboardLayout {
        // The system preset describes the plain text and search keyboards, which the stock
        // keyboard renders identically. Email and URL differ from it along other axes and
        // the keypads have no row of this shape at all; password fields must additionally
        // keep their toolbar-less layouts so the emoji panel stays unreachable there — an
        // invariant this guard preserves by construction rather than by a check.
        guard preset == .system, editorMode.usesSystemPreset else {
            return funputLayout(
                inputMethod: inputMethod,
                mode: mode,
                editorMode: editorMode,
                showsNumberRow: showsNumberRow
            )
        }
        return systemLayout(
            inputMethod: inputMethod,
            mode: mode,
            editorMode: editorMode,
            showsNumberRow: showsNumberRow
        )
    }

    private static func funputLayout(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Bool
    ) -> KeyboardLayout {
        let usesCompactLayout = editorMode == .text
            && inputMethod.isTelexFamily
            && !showsNumberRow
        return switch mode {
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
    }

    private static func systemLayout(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Bool
    ) -> KeyboardLayout {
        switch mode {
        case .letters:
            editorMode == .search
                ? SystemKeyboardLayouts.search(inputMethod)
                : SystemKeyboardLayouts.letters(inputMethod, showsNumberRow: showsNumberRow)
        // The symbol pages ignore `showsNumberRow`: page one always carries the digits,
        // so there is no compact variant of them to choose.
        case .symbolsPrimary:
            SystemSymbolKeyboardLayouts.primary(inputMethod)
        case .symbolsSecondary:
            SystemSymbolKeyboardLayouts.secondary(inputMethod)
        }
    }
}
