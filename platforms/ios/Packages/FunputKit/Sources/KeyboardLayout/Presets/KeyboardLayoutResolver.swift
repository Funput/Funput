public enum KeyboardLayoutResolver {
    public static func resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = .text,
        showsNumberRow: Bool = true,
        preset: KeyboardLayoutPreset = .funput,
        showsToolbar: Bool = true
    ) -> KeyboardLayout {
        // The system preset describes text and search; other editors retain their
        // specialized key rows. Toolbar visibility is applied afterwards, independently
        // of that choice, while secure layouts remain protected by having no toolbar.
        let layout = if preset == .system, editorMode.usesSystemPreset {
            systemLayout(
                inputMethod: inputMethod,
                mode: mode,
                editorMode: editorMode,
                showsNumberRow: showsNumberRow
            )
        } else {
            funputLayout(
                inputMethod: inputMethod,
                mode: mode,
                editorMode: editorMode,
                showsNumberRow: showsNumberRow
            )
        }
        guard !showsToolbar, layout.toolbar != nil else { return layout }
        return toolbarless(layout)
    }

    /// Moves the emoji entry point into the action row before removing the toolbar.
    /// Secure layouts and keypads arrive without a toolbar and never enter this path.
    private static func toolbarless(_ layout: KeyboardLayout) -> KeyboardLayout {
        var rows = layout.rows
        if !rows.contains(where: { $0.keys.contains { $0.role == .emoji } }),
           let actionIndex = rows.indices.last {
            let action = rows[actionIndex]
            var keys = action.keys
            keys.insert(actionRowEmojiKey(page: layout.id), at: min(1, keys.count))
            rows[actionIndex] = KeyboardRow(
                keys: keys,
                horizontalInsetUnits: action.horizontalInsetUnits
            )
        }
        return KeyboardLayout(
            id: "\(layout.id)-toolbarless",
            inputMethod: layout.inputMethod,
            toolbar: nil,
            rows: rows
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
