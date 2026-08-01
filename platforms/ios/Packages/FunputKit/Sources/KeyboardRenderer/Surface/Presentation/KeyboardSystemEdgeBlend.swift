import Foundation

enum KeyboardSystemEdgeBlend {
    static func depth(for height: CGFloat) -> CGFloat {
        min(32, max(24, height * 0.09))
    }

    static func locations(for height: CGFloat) -> [NSNumber] {
        guard height > 0 else { return [0, 1, 1] }
        let bottomDepth = min(depth(for: height), height * 0.5)
        let bottom = max(0.5, min(1, 1 - bottomDepth / height))
        return [0, NSNumber(value: bottom), 1]
    }
}
