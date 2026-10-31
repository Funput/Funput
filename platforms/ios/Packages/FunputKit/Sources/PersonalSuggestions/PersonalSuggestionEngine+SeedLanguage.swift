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
        // The tone byte is ignored while off; any valid one will do.
        let (code, tone): (Int32, FunputToneStyle) = switch language {
        case .off: (FUNPUT_SEED_LANGUAGE_OFF, .modern)
        case let .vietnamese(style): (FUNPUT_SEED_LANGUAGE_VIETNAMESE, style)
        }
        return funput_suggestion_set_seed_language(handle, UInt8(code), tone.rawValue)
    }
}
#endif
