/// Shared App Group identity used to exchange configuration between the
/// containing app and the keyboard extension.
public enum FunputAppGroup {
    /// Must match the `com.apple.security.application-groups` entitlement on
    /// both the app and the keyboard extension targets.
    public static let identifier = "group.app.funput.funput"

    /// Defaults key under which the encoded ``FunputConfiguration`` is stored.
    public static let configurationKey = "configuration"

    /// Defaults key for the keyboard's most recently used emoji.
    public static let emojiRecentsKey = "emoji-recents"
}
