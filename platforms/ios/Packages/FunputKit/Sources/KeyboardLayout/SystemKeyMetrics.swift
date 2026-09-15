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

    /// The combined height of a page's key rows, excluding gaps.
    ///
    /// It depends only on how many rows the page has, never on which of them is the
    /// number row: a compact Telex letters page and its "123" page are both four rows,
    /// but only the second carries digits, and the keyboard must not change height when
    /// switching between them. Within the budget, the geometry still draws a number row
    /// shorter than its neighbours.
    ///
    /// Four rows is the stock Telex keyboard (45/43pt each). Five is the stock VNI keyboard,
    /// whose letter rows shrink (42/40.3pt) so the digits cost less than a whole row.
    public static func rowsHeight(screenWidth: CGFloat, rowCount: Int) -> CGFloat {
        let isWide = screenWidth >= wideScreenWidth
        guard rowCount >= 5 else {
            return (isWide ? 45 : 43) * CGFloat(rowCount)
        }
        let letterRow: CGFloat = isWide ? 42 : 40.3
        return letterRow * (CGFloat(rowCount - 1) + numberRowHeightRatio)
    }
}
