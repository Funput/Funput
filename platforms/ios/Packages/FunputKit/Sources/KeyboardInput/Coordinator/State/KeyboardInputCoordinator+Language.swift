#if os(iOS) && canImport(FunputCore)
import FunputEngine

extension KeyboardInputCoordinator {
    /// The state the suggestion engine must mirror. The suggestion engine keeps its
    /// own copy of the language and tone style, apart from the composer's, so the host
    /// reads this at activation and follows ``onSeedLanguageChange`` after that.
    public var seedLanguage: SuggestionSeedLanguage {
        guard state.usesVietnameseComposition, let options = compositionOptions else { return .off }
        return .vietnamese(options.toneStyle)
    }

    /// The one writer of the composer's Vietnamese switch, and so the one place the
    /// suggestion data learns that the language or the tone style changed: `apply`
    /// (settings), `toggleLanguage` and `updateContext` (fields that do not compose)
    /// all end here. Reports only a change.
    func syncComposerLanguage() {
        composer.setEnabled(state.usesVietnameseComposition)
        let language = seedLanguage
        guard language != reportedSeedLanguage else { return }
        reportedSeedLanguage = language
        onSeedLanguageChange?(language)
    }
}
#endif
