import CoreGraphics

/// Metrics of Apple's stock Vietnamese keyboard in portrait on iPhone.
///
/// Measured from iOS 27 simulator screenshots of the Telex and VNI keyboards on 390pt,
/// 402pt, 420pt and 440pt wide phones. Gaps and padding were identical on all of them; row
/// heights step down below 414pt.
public enum SystemKeyMetrics {
    public static let horizontalPadding: CGFloat = 6.5
    public static let horizontalGap: CGFloat = 6
    public static let verticalGap: CGFloat = 11

    /// The toolbar band, kept equal to Funput's own (34pt band, 4pt gap).
    ///
    /// Apple's prediction bar is 36pt with an 8pt gap, but Funput's band often carries only
    /// the clipboard key, so the stock size left a tall empty strip above the keys and made
    /// the same toolbar look taller under system sizing than under Funput sizing.
    public static let toolbarHeight: CGFloat = 34
    public static let toolbarGap: CGFloat = 4

    /// VNI's digit row is drawn shorter than the letter rows beneath it: 35.7 over 42pt
    /// on wide phones, 34 over 40.3pt on narrow ones — the same proportion on both.
    public static let numberRowHeightRatio: CGFloat = 0.85

    /// Phones at least this wide get the taller rows.
    static let wideScreenWidth: CGFloat = 414

    /// Rows on a stock page without digits stacked above it: three letter rows and the
    /// action row, or the "123" page's four. Only a row beyond these — the digits over a
    /// VNI letters page — is drawn short; on a four-row page the digits are a full row.
    static let standardRowCount = 4

    /// The space below the bottom row. On 420pt and 440pt phones Apple's bottom row ends
    /// 7pt above the globe/dictation bar. On 390pt and 402pt phones it runs 4pt *into*
    /// the area that bar takes from a third-party keyboard, which no extension can draw
    /// into; ending the rows flush with the view is as close as a custom keyboard gets.
    public static func bottomPadding(screenWidth: CGFloat) -> CGFloat {
        screenWidth >= wideScreenWidth ? 6 : 0
    }

    /// The combined height of a page's key rows, excluding gaps.
    ///
    /// It depends only on how many rows the page has, never on which of them is the
    /// number row: a compact Telex letters page and its "123" page are both four rows,
    /// but only the second carries digits, and the keyboard must not change height when
    /// switching between them. Apple draws those digits as tall as the rows below them,
    /// so both pages have identical rows.
    ///
    /// Four rows is the stock Telex keyboard (45/43pt each). Five is the stock VNI keyboard,
    /// whose letter rows shrink (42/40.3pt) so the digits cost less than a whole row.
    public static func rowsHeight(screenWidth: CGFloat, rowCount: Int) -> CGFloat {
        let isWide = screenWidth >= wideScreenWidth
        guard rowCount > standardRowCount else {
            return (isWide ? 45 : 43) * CGFloat(rowCount)
        }
        let letterRow: CGFloat = isWide ? 42 : 40.3
        return letterRow * (CGFloat(rowCount - 1) + numberRowHeightRatio)
    }
}
