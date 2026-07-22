import Foundation

/// A value snapshot of every preference consumed by the input-method composer.
/// Keeping the mapping centralized makes settings synchronization atomic.
struct ComposerConfiguration: Equatable {
    let inputMethod: InputMethod
    let toneStyle: ToneStyle
    let enabled: Bool
    let smartEnglishRestore: Bool
    let eagerRestore: Bool
    let spellCheckEnabled: Bool
    let autoCapitalizeEnabled: Bool

    init(settings: AppSettings) {
        inputMethod = settings.inputMethod
        toneStyle = settings.toneStyle
        enabled = settings.vietnameseEnabled
        smartEnglishRestore = settings.smartEnglishRestore
        eagerRestore = settings.eagerRestore
        spellCheckEnabled = settings.spellCheckEnabled
        autoCapitalizeEnabled = settings.autoCapitalizeEnabled
    }

    init(
        inputMethod: InputMethod = .telex,
        toneStyle: ToneStyle = .traditional,
        enabled: Bool = true,
        smartEnglishRestore: Bool = true,
        eagerRestore: Bool = true,
        spellCheckEnabled: Bool = false,
        autoCapitalizeEnabled: Bool = false
    ) {
        self.inputMethod = inputMethod
        self.toneStyle = toneStyle
        self.enabled = enabled
        self.smartEnglishRestore = smartEnglishRestore
        self.eagerRestore = eagerRestore
        self.spellCheckEnabled = spellCheckEnabled
        self.autoCapitalizeEnabled = autoCapitalizeEnabled
    }
}

extension FunputComposer {
    func apply(_ configuration: ComposerConfiguration) {
        setMethod(configuration.inputMethod)
        setToneStyle(configuration.toneStyle)
        setEnabled(configuration.enabled)
        setSmartRestore(configuration.smartEnglishRestore)
        setEagerRestore(configuration.eagerRestore)
        setSpellCheck(configuration.spellCheckEnabled)
        setAutoCapitalize(configuration.autoCapitalizeEnabled)
    }
}
