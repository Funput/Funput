import AppKit
import InputMethodKit

extension FunputInputController {
    /// Own the raw word as marked text so clients need not expose committed ranges.
    /// The disabled engine still handles shortcut matching; it never adds diacritics.
    func handleEnglish(_ event: NSEvent, client: IMKTextInput) -> Bool {
        let settings = AppSettings.shared
        guard settings.shortcutsEnabled, settings.shortcutsInEnglish,
            hasSyncedShortcuts
        else { commit(into: client); return false }

        guard !InputEventPolicy.passesThroughEnglish(keyCode: event.keyCode, modifiers: event.modifierFlags)
        else { commit(into: client); return false }

        if event.keyCode == 51 {
            guard let edit = EnglishComposition.backspace(composer: composer) else { return false }
            return applyEnglish(edit, client: client)
        }
        guard let characters = event.characters, characters.unicodeScalars.count == 1,
            let scalar = characters.unicodeScalars.first, !isNonTextKey(event, scalar)
        else { commit(into: client); return false }

        return applyEnglish(EnglishComposition.process(scalar, composer: composer), client: client)
    }

    private func applyEnglish(_ edit: EnglishComposition.Edit, client: IMKTextInput) -> Bool {
        if edit.marked {
            setMarked(edit.text, client)
        } else if !edit.text.isEmpty {
            client.insertText(edit.text, replacementRange: Self.notFound)
        }
        return edit.handled
    }
}
