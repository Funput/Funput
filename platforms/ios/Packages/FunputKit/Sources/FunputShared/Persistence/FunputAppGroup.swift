/// Shared App Group identity used to exchange configuration between the
/// containing app and the keyboard extension.
public enum FunputAppGroup {
    /// Must match the `com.apple.security.application-groups` entitlement on
    /// both the app and the keyboard extension targets.
    public static let identifier = "group.app.funput.funput"

    /// Defaults key under which the encoded ``FunputConfiguration`` is stored.
    public static let configurationKey = "configuration"

    /// Short-lived configuration used only by the containing app's UI-test
    /// typing harness. Kept separate so automation never overwrites settings.
    public static let uiTestConfigurationOverrideKey = "ui-test-configuration-override"

    /// Defaults key for the keyboard's most recently used emoji.
    public static let emojiRecentsKey = "emoji-recents"

    /// Set by the keyboard after iOS confirms that Full Access is active.
    public static let observedFullAccessKey = "observed-full-access"

    /// Encoded custom keyboard themes shared with the extension.
    public static let customThemesKey = "custom-themes"
}
