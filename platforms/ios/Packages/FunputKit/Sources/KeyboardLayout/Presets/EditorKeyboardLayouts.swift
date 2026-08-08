public enum EditorKeyboardLayouts {
    public static func resolve(
        _ inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        switch editorMode {
        case .text:
            StandardKeyboardLayouts.letters(inputMethod, showsNumberRow: showsNumberRow)
        case .search:
            searchLayout(inputMethod)
        case .email:
            emailLayout(inputMethod)
        case .url:
            webLayout(prefix: "url", inputMethod: inputMethod)
        case .phone:
            PhoneKeyboardLayouts.resolve(inputMethod)
        case .password:
            PasswordKeyboardLayouts.text(inputMethod)
        case .pin:
            PasswordKeyboardLayouts.pin(inputMethod)
        case .number, .numberDecimal, .numberSigned, .numberSignedDecimal:
            NumberKeyboardLayouts.resolve(inputMethod, mode: editorMode)
        }
    }

    private static func emailLayout(_ method: KeyboardInputMethod) -> KeyboardLayout {
        qwertyLayout(
            id: "qwerty-email-\(method.rawValue)",
            inputMethod: method,
            leadingRows: [topNumberRow(.plainCharacter, pageID: "email-\(method.rawValue)")],
            actionKeys: webActionKeys(middleID: "at", middleLabel: "@", middleAccessibility: "A còng")
        )
    }

    /// Search composes Vietnamese, so it carries the same tone hints and the same VNI
    /// modifier row as the letters page — a field where the tones work but their hints are
    /// hidden, and where Shift is eaten by a tone key, just behaves differently for no
    /// reason the user can see. Only the action row keeps the web shape.
    private static func searchLayout(_ method: KeyboardInputMethod) -> KeyboardLayout {
        qwertyLayout(
            id: "qwerty-search-\(method.rawValue)",
            inputMethod: method,
            leadingRows: [topNumberRow(for: method, pageID: "search-\(method.rawValue)")],
            actionKeys: webActionKeys(
                middleID: "slash",
                middleLabel: "/",
                middleAccessibility: "Dấu gạch chéo",
                supportsLanguageSwipe: true
            ),
            showsTelexHints: method.isTelexFamily,
            supportsVietnameseAlternates: true
        )
    }

    /// URL fields do not compose Vietnamese, so no tone hints and plain digits.
    private static func webLayout(
        prefix: String,
        inputMethod: KeyboardInputMethod
    ) -> KeyboardLayout {
        qwertyLayout(
            id: "qwerty-\(prefix)-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            leadingRows: [
                topNumberRow(.plainCharacter, pageID: "\(prefix)-\(inputMethod.rawValue)"),
            ],
            actionKeys: webActionKeys(
                middleID: "slash",
                middleLabel: "/",
                middleAccessibility: "Dấu gạch chéo"
            )
        )
    }

    private static func webActionKeys(
        middleID: String,
        middleLabel: String,
        middleAccessibility: String,
        supportsLanguageSwipe: Bool = false
    ) -> [KeySpec] {
        [
            specialKey("symbols", "?123", .symbols, weight: 1.7, accessibilityLabel: "Ký hiệu"),
            specialKey(
                middleID,
                middleLabel,
                .punctuation,
                accessibilityLabel: middleAccessibility
            ),
            supportsLanguageSwipe ? standardSpaceKey() : asciiSpaceKey(weight: 5.8),
            specialKey("period", ".", .punctuation, accessibilityLabel: "Dấu chấm"),
            specialKey("enter", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
        ]
    }
}
