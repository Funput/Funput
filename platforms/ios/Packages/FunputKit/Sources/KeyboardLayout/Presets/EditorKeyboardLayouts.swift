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
            webLayout(
                prefix: "search",
                inputMethod: inputMethod,
                supportsLanguageSwipe: true,
                supportsVietnameseAlternates: true
            )
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

    private static func webLayout(
        prefix: String,
        inputMethod: KeyboardInputMethod,
        supportsLanguageSwipe: Bool = false,
        supportsVietnameseAlternates: Bool = false
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
                middleAccessibility: "Dấu gạch chéo",
                supportsLanguageSwipe: supportsLanguageSwipe
            ),
            supportsVietnameseAlternates: supportsVietnameseAlternates
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
