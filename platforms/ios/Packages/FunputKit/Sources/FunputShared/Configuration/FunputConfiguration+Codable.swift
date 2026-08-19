import Foundation
import KeyboardLayout

extension FunputConfiguration {
    /// Decodes tolerantly: any missing key keeps its default so adding a field
    /// in a later build never discards a user's other settings.
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        var config = FunputConfiguration()
        config.inputMethod = try container.decodeIfPresent(KeyboardInputMethod.self, forKey: .inputMethod) ?? config.inputMethod
        config.language = try container.decodeIfPresent(KeyboardLanguage.self, forKey: .language) ?? config.language
        config.toneStyle = try container.decodeIfPresent(ToneStyleOption.self, forKey: .toneStyle) ?? config.toneStyle
        config.spellCheck = try container.decodeIfPresent(Bool.self, forKey: .spellCheck) ?? config.spellCheck
        config.smartRestore = try container.decodeIfPresent(Bool.self, forKey: .smartRestore) ?? config.smartRestore
        config.eagerRestore = try container.decodeIfPresent(Bool.self, forKey: .eagerRestore) ?? config.eagerRestore
        config.autoCapitalize = try container.decodeIfPresent(Bool.self, forKey: .autoCapitalize) ?? config.autoCapitalize
        config.selectedThemeID = try container.decodeIfPresent(String.self, forKey: .selectedThemeID) ?? config.selectedThemeID
        config.isHapticFeedbackEnabled = try container.decodeIfPresent(Bool.self, forKey: .isHapticFeedbackEnabled) ?? config.isHapticFeedbackEnabled
        config.isKeySoundEnabled = try container.decodeIfPresent(Bool.self, forKey: .isKeySoundEnabled) ?? config.isKeySoundEnabled
        config.showsKeyPreviews = try container.decodeIfPresent(Bool.self, forKey: .showsKeyPreviews) ?? config.showsKeyPreviews
        config.smartGesturesEnabled = try container.decodeIfPresent(Bool.self, forKey: .smartGesturesEnabled) ?? config.smartGesturesEnabled
        config.showsNumberRow = try container.decodeIfPresent(Bool.self, forKey: .showsNumberRow) ?? config.showsNumberRow
        config.layoutPreset = try container.decodeIfPresent(KeyboardLayoutPreset.self, forKey: .layoutPreset) ?? config.layoutPreset
        config.heightScale = try container.decodeIfPresent(Double.self, forKey: .heightScale) ?? config.heightScale
        config.personalSuggestionsEnabled = try container.decodeIfPresent(Bool.self, forKey: .personalSuggestionsEnabled) ?? config.personalSuggestionsEnabled
        config.personalSuggestionResetToken = try container.decodeIfPresent(UUID.self, forKey: .personalSuggestionResetToken)
        config.clipboardEnabled = try container.decodeIfPresent(Bool.self, forKey: .clipboardEnabled) ?? config.clipboardEnabled
        config.clipboardExpiry = try container.decodeIfPresent(ClipboardExpiry.self, forKey: .clipboardExpiry) ?? config.clipboardExpiry
        config.schemaVersion = try container.decodeIfPresent(Int.self, forKey: .schemaVersion) ?? config.schemaVersion
        if config.schemaVersion < 2 {
            config.isHapticFeedbackEnabled = false
        }
        if config.schemaVersion < 3 {
            config.isKeySoundEnabled = false
            config.schemaVersion = 3
        }
        if config.schemaVersion < 4 {
            config.showsNumberRow = false
            config.schemaVersion = 4
        }
        if config.schemaVersion < 5 {
            config.schemaVersion = 5
        }
        if config.schemaVersion < 6 {
            config.schemaVersion = 6
        }
        if config.schemaVersion < 7 {
            config.schemaVersion = 7
        }
        // v8 dropped `showsGlobeKey`: iOS draws its own globe below a custom
        // keyboard, so Funput's copy was a duplicate that never shipped visible.
        if config.schemaVersion < 8 {
            config.schemaVersion = 8
        }
        // v9 added the clipboard settings; a payload without them takes the defaults,
        // which is exactly what `decodeIfPresent` above already did.
        if config.schemaVersion < 9 {
            config.schemaVersion = 9
        }
        // v10 added `layoutPreset`. It defaults to `.funput`, which is exactly how every
        // pre-v10 build rendered, so there is nothing to fix up — unlike the `< 4` rung,
        // which stomps a value because that field's default flipped.
        if config.schemaVersion < 10 {
            config.schemaVersion = 10
        }
        // v11 added `smartGesturesEnabled`, defaulting to on for everyone: the gestures
        // it covers are what iOS users already expect from the system keyboard.
        if config.schemaVersion < 11 {
            config.schemaVersion = 11
        }
        self = config
    }
}
