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
        configure(
            FunputConfig(
                method: configuration.inputMethod.ffiValue,
                tone_style: UInt8(configuration.toneStyle.rawValue),
                smart_restore: configuration.smartEnglishRestore,
                eager_restore: configuration.eagerRestore,
                spell_check: configuration.spellCheckEnabled,
                auto_capitalize: configuration.autoCapitalizeEnabled
            )
        )
        // `enabled` is a runtime toggle, not part of the batch config.
        setEnabled(configuration.enabled)
    }
}
