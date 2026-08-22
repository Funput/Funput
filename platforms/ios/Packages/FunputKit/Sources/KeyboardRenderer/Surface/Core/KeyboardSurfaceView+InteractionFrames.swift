#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceView {
    /// Gives fallback controls a complete, non-overlapping touch grid while their keycaps keep
    /// the authored visual frames. The central overlay remains the primary multi-touch path.
    func layoutKeyControls(in geometry: ResolvedKeyboard) {
        let rows = geometry.rows.map { $0.filter { $0.spec.role != .placeholder } }
            .filter { !$0.isEmpty }
        for (rowIndex, row) in rows.enumerated() {
            let visualMinY = row.map(\.frame.minY).min() ?? 0
            let visualMaxY = row.map(\.frame.maxY).max() ?? bounds.height
            let minY = rowIndex == rows.startIndex
                ? geometry.toolbarFrame?.maxY ?? 0
                : midpoint(rows[rowIndex - 1].visualMaxY, visualMinY)
            let maxY = rowIndex == rows.index(before: rows.endIndex)
                ? bounds.height
                : midpoint(visualMaxY, rows[rowIndex + 1].visualMinY)

            for (keyIndex, key) in row.enumerated() {
                let minX = keyIndex == row.startIndex
                    ? 0 : midpoint(row[keyIndex - 1].frame.maxX, key.frame.minX)
                let maxX = keyIndex == row.index(before: row.endIndex)
                    ? bounds.width : midpoint(key.frame.maxX, row[keyIndex + 1].frame.minX)
                let interaction = CGRect(x: minX, y: minY, width: maxX - minX, height: maxY - minY)
                keyControls[key.spec.id]?.applyFrames(interaction: interaction, visual: key.frame)
            }
        }
    }

    private func midpoint(_ lhs: CGFloat, _ rhs: CGFloat) -> CGFloat {
        lhs + (rhs - lhs) / 2
    }
}

private extension Array where Element == ResolvedKey {
    var visualMinY: CGFloat { map(\.frame.minY).min() ?? 0 }
    var visualMaxY: CGFloat { map(\.frame.maxY).max() ?? 0 }
}
#endif
