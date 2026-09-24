import Foundation

public struct KeyboardSizingProfile: Hashable, Sendable {
    public var keySizing: KeyboardKeySizing
    public var horizontalPadding: CGFloat
    public var verticalPadding: CGFloat
    public var horizontalGap: CGFloat
    public var verticalGap: CGFloat
    /// The suggestion band, sized like Gboard's strip rather than like a key row:
    /// the toolbar carries one line of text and two icons, so anything taller is
    /// keyboard height spent on padding. The default leaves 6pt above and below a
    /// 17pt candidate with its diacritics.
    public var toolbarHeight: CGFloat
    public var toolbarGap: CGFloat
    public var heightScale: CGFloat
    public var labelScale: CGFloat
    /// A number row's height relative to the other rows.
    public var numberRowHeightRatio: CGFloat

    public init(
        keySizing: KeyboardKeySizing = .funput,
        horizontalPadding: CGFloat = 6,
        verticalPadding: CGFloat = 6,
        horizontalGap: CGFloat = 5,
        verticalGap: CGFloat = 7,
        toolbarHeight: CGFloat = 34,
        toolbarGap: CGFloat = 4,
        heightScale: CGFloat = 1,
        labelScale: CGFloat = 1,
        numberRowHeightRatio: CGFloat = 1
    ) {
        precondition(horizontalPadding >= 0)
        precondition(verticalPadding >= 0)
        precondition(horizontalGap >= 0)
        precondition(verticalGap >= 0)
        precondition(toolbarHeight > 0)
        precondition(toolbarGap >= 0)
        precondition(heightScale > 0)
        precondition(labelScale > 0)
        precondition(numberRowHeightRatio > 0)

        self.keySizing = keySizing
        self.horizontalPadding = horizontalPadding
        self.verticalPadding = verticalPadding
        self.horizontalGap = horizontalGap
        self.verticalGap = verticalGap
        self.toolbarHeight = toolbarHeight
        self.toolbarGap = toolbarGap
        self.heightScale = heightScale
        self.labelScale = labelScale
        self.numberRowHeightRatio = numberRowHeightRatio
    }

    /// The vertical strip the toolbar claims: its band plus the gap down to the first
    /// key row. Height budgets reserve exactly this much, so nothing has to restate the
    /// sum and drift away from what the geometry actually lays out.
    public var toolbarChrome: CGFloat { toolbarHeight + toolbarGap }

    /// The padding below the last row of a keyboard `width` points wide.
    ///
    /// None of its own: iOS keeps a globe and dictation bar below a custom keyboard, so
    /// the rows already end well above the screen. Padding on top of that strip only
    /// pushes them further from the thumb.
    public func bottomPadding(forWidth width: CGFloat) -> CGFloat {
        keySizing == .system ? SystemKeyMetrics.bottomPadding(screenWidth: width) : 0
    }

    /// How tall `row` is relative to a letter row of `layout`.
    ///
    /// A number row is only drawn short when it is stacked above the standard rows, the
    /// same rule ``SystemKeyMetrics/rowsHeight(screenWidth:rowCount:)`` budgets for. The
    /// digits of a four-row "123" page are a full row, as on the stock keyboard.
    public func heightWeight(of row: KeyboardRow, in layout: KeyboardLayout) -> CGFloat {
        let isStacked = layout.rows.count > SystemKeyMetrics.standardRowCount
        return row.isNumberRow && isStacked ? numberRowHeightRatio : 1
    }

    public static let `default` = KeyboardSizingProfile()

    /// Apple's gaps and padding. Row heights come from the keyboard's height, which
    /// ``SystemKeyMetrics`` sizes per phone width; the height setting does not apply.
    public static let system = KeyboardSizingProfile(
        keySizing: .system,
        horizontalPadding: SystemKeyMetrics.horizontalPadding,
        horizontalGap: SystemKeyMetrics.horizontalGap,
        verticalGap: SystemKeyMetrics.verticalGap,
        toolbarHeight: SystemKeyMetrics.toolbarHeight,
        toolbarGap: SystemKeyMetrics.toolbarGap,
        numberRowHeightRatio: SystemKeyMetrics.numberRowHeightRatio
    )
}
