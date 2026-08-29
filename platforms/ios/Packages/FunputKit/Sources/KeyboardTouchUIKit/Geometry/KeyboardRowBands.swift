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

    /// A strip one key takes from the row above it, resolved ahead of the bands themselves.
    private struct Claim: Sendable {
        let rect: CGRect
        let key: ResolvedKey
    }

    private let bands: [Band]
    private let claims: [Claim]

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
        claims = extents.indices.dropFirst().flatMap { index in
            Self.claims(in: extents[index], reachingInto: extents[index - 1])
        }
    }

    private static func claims(in extent: Extent, reachingInto above: Extent) -> [Claim] {
        extent.keys.compactMap { key in
            KeyboardSpaceReach.claim(
                for: key,
                neighbourBottom: above.maxY,
                neighbourHeight: above.maxY - above.minY
            ).map { Claim(rect: $0, key: key) }
        }
    }

    /// Interior gaps are split at their midpoint, except where a key claims further into the
    /// row above — see `KeyboardSpaceReach`. Past the outermost rows, callers fall back to
    /// searching every key so the existing top and bottom tolerance remains unchanged.
    func candidates(at point: CGPoint) -> [ResolvedKey]? {
        for claim in claims where claim.rect.contains(point) {
            return [claim.key]
        }
        for band in bands where point.y >= band.minY && point.y <= band.maxY {
            return band.keys
        }
        return nil
    }

    private static func midpoint(_ lhs: CGFloat, _ rhs: CGFloat) -> CGFloat {
        lhs + (rhs - lhs) / 2
    }
}
