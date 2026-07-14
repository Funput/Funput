public enum ThemeBackgroundMode: String, Codable, Hashable, Sendable, CaseIterable {
    case gradient
    case image
}

public struct ThemeBackgroundImage: Codable, Hashable, Sendable {
    public var assetID: String
    public var focalX: Double
    public var focalY: Double
    public var zoom: Double
    public var blurRadius: Double

    public init(
        assetID: String,
        focalX: Double = 0.5,
        focalY: Double = 0.5,
        zoom: Double = 1,
        blurRadius: Double = 0
    ) {
        self.assetID = assetID
        self.focalX = focalX
        self.focalY = focalY
        self.zoom = zoom
        self.blurRadius = blurRadius
    }
}

public struct ThemeBackgroundEffects: Codable, Hashable, Sendable {
    public var mode: ThemeBackgroundMode
    public var image: ThemeBackgroundImage?
    public var overlay: AdaptiveThemeColor

    public init(
        mode: ThemeBackgroundMode = .gradient,
        image: ThemeBackgroundImage? = nil,
        overlay: AdaptiveThemeColor = .transparent
    ) {
        self.mode = mode
        self.image = image
        self.overlay = overlay
    }

    public static let `default` = ThemeBackgroundEffects()
}

public extension AdaptiveThemeColor {
    static let transparent = AdaptiveThemeColor(
        light: ThemeRGBA(hex: 0x000000, alpha: 0),
        dark: ThemeRGBA(hex: 0x000000, alpha: 0)
    )
}
