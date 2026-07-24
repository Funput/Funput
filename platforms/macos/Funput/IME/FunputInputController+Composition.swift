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

    func commit(into client: IMKTextInput) {
        let text = composer.buffer()
        if !text.isEmpty {
            client.insertText(text, replacementRange: Self.notFound)
        }
        composer.clear()
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

        if scalar == "\n" || scalar == "\r" || scalar == "\t" {
            client.insertText(word, replacementRange: Self.notFound)
            return false
        }
        client.insertText(word + String(scalar), replacementRange: Self.notFound)
        return true
    }

    func flipComposing(_ client: IMKTextInput) {
        guard composer.flipComposing().action != ACTION_NONE else { return }
        setMarked(composer.buffer(), client)
    }

    func toggleEnabled() {
        let settings = AppSettings.shared
        // The hotkey fires while the target app is focused, so the choice pins to
        // that app — the per-app default won't revert it on the next focus change.
        settings.pinVietnamese(
            !settings.vietnameseEnabled,
            to: NSWorkspace.shared.frontmostApplication?.bundleIdentifier
        )
        composer.setEnabled(settings.vietnameseEnabled)
    }

    func matchesFlipShortcut(_ event: NSEvent) -> Bool {
        AppSettings.shared.flipShortcut?.matches(event) ?? false
    }
}
