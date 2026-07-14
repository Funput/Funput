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
        composer.clear()
        composer.setInputMethod(configuration.inputMethod.engineMethod)
        composer.setToneStyle(configuration.toneStyle.engineToneStyle)
        composer.setSpellCheck(configuration.spellCheck)
        composer.setSmartRestore(configuration.smartRestore)
        composer.setEagerRestore(configuration.eagerRestore)
        composer.setAutoCapitalize(configuration.autoCapitalize)
        shiftController.resetTapSequence()
        documentSynchronizer.invalidate()
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
