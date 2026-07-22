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

    func setMarked(_ text: String, _ client: IMKTextInput) {
        let marked = NSAttributedString(string: text, attributes: [
            .underlineStyle: NSUnderlineStyle.single.rawValue,
            .underlineColor: NSColor.labelColor,
        ])
        client.setMarkedText(
            marked,
            selectionRange: NSRange(location: text.utf16.count, length: 0),
            replacementRange: Self.notFound
        )
    }

    func isNonTextKey(_ event: NSEvent, _ scalar: Unicode.Scalar) -> Bool {
        if event.modifierFlags.contains(.function) { return true }
        let value = scalar.value
        if (0xF700...0xF8FF).contains(value) { return true }
        return value < 0x20 && scalar != "\t" && scalar != "\n" && scalar != "\r"
    }
}
