import CoreGraphics

public struct ResolvedKey: Hashable, Sendable {
    public let spec: KeySpec
    public let frame: CGRect

    public init(spec: KeySpec, frame: CGRect) {
        self.spec = spec
        self.frame = frame
    }
}

public struct ResolvedKeyboard: Hashable, Sendable {
    public let size: CGSize
    public let toolbarFrame: CGRect?
    public let rows: [[ResolvedKey]]

    public var keys: [ResolvedKey] { rows.flatMap { $0 } }

    public init(size: CGSize, toolbarFrame: CGRect?, rows: [[ResolvedKey]]) {
        self.size = size
        self.toolbarFrame = toolbarFrame
        self.rows = rows
    }
}

public enum KeyboardGeometry {
    private static let canonicalColumnCount: CGFloat = 10

    public static func resolve(
        layout: KeyboardLayout,
        size: CGSize,
        sizing: KeyboardSizingProfile
    ) -> ResolvedKeyboard {
        precondition(size.width > 0 && size.height > 0, "Keyboard size must be positive")

        let verticalScale = sizing.heightScale
        let verticalPadding = sizing.verticalPadding * verticalScale
        let verticalGap = sizing.verticalGap * verticalScale
        let contentWidth = max(1, size.width - sizing.horizontalPadding * 2)
        let toolbarFrame = layout.toolbar.map { _ in
            CGRect(
                x: sizing.horizontalPadding,
                y: verticalPadding,
                width: contentWidth,
                height: sizing.toolbarHeight * verticalScale
            )
        }
        let rowsTop = toolbarFrame.map { $0.maxY + sizing.toolbarGap * verticalScale }
            ?? verticalPadding
        let rowsHeight = max(
            1,
            size.height - rowsTop - verticalPadding - verticalGap * CGFloat(layout.rows.count - 1)
        )
        let rowHeight = rowsHeight / CGFloat(layout.rows.count)
        let canonicalKeyWidth = max(
            1,
            (contentWidth - sizing.horizontalGap * (canonicalColumnCount - 1)) / canonicalColumnCount
        )
        let canonicalUnit = canonicalKeyWidth + sizing.horizontalGap

        let rows = layout.rows.enumerated().map { rowIndex, row in
            let visibleKeys = row.keys
            let inset = row.horizontalInsetUnits * canonicalUnit
            let rowWidth = max(1, contentWidth - inset * 2)
            let gapWidth = sizing.horizontalGap * CGFloat(max(visibleKeys.count - 1, 0))
            let totalWeight = visibleKeys.reduce(0) { $0 + $1.widthWeight }
            let widthPerWeight = max(1, (rowWidth - gapWidth) / totalWeight)
            let y = rowsTop + CGFloat(rowIndex) * (rowHeight + verticalGap)
            var x = sizing.horizontalPadding + inset

            return visibleKeys.map { key in
                let width = widthPerWeight * key.widthWeight
                let resolved = ResolvedKey(
                    spec: key,
                    frame: pixelAligned(CGRect(x: x, y: y, width: width, height: rowHeight))
                )
                x += width + sizing.horizontalGap
                return resolved
            }
        }

        return ResolvedKeyboard(size: size, toolbarFrame: toolbarFrame, rows: rows)
    }

    private static func pixelAligned(_ rect: CGRect) -> CGRect {
        CGRect(
            x: (rect.minX * 2).rounded() / 2,
            y: (rect.minY * 2).rounded() / 2,
            width: (rect.width * 2).rounded() / 2,
            height: (rect.height * 2).rounded() / 2
        )
    }
}
