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
        let bottomPadding = sizing.bottomPadding(forWidth: size.width) * verticalScale
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
            size.height - rowsTop - bottomPadding - verticalGap * CGFloat(layout.rows.count - 1)
        )
        let totalWeight = layout.rows.reduce(0) { $0 + sizing.heightWeight(of: $1, in: layout) }
        let unitRowHeight = rowsHeight / totalWeight
        var nextRowTop = rowsTop
        let canonicalKeyWidth = max(
            1,
            (contentWidth - sizing.horizontalGap * (canonicalColumnCount - 1)) / canonicalColumnCount
        )
        let canonicalUnit = canonicalKeyWidth + sizing.horizontalGap

        let grid = Grid(
            leading: sizing.horizontalPadding,
            trailing: size.width - sizing.horizontalPadding,
            pitch: canonicalUnit,
            gap: sizing.horizontalGap
        )

        let rows = layout.rows.map { row in
            let rowHeight = unitRowHeight * sizing.heightWeight(of: row, in: layout)
            let y = nextRowTop
            nextRowTop += rowHeight + verticalGap
            if let spans = row.columnSpans {
                return zip(row.keys, spans).map { key, span in
                    let minX = grid.position(of: span.start)
                    let width = max(1, grid.position(of: span.end) - minX)
                    return ResolvedKey(
                        spec: key,
                        frame: pixelAligned(CGRect(x: minX, y: y, width: width, height: rowHeight))
                    )
                }
            }

            let visibleKeys = row.keys
            let inset = row.horizontalInsetUnits * canonicalUnit
            let rowWidth = max(1, contentWidth - inset * 2)
            let gapWidth = sizing.horizontalGap * CGFloat(max(visibleKeys.count - 1, 0))
            let totalWeight = visibleKeys.reduce(0) { $0 + $1.widthWeight }
            let widthPerWeight = max(1, (rowWidth - gapWidth) / totalWeight)
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

    /// The ten-column letter grid a row's ``KeyColumnSpan``s are measured against.
    private struct Grid {
        let leading: CGFloat
        let trailing: CGFloat
        let pitch: CGFloat
        let gap: CGFloat

        func position(of anchor: KeyColumnSpan.Anchor) -> CGFloat {
            switch anchor {
            case .leading: leading
            case .trailing: trailing
            case let .grid(columns, gaps): leading + columns * pitch + gaps * gap
            }
        }
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
