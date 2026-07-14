/// Safe ranges for authored metrics.
///
/// Resolution clamps every value so a malformed or hostile theme can never
/// render an unusable keyboard (invisible keys, absurd radii, and so on).
enum MetricClamp {
    static func unit(_ value: Double) -> Double { clamp(value, 0, 1) }
    static func cornerRadius(_ value: Double) -> Double { clamp(value, 0, 20) }
    static func borderWidth(_ value: Double) -> Double { clamp(value, 0, 4) }
    static func shadowRadius(_ value: Double) -> Double { clamp(value, 0, 24) }
    static func pressedScale(_ value: Double) -> Double { clamp(value, 0.8, 1) }
    static func pressedOpacityMultiplier(_ value: Double) -> Double { clamp(value, 1, 1.5) }
    static func fontScale(_ value: Double) -> Double { clamp(value, 0.8, 1.4) }
    static func keycapHeight(_ value: Double) -> Double { clamp(value, 0.82, 1) }
    static func horizontalPadding(_ value: Double) -> Double { clamp(value, 2, 16) }
    static func horizontalGap(_ value: Double) -> Double { clamp(value, 2, 10) }
    static func verticalGap(_ value: Double) -> Double { clamp(value, 3, 12) }

    private static func clamp(_ value: Double, _ lower: Double, _ upper: Double) -> Double {
        min(max(value, lower), upper)
    }
}
