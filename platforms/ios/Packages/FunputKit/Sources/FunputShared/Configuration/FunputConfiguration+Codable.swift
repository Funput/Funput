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
        config.showsNumberRow = try container.decodeIfPresent(Bool.self, forKey: .showsNumberRow) ?? config.showsNumberRow
        config.showsGlobeKey = try container.decodeIfPresent(Bool.self, forKey: .showsGlobeKey) ?? config.showsGlobeKey
        config.heightScale = try container.decodeIfPresent(Double.self, forKey: .heightScale) ?? config.heightScale
        config.personalSuggestionsEnabled = try container.decodeIfPresent(Bool.self, forKey: .personalSuggestionsEnabled) ?? config.personalSuggestionsEnabled
        config.personalSuggestionResetToken = try container.decodeIfPresent(UUID.self, forKey: .personalSuggestionResetToken)
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
            config.showsGlobeKey = false
            config.schemaVersion = 5
        }
        if config.schemaVersion < 6 {
            config.schemaVersion = 6
        }
        self = config
    }
}
