#if canImport(UIKit)
import UIKit

struct KeyboardAlternatePaletteLayout: Equatable {
    let frame: CGRect
    let itemFrames: [CGRect]
    let sourceFrame: CGRect
    /// True when the palette had to be clamped over the key it came from, which happens
    /// on the top row where there is no room above for every cell.
    let overlapsSource: Bool

    /// Preferred grid width. The palette stays narrow and wraps into more rows so it
    /// reads as a block above the finger rather than a long strip across the keyboard.
    private static let preferredColumns = 6
    /// Past this the block would be taller than the keyboard, so wider rows win instead.
    private static let maximumRows = 3
    private static let preferredSpan: CGFloat = 40
    /// Cells shrink no further than this, even to win a row.
    private static let minimumSpan: CGFloat = 24
    private static let cellHeight: CGFloat = 42
    private static let padding: CGFloat = 6
    private static let gap: CGFloat = 2
    private static let sourceGap: CGFloat = 4
    /// How far the finger must leave its starting point before it stops meaning
    /// "the default cell", used only when the palette covers the source key.
    private static let holdSlop: CGFloat = 16

    static func resolve(count: Int, sourceFrame: CGRect, bounds: CGRect) -> Self {
        let safe = bounds.insetBy(dx: 6, dy: 4)
        let available = max(1, safe.width - padding * 2)
        let columns = columnCount(count: count, available: available)
        let rows = Int(ceil(Double(count) / Double(columns)))
        let span = min(preferredSpan, (available + gap) / CGFloat(columns))
        let width = min(safe.width, CGFloat(columns) * span - gap + padding * 2)
        let height = CGFloat(rows) * cellHeight + CGFloat(rows - 1) * gap + padding * 2
        let x = min(
            max(safe.minX, sourceFrame.midX - width / 2),
            safe.maxX - width
        )
        // The palette always rises from the key. Flipping it below would put it under the
        // hand and away from the finger, so a palette too tall for the space above is
        // clamped to the top edge instead.
        let y = max(safe.minY, min(sourceFrame.minY - sourceGap - height, safe.maxY - height))
        let frame = CGRect(x: x, y: y, width: width, height: height)
        let contentWidth = width - padding * 2 - CGFloat(columns - 1) * gap
        let cellWidth = contentWidth / CGFloat(columns)
        let items = (0..<count).map { index in
            let row = index / columns
            let column = index % columns
            return CGRect(
                x: padding + CGFloat(column) * (cellWidth + gap),
                y: padding + CGFloat(row) * (cellHeight + gap),
                width: cellWidth,
                height: cellHeight
            )
        }
        return Self(
            frame: frame,
            itemFrames: items,
            sourceFrame: sourceFrame,
            overlapsSource: frame.intersects(sourceFrame)
        )
    }

    /// Wraps at ``preferredColumns`` and widens only when the set would otherwise need
    /// more than ``maximumRows`` rows. The rows are then evened out, so thirteen cells
    /// read as 5 + 5 + 3 rather than 6 + 6 + 1.
    private static func columnCount(count: Int, available: CGFloat) -> Int {
        let widthLimit = max(1, Int((available + gap) / minimumSpan))
        let wrapped = Int(ceil(Double(count) / Double(preferredColumns)))
        let rows = min(maximumRows, max(1, wrapped))
        let balanced = Int(ceil(Double(count) / Double(rows)))
        return min(count, min(widthLimit, max(1, balanced)))
    }

    /// The cell a moved finger is on. A palette clamped over its own key has no key
    /// region left to stand for the default, so the touch's starting point holds it
    /// until the finger travels away — otherwise a hand resting still would drift onto
    /// whichever cell happens to cover it.
    func selection(at point: CGPoint, from start: CGPoint) -> Int? {
        guard overlapsSource else { return index(at: point) }
        let dx = point.x - start.x
        let dy = point.y - start.y
        if dx * dx + dy * dy <= Self.holdSlop * Self.holdSlop { return 0 }
        return index(at: point)
    }

    func index(at point: CGPoint) -> Int? {
        if !overlapsSource, sourceFrame.insetBy(dx: -8, dy: -8).contains(point) { return 0 }
        let local = CGPoint(x: point.x - frame.minX, y: point.y - frame.minY)
        return itemFrames.firstIndex { $0.insetBy(dx: -1, dy: -1).contains(local) }
    }
}
#endif
