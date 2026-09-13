#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout

struct KeyboardShortcutState {
    var library = ShortcutLibrary(isEnabled: false)
    var pending: ShortcutLibrary?
    var word = ""
}

extension KeyboardInputCoordinator {
    /// Called at activation before loading. Never carries the previous table into a new session.
    public func beginShortcutActivation() {
        composer.clear()
        shortcuts = KeyboardShortcutState()
        composer.clearShortcuts()
        composer.setShortcutsEnabled(false)
    }

    public func receiveShortcuts(_ library: ShortcutLibrary) {
        shortcuts.pending = library
        if shortcuts.word.isEmpty { installPendingShortcuts() }
    }

    var usesEngine: Bool {
        state.usesVietnameseComposition || (state.editorMode.supportsVietnameseComposition
            && shortcuts.library.isEnabled && shortcuts.library.inEnglish
            && !shortcuts.library.entries.isEmpty)
    }

    func trackShortcutInput(_ scalar: Unicode.Scalar) {
        let advancedModifier = state.usesVietnameseComposition && state.inputMethod == .telexAdvanced
            && (scalar == "[" || scalar == "]")
        let boundary = scalar.properties.isWhitespace || (scalar.isASCII && !scalar.properties.isAlphabetic
            && !(48...57).contains(scalar.value) && scalar.value >= 33 && scalar.value <= 126)
        if boundary && !advancedModifier {
            finishShortcutWord()
        } else if state.editorMode.supportsVietnameseComposition {
            shortcuts.word.unicodeScalars.append(scalar)
        }
    }

    func finishShortcutWord() {
        shortcuts.word = ""
        composer.setShortcutsEnabled(shortcuts.library.isEnabled)
        installPendingShortcuts()
    }

    func abandonUnsafeReplacement() {
        composer.clear()
        composer.setShortcutsEnabled(false)
    }

    /// Every composition reset also closes the pending-library boundary.
    func clearComposition() {
        composer.clear()
        finishShortcutWord()
    }

    private func installPendingShortcuts() {
        guard let library = shortcuts.pending else { return }
        shortcuts.pending = nil
        shortcuts.library = library
        composer.clearShortcuts()
        for entry in library.entries {
            composer.addShortcut(trigger: entry.trigger, expansion: entry.expansion)
        }
        composer.setShortcutSmartCase(library.smartCase)
        composer.setShortcutsInEnglish(library.inEnglish)
        composer.setShortcutsEnabled(library.isEnabled)
    }
}
#endif
