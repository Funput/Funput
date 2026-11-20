public extension FunputConfiguration {
    /// The height settings the keyboard can lay out. The settings screen offers nothing
    /// outside it, but stored configuration is decoded as written, and a height of zero
    /// would leave the user with no keyboard at all.
    static let heightScaleRange: ClosedRange<Double> = 0.85...1.2

    /// `heightScale` held inside ``heightScaleRange``; a value that is not a number
    /// falls back to the default height.
    var clampedHeightScale: Double {
        guard heightScale.isFinite else { return 1 }
        return min(max(heightScale, Self.heightScaleRange.lowerBound), Self.heightScaleRange.upperBound)
    }
}
