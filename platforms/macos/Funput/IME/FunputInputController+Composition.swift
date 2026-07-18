import AppKit
import InputMethodKit

extension FunputInputController {
    func applyPerAppDefault() {
        let settings = AppSettings.shared
        guard !settings.excludedApps.isEmpty else { return }
        let front = NSWorkspace.shared.frontmostApplication?.bundleIdentifier
        let vietnamese = !settings.isExcluded(front)
        guard settings.vietnameseEnabled != vietnamese else { return }
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

    func commitBoundary(_ scalar: Unicode.Scalar, into client: IMKTextInput) -> Bool {
        let pre = composer.buffer()
        guard !pre.isEmpty else {
            if AppSettings.shared.autoCapitalizeEnabled { _ = composer.process(scalar) }
            return false
        }

        let result = composer.process(scalar)
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
        settings.vietnameseEnabled.toggle()
        composer.setEnabled(settings.vietnameseEnabled)
    }

    func matchesFlipShortcut(_ event: NSEvent) -> Bool {
        AppSettings.shared.flipShortcut?.matches(event) ?? false
    }
}
