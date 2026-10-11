/// The iPad pages, modelled on the stock iPad keyboard: what the system preset means on iPad.
///
/// Text, search, email, URL and password fields get them. The Funput preset, keypads
/// (number, phone, PIN) and every phone keep the existing layouts.
enum PadKeyboardLayouts {
    static func resolve(
        formFactor: KeyboardFormFactor,
        inputMethod: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Bool,
        preset: KeyboardLayoutPreset
    ) -> KeyboardLayout? {
        guard formFactor == .pad, preset == .system, !editorMode.usesKeypad else { return nil }
        let page = PadPage(
            inputMethod: inputMethod,
            editorMode: editorMode,
            showsNumberRow: showsNumberRow
        )
        return switch mode {
        case .letters:
            PadLetterKeyboardLayouts.letters(page)
        case .symbolsPrimary:
            PadSymbolKeyboardLayouts.primary(page)
        case .symbolsSecondary:
            PadSymbolKeyboardLayouts.secondary(page)
        }
    }
}

/// What every iPad page of one field needs to know.
struct PadPage {
    let inputMethod: KeyboardInputMethod
    let editorMode: KeyboardEditorMode
    let showsNumberRow: Bool

    /// Text and search compose Vietnamese; email, URL and password type plain ASCII.
    var composes: Bool { editorMode.supportsVietnameseComposition }

    /// Same answer as the phone pages: Telex may drop the digit row, VNI never does, and a
    /// password field always keeps it.
    var isCompact: Bool {
        usesCompactLetterRows(
            inputMethod: inputMethod,
            editorMode: editorMode,
            showsNumberRow: showsNumberRow
        )
    }

    /// Passwords get no toolbar, as on the phone.
    var toolbar: KeyboardToolbarSpec? { editorMode.isPassword ? nil : .standard }

    /// Rows on the letters page, which every page of the field is budgeted for.
    var letterRowCount: Int { isCompact ? 4 : 5 }

    /// The suffix that keeps layout ids apart per field kind; text and search share one.
    var editorID: String { composes ? "" : "-\(editorMode.rawValue)" }
}
