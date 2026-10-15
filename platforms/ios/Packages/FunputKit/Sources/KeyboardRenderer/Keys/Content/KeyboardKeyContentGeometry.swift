import Foundation
import KeyboardLayout

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

    /// The frame of a key's main label for its placement; `fallback` is the centered frame.
    static func labelFrame(
        for placement: KeyLabelPlacement,
        in bounds: CGRect,
        lineHeight: CGFloat,
        fallback: CGRect
    ) -> CGRect {
        switch placement {
        case .center: fallback
        case .bottomLeading, .bottomTrailing: cornerLabel(in: bounds, lineHeight: lineHeight)
        case .bottomCenter: bottomCenterLabel(in: bounds, lineHeight: lineHeight)
        }
    }

    /// The hint's frame; on the iPad's letter keys it sits lower than the phone's, a tenth of
    /// the key's height below the top edge, as on the stock keyboard.
    static func hintFrame(for placement: KeyLabelPlacement, in bounds: CGRect, fallback: CGRect) -> CGRect {
        guard placement == .bottomCenter else { return fallback }
        var frame = fallback
        frame.origin.y = max(fallback.origin.y, bounds.height * 0.1)
        return frame
    }

    /// One line of text tucked into the bottom corner, as on the iPad's function keys.
    static func cornerLabel(in bounds: CGRect, lineHeight: CGFloat) -> CGRect {
        let inset = min(12, bounds.width * 0.12)
        let height = min(lineHeight + 2, bounds.height)
        return CGRect(
            x: inset,
            y: bottomEdge(in: bounds, height: height),
            width: max(0, bounds.width - inset * 2),
            height: height
        )
    }

    /// One line of text centered along the bottom, on the same line as the corner words, as
    /// on the iPad's letter keys.
    static func bottomCenterLabel(in bounds: CGRect, lineHeight: CGFloat) -> CGRect {
        let height = min(lineHeight + 2, bounds.height)
        return CGRect(
            x: 2,
            y: bottomEdge(in: bounds, height: height),
            width: max(0, bounds.width - 4),
            height: height
        )
    }

    /// Top of a line `height` tall, so every bottom-placed label shares one baseline.
    private static func bottomEdge(in bounds: CGRect, height: CGFloat) -> CGFloat {
        max(0, bounds.maxY - height - min(8, bounds.height * 0.12))
    }
}
