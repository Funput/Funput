import CoreGraphics
import KeyboardLayout

/// The vertical band each keycap row owns, resolved once so a hit test can ask which row a
/// finger intends before it asks which key is nearest.
///
/// Without this, a row that is inset — the ASDF row is, half a key on each side, standard for
/// QWERTY — leaves a strip at the rim where the nearest keycap by plain distance belongs to the
/// row above or below. Every point in that strip resolves to `q` or `shift` rather than `a`,
/// which reads to the user as the key not registering at all when the winner is a modifier.
struct KeyboardRowBands: Sendable {
    private struct Extent: Sendable {
        let minY: CGFloat
        let maxY: CGFloat
        let keys: [ResolvedKey]
    }

    private struct Band: Sendable {
        let minY: CGFloat
        let maxY: CGFloat
        let keys: [ResolvedKey]
    }

    private let bands: [Band]

    init(rows: [[ResolvedKey]]) {
        let extents = rows.compactMap { row -> Extent? in
            let keys = row.filter { $0.spec.role != .placeholder }
            guard let minY = keys.map(\.frame.minY).min(),
                  let maxY = keys.map(\.frame.maxY).max() else { return nil }
            return Extent(minY: minY, maxY: maxY, keys: keys)
        }
        bands = extents.enumerated().map { index, extent in
            let minY = index == extents.startIndex
                ? extent.minY
                : Self.midpoint(extents[index - 1].maxY, extent.minY)
            let maxY = index == extents.index(before: extents.endIndex)
                ? extent.maxY
                : Self.midpoint(extent.maxY, extents[index + 1].minY)
            return Band(minY: minY, maxY: maxY, keys: extent.keys)
        }
    }

    /// Interior gaps are split at their midpoint. Past the outermost rows, callers fall back to
    /// searching every key so the existing top and bottom tolerance remains unchanged.
    func keys(containing y: CGFloat) -> [ResolvedKey]? {
        for band in bands where y >= band.minY && y <= band.maxY {
            return band.keys
        }
        return nil
    }

    private static func midpoint(_ lhs: CGFloat, _ rhs: CGFloat) -> CGFloat {
        lhs + (rhs - lhs) / 2
    }
}
