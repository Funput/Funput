#if os(iOS) && canImport(FunputCore)
import FunputEngine

extension KeyboardInputCoordinator {
    /// The state the suggestion engine must mirror: it keeps its own copy of the
    /// language and tone style, apart from the composer's. Hosts follow it through
    /// ``onSeedLanguageChange``, which hands over the current value and every change.
    public var seedLanguage: SuggestionSeedLanguage {
        guard state.usesVietnameseComposition, let options = compositionOptions else { return .off }
        return .vietnamese(options.toneStyle)
    }

    /// The one writer of the composer's Vietnamese switch, and so the one place the
    /// suggestion data learns that the language or the tone style changed: `apply`
    /// (settings), `toggleLanguage` and `updateContext` (fields that do not compose)
    /// all end here.
    func syncComposerLanguage() {
        composer.setEnabled(state.usesVietnameseComposition)
        reportSeedLanguage()
    }

    /// A new subscriber gets the current value at once, provided settings have been
    /// applied; before that there is nothing to say yet, and the first `apply` reports.
    func handOverSeedLanguage() {
        reportedSeedLanguage = nil
        if compositionOptions != nil { reportSeedLanguage() }
    }

    /// Reports only a change.
    private func reportSeedLanguage() {
        let language = seedLanguage
        guard language != reportedSeedLanguage else { return }
        reportedSeedLanguage = language
        onSeedLanguageChange?(language)
    }
}
#endif
