/// Where a key's label sits on its keycap.
///
/// The iPad keyboard writes its function keys out in words tucked into a bottom corner —
/// `tab`, `caps lock` and `shift` on the left, `delete`, `return` and the right `shift` on
/// the right. A key placed in a corner is drawn as that text, never as an icon.
public enum KeyLabelPlacement: String, Hashable, Sendable {
    case center
    case bottomLeading
    case bottomTrailing

    public var isCorner: Bool { self != .center }
}
