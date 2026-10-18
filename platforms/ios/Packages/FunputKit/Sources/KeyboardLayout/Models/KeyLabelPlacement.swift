/// Where a key's label sits on its keycap.
///
/// The iPad keyboard writes its function keys out in words tucked into a bottom corner —
/// `tab`, `caps lock` and `shift` on the left, `delete`, `return` and the right `shift` on
/// the right. A key placed in a corner is drawn as that text, never as an icon, except the
/// symbol-only keys (globe, emoji, hide keyboard), whose symbol is anchored in that corner.
/// Its letter keys sit on the same bottom line, centered, with the hint printed above them.
public enum KeyLabelPlacement: String, Hashable, Sendable {
    case center
    case bottomLeading
    case bottomTrailing
    case bottomCenter

    /// Whether the key is drawn as a word in a bottom corner.
    public var isCorner: Bool { self == .bottomLeading || self == .bottomTrailing }
}
