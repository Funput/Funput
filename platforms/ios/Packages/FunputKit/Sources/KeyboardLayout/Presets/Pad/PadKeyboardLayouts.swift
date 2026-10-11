import CoreGraphics

/// The letters page of the stock iPad keyboard, shared by the Funput and system presets.
///
/// ```
/// tab    q w e r t y u i o p   delete
/// caps   a s d f g h j k l     return
/// shift  z x c v b n m , .     shift
/// globe  ?123   space   ?123   hide
/// ```
///
/// Every row is weighted to the same total, so the letters keep their column from row to
/// row. The page is layout only: the iPad-specific keys are drawn but nothing handles them
/// yet. Anything that is not the letters page of a text or search field, and every phone,
/// is left to the existing layouts.
enum PadKeyboardLayouts {
    static func resolve(
        formFactor: KeyboardFormFactor,
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Bool,
        preset: KeyboardLayoutPreset
    ) -> KeyboardLayout? {
        guard formFactor == .pad, mode == .letters, editorMode.usesSystemPreset else { return nil }
        return letters(inputMethod, showsNumberRow: showsNumberRow, preset: preset)
    }

    static func letters(
        _ inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool,
        preset: KeyboardLayoutPreset
    ) -> KeyboardLayout {
        // Same answer as the phone pages: Telex may drop the digit row, VNI never does.
        let isCompact = usesCompactLetterRows(
            inputMethod: inputMethod,
            editorMode: .text,
            showsNumberRow: showsNumberRow
        )
        let layout = KeyboardLayout(
            id: "qwerty-pad-\(preset.rawValue)-\(inputMethod.rawValue)\(isCompact ? "-compact" : "")",
            inputMethod: inputMethod,
            toolbar: .standard,
            rows: (isCompact ? [] : [numberRow(inputMethod)]) + [
                row(
                    leading: edgeKey("tab", "tab", .tab, "Tab"),
                    letters: "qwertyuiop",
                    trailing: edgeKey("backspace", "", .backspace, "Xóa"),
                    inputMethod: inputMethod
                ),
                row(
                    leading: edgeKey("capslock", "caps lock", .capsLock, "Caps Lock", weight: wide),
                    letters: "asdfghjkl",
                    trailing: edgeKey("enter", "", .enter, "Enter", weight: wide),
                    inputMethod: inputMethod
                ),
                bottomRow(inputMethod),
                actionRow,
            ]
        )
        return isCompact ? CompactDigitAlternates.decorate(layout) : layout
    }

    /// What each row's width adds up to, so the letters line up.
    private static let side: CGFloat = 1.5
    private static let wide: CGFloat = 2

    private static func edgeKey(
        _ id: String,
        _ label: String,
        _ role: KeyRole,
        _ accessibility: String,
        weight: CGFloat = PadKeyboardLayouts.side
    ) -> KeySpec {
        specialKey("\(id)-pad", label, role, weight: weight, accessibilityLabel: accessibility)
    }

    private static func letterKeys(_ characters: String, _ inputMethod: KeyboardInputMethod) -> [KeySpec] {
        characters.map {
            characterKey($0, showsTelexHint: inputMethod.isTelexFamily, supportsVietnameseAlternates: true)
        }
    }

    private static func row(
        leading: KeySpec,
        letters: String,
        trailing: KeySpec,
        inputMethod: KeyboardInputMethod
    ) -> KeyboardRow {
        KeyboardRow(keys: [leading] + letterKeys(letters, inputMethod) + [trailing])
    }

    /// The digits, held to the letter columns by an empty key at each end.
    private static func numberRow(_ inputMethod: KeyboardInputMethod) -> KeyboardRow {
        let digits = topNumberRow(for: inputMethod, pageID: "pad-\(inputMethod.rawValue)")
        func spacer(_ id: String) -> KeySpec {
            KeySpec(
                id: "placeholder-pad-\(id)",
                label: "",
                role: .placeholder,
                widthWeight: side,
                accessibilityLabel: "Vị trí trống"
            )
        }
        return KeyboardRow(keys: [spacer("leading")] + digits.keys + [spacer("trailing")], isNumberRow: true)
    }

    /// `shift z … m , . shift`; the stock keys print `!` and `?` above the comma and period.
    private static func bottomRow(_ inputMethod: KeyboardInputMethod) -> KeyboardRow {
        let comma = KeySpec(
            id: "comma-pad",
            label: ",",
            role: .punctuation,
            secondaryLabel: "!",
            accessibilityLabel: "Dấu phẩy"
        )
        let period = KeySpec(
            id: "period-pad",
            label: ".",
            role: .punctuation,
            secondaryLabel: "?",
            accessibilityLabel: "Dấu chấm",
            alternates: PunctuationKeyAlternates.period,
            alternateColumns: PunctuationKeyAlternates.periodColumns
        )
        return KeyboardRow(keys:
            [edgeKey("shift", "", .shift, "Shift", weight: wide)]
                + letterKeys("zxcvbnm", inputMethod) + [comma, period]
                + [edgeKey("shift-right", "", .shift, "Shift", weight: wide)]
        )
    }

    /// `globe ?123 space ?123 hide`, with no dictation key.
    private static let actionRow = KeyboardRow(keys: [
        edgeKey("globe", "", .globe, "Đổi bàn phím"),
        edgeKey("symbols", "?123", .symbols, "Ký hiệu"),
        standardSpaceKey(weight: 7),
        edgeKey("symbols-right", "?123", .symbols, "Ký hiệu"),
        edgeKey("dismiss", "", .dismissKeyboard, "Ẩn bàn phím"),
    ])
}
