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
}
