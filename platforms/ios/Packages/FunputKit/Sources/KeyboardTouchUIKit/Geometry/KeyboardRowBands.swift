import CoreGraphics
import KeyboardLayout

/// The vertical band each keycap row occupies, resolved once so a hit test can ask which row a
/// finger is in before it asks which key is nearest.
///
/// Without this, a row that is inset — the ASDF row is, half a key on each side, standard for
/// QWERTY — leaves a strip at the rim where the nearest keycap by plain distance belongs to the
/// row above or below. Every point in that strip resolves to `q` or `shift` rather than `a`,
/// which reads to the user as the key not registering at all when the winner is a modifier.
struct KeyboardRowBands: Sendable {
    private struct Band: Sendable {
        let minY: CGFloat
        let maxY: CGFloat
        let keys: [ResolvedKey]
    }

    private let bands: [Band]

    init(rows: [[ResolvedKey]]) {
        bands = rows.compactMap { row in
            let keys = row.filter { $0.spec.role != .placeholder }
            guard let minY = keys.map(\.frame.minY).min(),
                  let maxY = keys.map(\.frame.maxY).max() else { return nil }
            return Band(minY: minY, maxY: maxY, keys: keys)
        }
    }

    /// The keys of the row whose band contains `y`, or `nil` when the point sits in a row gap or
    /// past the outermost row. Callers fall back to searching every key in that case, which is
    /// the behaviour every gap had before rows were considered at all.
    func keys(containing y: CGFloat) -> [ResolvedKey]? {
        for band in bands where y >= band.minY && y <= band.maxY {
            return band.keys
        }
        return nil
    }
}
