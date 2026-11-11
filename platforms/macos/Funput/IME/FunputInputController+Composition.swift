import AppKit
import InputMethodKit

extension FunputInputController {
    func applyPerAppDefault() {
        let settings = AppSettings.shared
        let front = NSWorkspace.shared.frontmostApplication?.bundleIdentifier
        guard let vietnamese = settings.resolveVietnamese(for: front),
            settings.vietnameseEnabled != vietnamese
        else { return }
        settings.vietnameseEnabled = vietnamese
        composer.setEnabled(vietnamese)
    }

    /// Put the engine's current composition on screen. When the session had to drop the
    /// word (the text before the caret is no longer Funput's), the engine forgets it too
    /// and the result is false: the key was not handled.
    func showComposition(_ client: IMKTextInput) -> Bool {
        guard session.show(composer.buffer(), on: IMKCompositionSurface(client: client)) else {
            composer.clear()
            return false
        }
        return true
    }

    /// Show the composition after `scalar` reached the engine. A dropped word does not
    /// swallow the key: it is replayed as the first letter of a new word.
    func showKey(_ scalar: Unicode.Scalar, _ client: IMKTextInput) -> Bool {
        if showComposition(client) { return true }
        composer.process(scalar)
        return !composer.buffer().isEmpty && showComposition(client)
    }

    /// Keys for a password field that refuses marked text (`SecureInputGuard`): nil when the
    /// key is handled as usual, otherwise whether the key was handled. Only keys Funput
    /// would compose are guarded; English keys that go to the app as typed need no answer.
    func handleSecureField(_ event: NSEvent, client: IMKTextInput) -> Bool? {
        guard AppSettings.shared.vietnameseEnabled || holdsEnglishWords else { return nil }
        let key = InputEventPolicy.typedText(event.characters, modifiers: event.modifierFlags)
        let surface = IMKCompositionSurface(client: client)
        let decision = secureInput.decide(typing: key, on: surface, composing: !composer.buffer().isEmpty) {
            commit(into: client)
        }
        switch decision {
        case .compose: return nil
        case .handOver: return false
        case .typed: return true
        }
    }

    func commit(into client: IMKTextInput) {
        let text = composer.finishComposition()
        session.commit(text, boundary: "", on: IMKCompositionSurface(client: client))
    }

    func commitBoundary(
        _ scalar: Unicode.Scalar,
        into client: IMKTextInput,
        source: FunputComposer.KeySource = .standard
    ) -> Bool {
        let pre = composer.buffer()
        guard !pre.isEmpty else {
            if AppSettings.shared.autoCapitalizeEnabled { _ = composer.process(scalar, source: source) }
            return false
        }

        let result = composer.process(scalar, source: source)
        let word = result.action == ACTION_SEND
            ? String(FunputComposer.output(of: result).dropLast())
            : pre

        // Enter and Tab keep their native action: the app receives the key itself.
        let actionKey = scalar == "\n" || scalar == "\r" || scalar == "\t"
        session.commit(word, boundary: actionKey ? "" : String(scalar), on: IMKCompositionSurface(client: client))
        return !actionKey
    }

    func flipComposing(_ client: IMKTextInput) {
        guard composer.flipComposing().action != ACTION_NONE else { return }
        _ = showComposition(client)
    }

    func toggleEnabled() {
        let settings = AppSettings.shared
        // The hotkey fires while the target app is focused, so the choice pins to
        // that app — the per-app default won't revert it on the next focus change.
        settings.pinVietnamese(
            !settings.vietnameseEnabled,
            to: NSWorkspace.shared.frontmostApplication?.bundleIdentifier
        )
    }

    func matchesFlipShortcut(_ event: NSEvent) -> Bool {
        AppSettings.shared.flipShortcut?.matches(event) ?? false
    }
}
