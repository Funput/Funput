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
    public var showsKeyPreviews: Bool
    public var heightScale: Double
    public var schemaVersion: Int

    enum CodingKeys: String, CodingKey {
        case inputMethod, language, toneStyle, spellCheck, smartRestore
        case eagerRestore, autoCapitalize, selectedThemeID
        case isHapticFeedbackEnabled, showsKeyPreviews, heightScale, schemaVersion
    }

    public init(
        inputMethod: KeyboardInputMethod = .vni,
        language: KeyboardLanguage = .vietnamese,
        toneStyle: ToneStyleOption = .traditional,
        spellCheck: Bool = false,
        smartRestore: Bool = true,
        eagerRestore: Bool = true,
        autoCapitalize: Bool = false,
        selectedThemeID: String = FunputConfiguration.defaultThemeID,
        isHapticFeedbackEnabled: Bool = false,
        showsKeyPreviews: Bool = true,
        heightScale: Double = 1,
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
        self.showsKeyPreviews = showsKeyPreviews
        self.heightScale = heightScale
        self.schemaVersion = schemaVersion
    }

    /// Identifier of the bundled default theme. Must equal the default bundled
    /// theme's `id`; a cross-module test guards that equality.
    public static let defaultThemeID = "app.funput.theme.glass"

    /// Schema version emitted by this build. Bump when the stored shape changes.
    public static let currentSchemaVersion = 2
}
