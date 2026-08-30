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
    let shortcutSmartCase: Bool

    init(settings: AppSettings) {
        inputMethod = settings.inputMethod
        toneStyle = settings.toneStyle
        enabled = settings.vietnameseEnabled
        smartEnglishRestore = settings.smartEnglishRestore
        eagerRestore = settings.eagerRestore
        spellCheckEnabled = settings.spellCheckEnabled
        autoCapitalizeEnabled = settings.autoCapitalizeEnabled
        shortcutSmartCase = settings.shortcutSmartCase
    }

    init(
        inputMethod: InputMethod = .telex,
        toneStyle: ToneStyle = .traditional,
        enabled: Bool = true,
        smartEnglishRestore: Bool = true,
        eagerRestore: Bool = true,
        spellCheckEnabled: Bool = false,
        autoCapitalizeEnabled: Bool = false,
        shortcutSmartCase: Bool = true
    ) {
        self.inputMethod = inputMethod
        self.toneStyle = toneStyle
        self.enabled = enabled
        self.smartEnglishRestore = smartEnglishRestore
        self.eagerRestore = eagerRestore
        self.spellCheckEnabled = spellCheckEnabled
        self.autoCapitalizeEnabled = autoCapitalizeEnabled
        self.shortcutSmartCase = shortcutSmartCase
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
        // Neither of these rides the by-value `FunputConfig`: `enabled` is a runtime
        // toggle, and smart case is kept off the C struct for ABI stability. Order does
        // not matter — `configure` edits the engine config rather than replacing it.
        setShortcutSmartCase(configuration.shortcutSmartCase)
        setEnabled(configuration.enabled)
    }
}
