import AppKit
import InputMethodKit

extension FunputInputController {
    func syncSettings() {
        let settings = AppSettings.shared
        composer.apply(ComposerConfiguration(settings: settings))

        guard lastSyncedShortcutsRevision != settings.shortcutsRevision else { return }
        composer.clearShortcuts()
        for shortcut in settings.shortcuts where !shortcut.trigger.isEmpty {
            composer.addShortcut(trigger: shortcut.trigger, expansion: shortcut.expansion)
        }
        lastSyncedShortcutsRevision = settings.shortcutsRevision
    }

    /// Show `text` as the marked (underlined) composition. `replacementRange` defaults to
    /// "wherever the caret is"; retoning passes a real range to swallow the committed word
    /// it is re-opening, along with the character Backspace was deleting.
    func setMarked(
        _ text: String,
        _ client: IMKTextInput,
        replacementRange: NSRange = FunputInputController.notFound
    ) {
        let marked = NSAttributedString(string: text, attributes: [
            .underlineStyle: NSUnderlineStyle.single.rawValue,
            .underlineColor: NSColor.labelColor,
        ])
        client.setMarkedText(
            marked,
            selectionRange: NSRange(location: text.utf16.count, length: 0),
            replacementRange: replacementRange
        )
    }

    func isNonTextKey(_ event: NSEvent, _ scalar: Unicode.Scalar) -> Bool {
        if event.modifierFlags.contains(.function) { return true }
        let value = scalar.value
        if (0xF700...0xF8FF).contains(value) { return true }
        return value < 0x20 && scalar != "\t" && scalar != "\n" && scalar != "\r"
    }

    /// A digit typed on the numeric keypad. In VNI these must stay literal numbers
    /// rather than act as tone/shape modifiers, so they are committed like a word
    /// boundary (see `commitBoundary(_:into:source:)`). Keypad arrows / Enter also set
    /// `.numericPad` but carry non-digit scalars already filtered by `isNonTextKey`.
    func isNumpadDigit(_ event: NSEvent, _ scalar: Unicode.Scalar) -> Bool {
        event.modifierFlags.contains(.numericPad) && (0x30...0x39).contains(scalar.value)
    }
}
