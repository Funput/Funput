import AppKit

enum InputEventPolicy {
    /// Keep macOS commands, including Option-Delete's word deletion, native in EN.
    static func passesThroughEnglish(keyCode: UInt16, modifiers: NSEvent.ModifierFlags) -> Bool {
        !modifiers.isDisjoint(with: [.command, .control, .function])
            || (keyCode == 51 && modifiers.contains(.option))
    }

    /// The text a key types, or nil for one that types none: Backspace, Tab, Enter, Esc,
    /// navigation and function keys, Command/Control shortcuts and dead keys.
    static func typedText(_ characters: String?, modifiers: NSEvent.ModifierFlags) -> String? {
        guard modifiers.isDisjoint(with: [.command, .control, .function]),
            let characters, !characters.isEmpty,
            characters.unicodeScalars.allSatisfy(isPrintable)
        else { return nil }
        return characters
    }

    /// Not a control character, and not one of the function-key characters AppKit puts in
    /// the private-use area for arrows, F-keys, Home, End and the like.
    private static func isPrintable(_ scalar: Unicode.Scalar) -> Bool {
        scalar.value >= 0x20 && scalar.value != 0x7F && !(0xF700...0xF8FF).contains(scalar.value)
    }

    static func isBoundary(_ scalar: Unicode.Scalar, method: InputMethod) -> Bool {
        guard scalar.isASCII else { return false }
        if method == .telexAdvanced, scalar == "[" || scalar == "]" {
            return false
        }
        if scalar == " " || scalar == "\t" || scalar == "\n" || scalar == "\r" {
            return true
        }
        let value = scalar.value
        return (0x21...0x2F).contains(value) || (0x3A...0x40).contains(value)
            || (0x5B...0x60).contains(value) || (0x7B...0x7E).contains(value)
    }
}
