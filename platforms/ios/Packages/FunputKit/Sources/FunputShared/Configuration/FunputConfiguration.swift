import Foundation
import KeyboardLayout

/// The durable, user-facing settings shared between the containing app and the
/// keyboard extension through the App Group.
///
/// Only preferences that must outlive a single keyboard session live here.
/// Values derived from the focused text field (editor mode, layout page,
/// autocapitalization) are resolved live and are intentionally absent.
public struct FunputConfiguration: Codable, Hashable, Sendable {
    public var inputMethod: KeyboardInputMethod
    public var language: KeyboardLanguage
    public var toneStyle: ToneStyleOption
    public var spellCheck: Bool
    public var smartRestore: Bool
    public var eagerRestore: Bool
    public var autoCapitalize: Bool
    public var selectedThemeID: String
    public var isHapticFeedbackEnabled: Bool
    public var isKeySoundEnabled: Bool
    public var showsKeyPreviews: Bool
    /// Double-tap space, spacebar cursor panning and swipe-to-delete-word, as one switch.
    public var smartGesturesEnabled: Bool
    public var showsNumberRow: Bool
    public var layoutPreset: KeyboardLayoutPreset
    /// Overrides the light/dark appearance the host app would otherwise impose.
    public var keyboardAppearance: KeyboardAppearanceOption
    public var heightScale: Double
    public var personalSuggestionsEnabled: Bool
    public var clipboardEnabled: Bool
    public var clipboardExpiry: ClipboardExpiry
    public var personalSuggestionResetToken: UUID?
    public var schemaVersion: Int

    enum CodingKeys: String, CodingKey {
        case inputMethod, language, toneStyle, spellCheck, smartRestore
        case eagerRestore, autoCapitalize, selectedThemeID
        case isHapticFeedbackEnabled, isKeySoundEnabled, showsKeyPreviews
        case smartGesturesEnabled
        case showsNumberRow, layoutPreset, heightScale, keyboardAppearance
        case personalSuggestionsEnabled, personalSuggestionResetToken
        case clipboardEnabled, clipboardExpiry, schemaVersion
    }

    public init(
        inputMethod: KeyboardInputMethod = .vni,
        language: KeyboardLanguage = .vietnamese,
        toneStyle: ToneStyleOption = .modern,
        spellCheck: Bool = false,
        smartRestore: Bool = true,
        eagerRestore: Bool = true,
        autoCapitalize: Bool = true,
        selectedThemeID: String = FunputConfiguration.defaultThemeID,
        isHapticFeedbackEnabled: Bool = false,
        isKeySoundEnabled: Bool = false,
        showsKeyPreviews: Bool = true,
        smartGesturesEnabled: Bool = true,
        showsNumberRow: Bool = false,
        layoutPreset: KeyboardLayoutPreset = .funput,
        keyboardAppearance: KeyboardAppearanceOption = .system,
        heightScale: Double = 1.1,
        personalSuggestionsEnabled: Bool = true,
        clipboardEnabled: Bool = true,
        clipboardExpiry: ClipboardExpiry = .hour,
        personalSuggestionResetToken: UUID? = nil,
        schemaVersion: Int = FunputConfiguration.currentSchemaVersion
    ) {
        self.inputMethod = inputMethod
        self.language = language
        self.toneStyle = toneStyle
        self.spellCheck = spellCheck
        self.smartRestore = smartRestore
        self.eagerRestore = eagerRestore
        self.autoCapitalize = autoCapitalize
        self.selectedThemeID = selectedThemeID
        self.isHapticFeedbackEnabled = isHapticFeedbackEnabled
        self.isKeySoundEnabled = isKeySoundEnabled
        self.showsKeyPreviews = showsKeyPreviews
        self.smartGesturesEnabled = smartGesturesEnabled
        self.showsNumberRow = showsNumberRow
        self.layoutPreset = layoutPreset
        self.keyboardAppearance = keyboardAppearance
        self.heightScale = heightScale
        self.personalSuggestionsEnabled = personalSuggestionsEnabled
        self.clipboardEnabled = clipboardEnabled
        self.clipboardExpiry = clipboardExpiry
        self.personalSuggestionResetToken = personalSuggestionResetToken
        self.schemaVersion = schemaVersion
    }

    /// Identifier of the bundled default theme. Must equal the default bundled
    /// theme's `id`; a cross-module test guards that equality.
    public static let defaultThemeID = "app.funput.theme.glass"

    /// Schema version emitted by this build. Bump when the stored shape changes.
    public static let currentSchemaVersion = 12
}
