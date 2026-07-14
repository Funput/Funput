/// Preset directions supported by the two-stop keyboard background gradient.
public enum ThemeGradientDirection: String, Codable, Hashable, Sendable, CaseIterable {
    case horizontal
    case vertical
    case diagonalRight
    case diagonalLeft

    public static let `default` = ThemeGradientDirection.diagonalRight
}
