/// Shared App Group identity used to exchange configuration between the
/// containing app and the keyboard extension.
public enum FunputAppGroup {
    /// Must match the `com.apple.security.application-groups` entitlement on
    /// both the app and the keyboard extension targets.
    public static let identifier = "group.app.funput.funput"

    /// Defaults key under which the encoded ``FunputConfiguration`` is stored.
    public static let configurationKey = "configuration"

#if DEBUG
    /// Short-lived configuration used only by the debug UI-test harness.
    public static let uiTestConfigurationOverrideKey = "ui-test-configuration-override"
    public static let touchDiagnosticSessionKey = "touch-diagnostic-session"
    public static let touchDiagnosticReportKey = "touch-diagnostic-report"
#endif

    /// Defaults key for the keyboard's most recently used emoji.
    public static let emojiRecentsKey = "emoji-recents"

    /// Set by the keyboard after iOS confirms that Full Access is active.
    public static let observedFullAccessKey = "observed-full-access"

    /// Reset command acknowledged only by the keyboard extension writer.
    public static let personalSuggestionAppliedResetKey = "personal-suggestion-applied-reset"

    /// Directory containing the private Rust snapshot and journal.
    public static let personalSuggestionsDirectory = "PersonalSuggestions"

    /// Directory holding the clipboard history file. A directory rather than a
    /// defaults key so the contents can be excluded from device backups.
    public static let clipboardDirectory = "Clipboard"

    /// Encoded custom keyboard themes shared with the extension.
    public static let customThemesKey = "custom-themes"
}
