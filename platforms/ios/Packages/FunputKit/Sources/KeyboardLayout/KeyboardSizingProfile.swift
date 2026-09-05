import Foundation

public struct KeyboardSizingProfile: Hashable, Sendable {
    public var horizontalPadding: CGFloat
    public var verticalPadding: CGFloat
    public var horizontalGap: CGFloat
    public var verticalGap: CGFloat
    /// The suggestion band, sized like Gboard's strip rather than like a key row:
    /// the toolbar carries one line of text and two icons, so anything taller is
    /// keyboard height spent on padding.
    public var toolbarHeight: CGFloat
    public var toolbarGap: CGFloat
    public var heightScale: CGFloat
    public var labelScale: CGFloat

    public init(
        horizontalPadding: CGFloat = 6,
        verticalPadding: CGFloat = 6,
        horizontalGap: CGFloat = 5,
        verticalGap: CGFloat = 7,
        toolbarHeight: CGFloat = 36,
        toolbarGap: CGFloat = 4,
        heightScale: CGFloat = 1,
        labelScale: CGFloat = 1
    ) {
        precondition(horizontalPadding >= 0)
        precondition(verticalPadding >= 0)
        precondition(horizontalGap >= 0)
        precondition(verticalGap >= 0)
        precondition(toolbarHeight > 0)
        precondition(toolbarGap >= 0)
        precondition(heightScale > 0)
        precondition(labelScale > 0)

        self.horizontalPadding = horizontalPadding
        self.verticalPadding = verticalPadding
        self.horizontalGap = horizontalGap
        self.verticalGap = verticalGap
        self.toolbarHeight = toolbarHeight
        self.toolbarGap = toolbarGap
        self.heightScale = heightScale
        self.labelScale = labelScale
    }

    /// The vertical strip the toolbar claims: its band plus the gap down to the first
    /// key row. Height budgets reserve exactly this much, so nothing has to restate the
    /// sum and drift away from what the geometry actually lays out.
    public var toolbarChrome: CGFloat { toolbarHeight + toolbarGap }

    public static let `default` = KeyboardSizingProfile()
}
