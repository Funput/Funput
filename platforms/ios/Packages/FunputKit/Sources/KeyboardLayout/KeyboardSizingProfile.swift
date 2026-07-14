import Foundation

public struct KeyboardSizingProfile: Hashable, Sendable {
    public var horizontalPadding: CGFloat
    public var verticalPadding: CGFloat
    public var horizontalGap: CGFloat
    public var verticalGap: CGFloat
    public var toolbarHeight: CGFloat
    public var toolbarGap: CGFloat
    public var heightScale: CGFloat
    public var labelScale: CGFloat

    public init(
        horizontalPadding: CGFloat = 6,
        verticalPadding: CGFloat = 6,
        horizontalGap: CGFloat = 5,
        verticalGap: CGFloat = 7,
        toolbarHeight: CGFloat = 44,
        toolbarGap: CGFloat = 6,
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

    public static let `default` = KeyboardSizingProfile()
}
