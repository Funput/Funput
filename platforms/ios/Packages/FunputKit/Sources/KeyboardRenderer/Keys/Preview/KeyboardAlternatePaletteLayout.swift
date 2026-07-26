#if canImport(UIKit)
import UIKit

struct KeyboardAlternatePaletteLayout: Equatable {
    let frame: CGRect
    let itemFrames: [CGRect]
    let sourceFrame: CGRect

    static func resolve(count: Int, sourceFrame: CGRect, bounds: CGRect) -> Self {
        let safe = bounds.insetBy(dx: 6, dy: 4)
        let padding: CGFloat = 6
        let gap: CGFloat = 2
        let cellHeight: CGFloat = 42
        let available = max(1, safe.width - padding * 2)
        let columns = min(count, max(1, min(9, Int((available + gap) / 40))))
        let rows = Int(ceil(Double(count) / Double(columns)))
        let width = min(safe.width, CGFloat(columns) * 40 - gap + padding * 2)
        let height = CGFloat(rows) * cellHeight + CGFloat(rows - 1) * gap + padding * 2
        let x = min(
            max(safe.minX, sourceFrame.midX - width / 2),
            safe.maxX - width
        )
        let aboveY = sourceFrame.minY - height - 4
        let belowY = sourceFrame.maxY + 4
        let y: CGFloat
        if aboveY >= safe.minY {
            y = aboveY
        } else if belowY + height <= safe.maxY {
            y = belowY
        } else {
            y = max(safe.minY, min(aboveY, safe.maxY - height))
        }
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
        return Self(frame: frame, itemFrames: items, sourceFrame: sourceFrame)
    }

    func index(at point: CGPoint) -> Int? {
        if sourceFrame.insetBy(dx: -8, dy: -8).contains(point) { return 0 }
        let local = CGPoint(x: point.x - frame.minX, y: point.y - frame.minY)
        return itemFrames.firstIndex { $0.insetBy(dx: -1, dy: -1).contains(local) }
    }
}
#endif
