#if os(iOS) && canImport(FunputCore)
import FunputEngine
import FunputShared
import KeyboardLayout

public extension KeyboardInputCoordinator {
    /// Applies durable user preferences from shared configuration to the engine
    /// and input state.
    ///
    /// Handles preferences only. Per-field traits (editor mode, layout page,
    /// autocapitalization) continue to come from ``updateContext(_:)``; the
    /// language chosen here can still be toggled at runtime afterward.
    func apply(_ configuration: FunputConfiguration) {
        clearComposition()
        if configuration.inputMethod.isTelexFamily {
            preferredTelexMethod = configuration.inputMethod
        }
        let options = FunputCompositionOptions(
            inputMethod: configuration.inputMethod.engineMethod,
            toneStyle: configuration.toneStyle.engineToneStyle,
            smartRestore: configuration.smartRestore,
            eagerRestore: configuration.eagerRestore,
            spellCheck: configuration.spellCheck
        )
        composer.configure(options)
        compositionOptions = options
        shiftController.resetTapSequence()
        spaceTapTracker.reset()
        smartGesturesEnabled = configuration.smartGesturesEnabled
        documentSynchronizer.invalidate()
        personalSuggestionsEnabled = configuration.personalSuggestionsEnabled
        resetPersonalSuggestionTracking()
        replaceState(inputMethod: configuration.inputMethod, language: configuration.language)
        composer.setEnabled(state.usesVietnameseComposition)
    }
}

extension ToneStyleOption {
    var engineToneStyle: FunputToneStyle {
        switch self {
        case .traditional: .traditional
        case .modern: .modern
        }
    }
}
#endif
