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
            spellCheck: configuration.spellCheck,
            extraOnsets: configuration.extraOnsets.engineOnsets
        )
        composer.configure(options)
        compositionOptions = options
        shiftController.resetTapSequence()
        spaceTapTracker.reset()
        smartGesturesEnabled = configuration.smartGesturesEnabled
        returnsToLettersAfterPunctuation = configuration.returnsToLettersAfterPunctuation
        documentSynchronizer.invalidate()
        personalSuggestionsEnabled = configuration.personalSuggestionsEnabled
        resetPersonalSuggestionTracking()
        replaceState(inputMethod: configuration.inputMethod, language: configuration.language)
        composer.setTypoCorrection(configuration.typoCorrection)
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

extension ExtraOnsetLetters {
    /// Shared storage bits and C ABI bits are independent; map letters explicitly.
    var engineOnsets: FunputExtraOnsets {
        var onsets: FunputExtraOnsets = []
        if contains(.z) { onsets.insert(.z) }
        if contains(.f) { onsets.insert(.f) }
        if contains(.w) { onsets.insert(.w) }
        if contains(.j) { onsets.insert(.j) }
        return onsets
    }
}
#endif
