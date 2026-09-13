import Foundation
import FunputShared
import KeyboardInput
import KeyboardLayout

@MainActor
final class ShortcutTypingRig {
    let coordinator: KeyboardInputCoordinator
    let writer = ShortcutDocument()

    init(language: KeyboardLanguage = .english, method: KeyboardInputMethod = .vni,
         library: ShortcutLibrary = .init(entries: [.init(trigger: "vn", expansion: "việt nam")])) {
        coordinator = KeyboardInputCoordinator(inputMethod: method)
        var config = FunputConfiguration.default
        config.inputMethod = method
        config.language = language
        config.autoCapitalize = false
        config.spellCheck = false
        coordinator.apply(config)
        coordinator.updateContext(.init(editorMode: .text, enterAction: .newLine, autocapitalization: .none))
        coordinator.beginShortcutActivation()
        coordinator.receiveShortcuts(library)
    }

    func type(_ text: String) {
        for ch in text {
            let role: KeyRole = ch == " " ? .space : ch == "\n" ? .enter : .character
            key(String(ch), role: role)
        }
    }

    func key(_ label: String, role: KeyRole) {
        coordinator.handle(.init(id: "test.\(label)", label: label, role: role), writer: writer)
    }
}

@MainActor
final class ShortcutDocument: KeyboardDocumentWriting {
    var text = ""
    var hasSelection = false
    var identifier = UUID()
    var reportedContext: String?
    var deletions = 0
    var snapshot: KeyboardDocumentSnapshot {
        .init(documentIdentifier: identifier, contextBeforeInput: reportedContext ?? text, hasSelection: hasSelection)
    }
    func apply(_ transaction: InputTransaction) {
        for mutation in transaction.mutations {
            switch mutation {
            case let .insert(value): text += value
            case let .deleteBackward(count):
                deletions += count
                text = String(text.dropLast(count))
            case .moveCursor: break
            }
        }
    }
}
