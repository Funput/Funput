/// How heavy a theme draws its key labels.
///
/// Four named steps rather than a raw `UIFont.Weight`, matching what the Android editor offers,
/// so a theme reads the same on both platforms. The renderer shifts each kind of label from the
/// weight it already uses, so ``regular`` draws exactly what the keyboard has always drawn.
public enum ThemeLabelWeight: String, Codable, Hashable, Sendable, CaseIterable {
    case light
    case regular
    case medium
    case bold

    public static let `default` = ThemeLabelWeight.regular
}
