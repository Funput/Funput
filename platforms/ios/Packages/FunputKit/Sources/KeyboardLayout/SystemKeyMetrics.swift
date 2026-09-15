import CoreGraphics

/// Metrics of Apple's stock Vietnamese keyboard in portrait on iPhone.
///
/// Measured from iOS 27 simulator screenshots of the Telex and VNI keyboards on 402pt,
/// 420pt and 440pt wide phones. Gaps and padding were identical on all three; row heights
/// step down on the narrower phone. Narrower phones than 402pt were not measured and use
/// the narrow values.
public enum SystemKeyMetrics {
    public static let horizontalPadding: CGFloat = 6.5
    public static let horizontalGap: CGFloat = 6
    public static let verticalGap: CGFloat = 11

    /// VNI's digit row is drawn shorter than the letter rows beneath it: 35.7 over 42pt
    /// on wide phones, 34 over 40.3pt on narrow ones — the same proportion on both.
    public static let numberRowHeightRatio: CGFloat = 0.85

    /// Phones at least this wide get the taller rows.
    static let wideScreenWidth: CGFloat = 414

    /// The height of a letter row. A layout with a number row gets shorter letter rows,
    /// as the stock VNI keyboard does, so the keyboard does not grow by a whole row.
    public static func letterRowHeight(screenWidth: CGFloat, hasNumberRow: Bool) -> CGFloat {
        switch (screenWidth >= wideScreenWidth, hasNumberRow) {
        case (true, false): 45
        case (true, true): 42
        case (false, false): 43
        case (false, true): 40.3
        }
    }
}
