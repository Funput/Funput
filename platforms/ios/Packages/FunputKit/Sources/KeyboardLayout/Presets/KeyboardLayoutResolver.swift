public enum KeyboardLayoutResolver {
    public static func resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode = .text,
        showsNumberRow: Bool = true,
        preset: KeyboardLayoutPreset = .funput,
        showsToolbar: Bool = true,
        allowsLanguageToggle: Bool = true
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
        let toggled = allowsLanguageToggle ? layout : languageLocked(layout)
        guard !showsToolbar, toggled.toolbar != nil else { return toggled }
        return toolbarless(toggled)
    }

    /// Turns every language-switching space bar back into a plain one.
    ///
    /// Applied to the resolved layout rather than threaded through the factories: the
    /// switch rides on the space key of the letters page, both symbol pages and every
    /// preset, and they have no other reason to know about the setting. Secure pages
    /// already carry a space bar without the switch and pass through untouched.
    private static func languageLocked(_ layout: KeyboardLayout) -> KeyboardLayout {
        KeyboardLayout(
            id: "\(layout.id)-vietnamese",
            inputMethod: layout.inputMethod,
            toolbar: layout.toolbar,
            rows: layout.rows.map { row in
                guard row.keys.contains(where: { $0.horizontalSwipeAction == .toggleLanguage })
                else { return row }
                return KeyboardRow(
                    keys: row.keys.map(plainSpaceKey),
                    horizontalInsetUnits: row.horizontalInsetUnits,
                    isNumberRow: row.isNumberRow,
                    columnSpans: row.columnSpans
                )
            }
        )
    }

    private static func plainSpaceKey(_ key: KeySpec) -> KeySpec {
        guard key.horizontalSwipeAction == .toggleLanguage else { return key }
        return KeySpec(
            id: key.id,
            // The renderer shows a static label and drops the chevrons once the swipe is
            // gone, so the key reads as what it now is.
            label: "␣",
            role: key.role,
            widthWeight: key.widthWeight,
            accessibilityLabel: "Dấu cách. Giữ rồi kéo để di chuyển con trỏ",
            alternates: key.alternates
        )
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
        let usesCompactLayout = usesCompactLetterRows(
            inputMethod: inputMethod,
            editorMode: editorMode,
            showsNumberRow: showsNumberRow
        )
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
