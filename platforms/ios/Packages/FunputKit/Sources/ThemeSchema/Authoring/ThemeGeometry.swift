/// Persistable keyboard geometry authored by bundled and custom themes.
public struct ThemeGeometry: Codable, Hashable, Sendable {
    public var keycapHeightScale: Double
    public var horizontalPadding: Double
    public var horizontalGap: Double
    public var verticalGap: Double

    public init(
        keycapHeightScale: Double = 1,
        horizontalPadding: Double = 6,
        horizontalGap: Double = 5,
        verticalGap: Double = 7
    ) {
        self.keycapHeightScale = keycapHeightScale
        self.horizontalPadding = horizontalPadding
        self.horizontalGap = horizontalGap
        self.verticalGap = verticalGap
    }

    public static let `default` = ThemeGeometry()
}
