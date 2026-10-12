import Foundation

struct KeyboardKeyContentFrames {
    let primaryLabel: CGRect
    let hint: CGRect
    let icon: CGRect
}

enum KeyboardKeyContentGeometry {
    static func frames(in bounds: CGRect, hintLineHeight: CGFloat) -> KeyboardKeyContentFrames {
        let inset = max(6, bounds.height * 0.2)
        let hintHeight = min(bounds.height * 0.3, hintLineHeight + 2)
        return KeyboardKeyContentFrames(
            primaryLabel: bounds.insetBy(dx: 5, dy: inset * 0.5),
            hint: CGRect(x: 4, y: 3, width: bounds.width - 8, height: hintHeight),
            icon: bounds.insetBy(dx: inset, dy: inset)
        )
    }

    /// One line of text tucked into the bottom corner, as on the iPad's function keys.
    static func cornerLabel(in bounds: CGRect, lineHeight: CGFloat) -> CGRect {
        let inset = min(12, bounds.width * 0.12)
        let height = min(lineHeight + 2, bounds.height)
        return CGRect(
            x: inset,
            y: max(0, bounds.maxY - height - min(8, bounds.height * 0.12)),
            width: max(0, bounds.width - inset * 2),
            height: height
        )
    }
}
