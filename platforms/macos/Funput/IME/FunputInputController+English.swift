import AppKit
import InputMethodKit

extension FunputInputController {
    /// Own the raw word as the live composition, so an expanding shortcut can replace it:
    /// written into the document where the session can, marked text elsewhere, which
    /// needs no committed ranges from the client. The disabled engine still handles
    /// shortcut matching; it never adds diacritics.
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

    /// English never rewrites letters as they are typed, so a word the session drops (only
    /// possible on Backspace) is fine to hand back: the app performs the key itself.
    private func applyEnglish(_ edit: EnglishComposition.Edit, client: IMKTextInput) -> Bool {
        let surface = IMKCompositionSurface(client: client)
        if edit.marked {
            guard session.show(edit.text, on: surface) else {
                composer.clear()
                return false
            }
        } else {
            let (word, boundary) = Self.split(edit)
            session.commit(word, boundary: boundary, on: surface)
        }
        return edit.handled
    }

    /// A committed English edit ends with the boundary the user typed, except Enter and
    /// Tab (`handled == false`), which keep their native action and are not in the text.
    private static func split(_ edit: EnglishComposition.Edit) -> (word: String, boundary: String) {
        guard edit.handled, let last = edit.text.unicodeScalars.last else { return (edit.text, "") }
        let word = String(String.UnicodeScalarView(edit.text.unicodeScalars.dropLast()))
        return (word, String(last))
    }
}
