#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout

@testable import KeyboardInput

/// A coordinator with a gõ tắt table loaded, typed into a plain document the way a
/// host would: letters as characters, the space bar as `.space`.
@MainActor
struct ShortcutUndoFixture {
    let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
    let document = TestKeyboardWriter()

    init(
        language: KeyboardLanguage = .vietnamese,
        shortcuts: [String: String] = ["vn": "Việt Nam"],
        eagerRestore: Bool = true,
        typoCorrection: Bool = true
    ) {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .telex
        configuration.language = language
        configuration.autoCapitalize = false
        configuration.spellCheck = false
        configuration.eagerRestore = eagerRestore
        configuration.typoCorrection = typoCorrection
        coordinator.apply(configuration)
        coordinator.updateContext(
            .init(editorMode: .text, enterAction: .newLine, autocapitalization: .none)
        )
        coordinator.beginShortcutActivation()
        coordinator.receiveShortcuts(ShortcutLibrary(
            entries: shortcuts.map { TextShortcut(trigger: $0.key, expansion: $0.value) }
        ))
    }

    var text: String { document.text }
    var preview: CorrectionPreview? { coordinator.correctionPreview }

    func type(_ keys: String) {
        for character in keys {
            let key = character == " "
                ? testKey(.space)
                : testKey(.character, label: String(character))
            coordinator.handle(key, writer: document)
        }
    }

    func backspace(_ times: Int = 1) {
        for _ in 0..<times { coordinator.handle(testKey(.backspace), writer: document) }
    }
}
#endif
