#if os(iOS) && canImport(FunputCore)
import FunputCore
import FunputEngine
import KeyboardInput

extension PersonalSuggestionEngine {
    /// Says what the shipped Vietnamese pairs may predict. The handle keeps its own copy
    /// of the language and tone style, apart from the composer's; see
    /// `KeyboardInputCoordinator.syncComposerLanguage`. False only for a call the
    /// native boundary abandoned.
    @discardableResult
    public func setSeedLanguage(_ language: SuggestionSeedLanguage) -> Bool {
        switch language {
        case .off:
            // The tone byte is ignored while off; any valid one will do.
            funput_suggestion_set_seed_language(
                handle, UInt8(FUNPUT_SEED_LANGUAGE_OFF), FunputToneStyle.modern.rawValue
            )
        case let .vietnamese(style):
            funput_suggestion_set_seed_language(
                handle, UInt8(FUNPUT_SEED_LANGUAGE_VIETNAMESE), style.rawValue
            )
        }
    }
}
#endif
