#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

extension KeyboardInputCoordinator {
    @discardableResult
    public func handle(
        _ key: KeySpec,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        let signpost = KeyboardInputSignposts.begin("CoordinatorHandle")
        defer { KeyboardInputSignposts.end("CoordinatorHandle", signpost) }
        synchronizeBeforeInput(writer)
        if key.role.mutatesDocument {
            return commit(
                writer: writer,
                closesEpoch: closesCompositionEpoch(for: key),
                preservesOneShotShift: key.role != .character
            ) { builder in
                applyDocumentKey(key, builder: &builder)
            }
        }
        let previous = state
        switch key.role {
        case .shift: toggleShift()
        case .inputMethod: toggleInputMethod()
        case .symbols: updateLayoutMode(.symbolsPrimary)
        case .moreSymbols: updateLayoutMode(.symbolsSecondary)
        case .letters: updateLayoutMode(.letters)
        default: break
        }
        let changed = state != previous
        let changesSuggestions = changed && key.role != .shift
        return .init(
            presentationChanged: changed,
            suggestionsChanged: changesSuggestions
        )
    }

    @discardableResult
    public func handleAlternate(
        _ alternate: KeyAlternate,
        from key: KeySpec,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        guard key.role == .character else { return .none }
        return commit(writer: writer, preservesOneShotShift: false) { builder in
            input(
                alternate.text(for: state.shiftState),
                builder: &builder
            )
            consumeOneShotShift()
        }
    }

    private func applyDocumentKey(
        _ key: KeySpec,
        builder: inout InputTransactionBuilder
    ) {
        switch key.role {
        case .character:
            input(characterText(for: key), builder: &builder)
            consumeOneShotShift()
        case .vniModifier, .punctuation:
            input(key.label, builder: &builder)
        case .space: input(" ", builder: &builder)
        case .enter: input("\n", builder: &builder)
        case .backspace:
            performDeleteBackward(builder: &builder)
            reopenPreviousWord()
        default: break
        }
    }

    private func closesCompositionEpoch(for key: KeySpec) -> Bool {
        if state.inputMethod == .telexAdvanced,
           key.role == .punctuation,
           (key.label == "[" || key.label == "]") {
            return false
        }
        return switch key.role {
        case .space, .punctuation, .enter: true
        default: false
        }
    }
}

private extension KeyRole {
    var mutatesDocument: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space, .enter, .backspace:
            true
        default:
            false
        }
    }
}
#endif
