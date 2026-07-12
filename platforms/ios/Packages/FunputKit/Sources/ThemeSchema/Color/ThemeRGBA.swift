/// A resolved, non-adaptive sRGB color with components in the `0...1` range.
///
/// `ThemeRGBA` is a plain value with no platform (`UIColor`) dependency, so the
/// schema stays usable and testable on any host. Conversion to a `UIColor`
/// happens in the renderer, not here.
public struct ThemeRGBA: Hashable, Sendable, Codable {
    public let red: Double
    public let green: Double
    public let blue: Double
    public let alpha: Double

    public init(red: Double, green: Double, blue: Double, alpha: Double = 1) {
        self.red = red
        self.green = green
        self.blue = blue
        self.alpha = alpha
    }

    /// Creates a color from a packed 24-bit `0xRRGGBB` value.
    public init(hex: UInt32, alpha: Double = 1) {
        red = Double((hex >> 16) & 0xFF) / 255
        green = Double((hex >> 8) & 0xFF) / 255
        blue = Double(hex & 0xFF) / 255
        self.alpha = alpha
    }
}
