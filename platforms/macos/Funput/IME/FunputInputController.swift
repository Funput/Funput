import AppKit
import InputMethodKit

/// Bridges macOS key events to `funput-engine` via marked (underlined) text.
///
/// Each text client gets its own controller + `FunputComposer`. The composing word
/// is shown as marked text; it commits on a word boundary, Enter/Tab, focus change,
/// or navigation key. Settings (method, Vietnamese on/off) are read live from
/// `AppSettings.shared` — same process, so changes apply to the next keystroke.
@objc(FunputInputController)
final class FunputInputController: IMKInputController {
    let composer = FunputComposer()
    /// Last `AppSettings.shortcutsRevision` pushed to the engine. `-1` forces a sync on
    /// the first `syncSettings()` so the engine starts with the saved table.
    var lastSyncedShortcutsRevision = -1

    private enum KeyCode {
        static let backspace: UInt16 = 51
    }

    static let notFound = NSRange(location: NSNotFound, length: 0)

    override init!(server: IMKServer!, delegate: Any!, client inputClient: Any!) {
        super.init(server: server, delegate: delegate, client: inputClient)
        syncSettings()
    }

    override func recognizedEvents(_ sender: Any!) -> Int {
        Int(NSEvent.EventTypeMask.keyDown.rawValue)
    }

    // MARK: - Event entry point

    override func handle(_ event: NSEvent!, client sender: Any!) -> Bool {
        guard let event, let client = sender as? IMKTextInput else { return false }
        guard event.type == .keyDown else { return false }

        syncSettings()

        if AppSettings.shared.toggleShortcut.matches(event) {
            toggleEnabled()
            return true
        }

        // English mode: pass everything straight through to the app. The VI/EN state
        // is set per-app on focus change (see `activateServer`) and can be toggled.
        guard AppSettings.shared.vietnameseEnabled else { return false }

        // Flip the word being composed between Vietnamese and raw keys. Handled in
        // Vietnamese mode, before the Control-combo passthrough, so the hotkey isn't
        // swallowed in English mode (e.g. ⌃⇧Z stays Redo there).
        if matchesFlipShortcut(event) {
            flipComposing(client)
            return true
        }

        // Keyboard shortcuts (⌘C, ⌃A, …) are not text: end composition and let
        // the app handle them. Control/Command combos carry control characters in
        // `event.characters`, which would otherwise be fed to the composer.
        if !event.modifierFlags.isDisjoint(with: [.command, .control]) {
            commit(into: client)
            return false
        }

        if event.keyCode == KeyCode.backspace {
            // Nothing composing: the character about to disappear is a committed one, and
            // its removal may leave the caret at the end of a finished word to re-open.
            guard !composer.buffer().isEmpty else { return reopenPreviousWord(client, event: event) }
            composer.backspace()
            setMarked(composer.buffer(), client)
            return true
        }

        guard let scalar = event.characters?.unicodeScalars.first else {
            commit(into: client) // dead keys with no character end composition
            return false
        }

        // Navigation / function / editing keys carry a character (arrows live in the
        // function-key private-use area, Esc is U+001B) but are not text. End the
        // composition and let the app move the cursor / dismiss / delete forward.
        if isNonTextKey(event, scalar) {
            commit(into: client)
            return false
        }

        // Numeric-keypad digits are literal numbers, never VNI tone/shape modifiers:
        // commit the current word and insert the digit, like a word boundary.
        if isNumpadDigit(event, scalar) {
            return commitBoundary(scalar, into: client, source: .numpad)
        }

        if InputEventPolicy.isBoundary(scalar, method: AppSettings.shared.inputMethod) {
            return commitBoundary(scalar, into: client)
        }

        composer.process(scalar)
        // Nothing composing after this key → the engine passed it through (e.g. a
        // digit starting a word: a number, not Vietnamese). Let the app insert it.
        if composer.buffer().isEmpty {
            return false
        }
        setMarked(composer.buffer(), client)
        return true
    }

    override func commitComposition(_ sender: Any!) {
        if let client = sender as? IMKTextInput { commit(into: client) }
    }

    /// Focus moved into this client. Apply the per-app resolution: a manual VI/EN
    /// choice pinned for this app wins; otherwise apps on the exclusion list switch
    /// to English and every other app to Vietnamese. This updates `vietnameseEnabled`
    /// (so the menu bar reflects it immediately); a manual toggle stays pinned for
    /// the app it was made in (see `AppSettings.resolveVietnamese`).
    override func activateServer(_ sender: Any!) {
        super.activateServer(sender)
        applyPerAppDefault()
        syncSettings()
        // Focus on a field is the start of input: arm so the first letter is capitalized.
        if AppSettings.shared.autoCapitalizeEnabled { composer.armCapitalization() }
    }

    override func deactivateServer(_ sender: Any!) {
        if let client = sender as? IMKTextInput { commit(into: client) }
        super.deactivateServer(sender)
    }
}
