#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyBorderLayer: CAShapeLayer {
    // CoreAnimation clones layers off the main thread, so the `init(layer:)` override
    // below inherits the superclass's nonisolated context and cannot touch isolated
    // storage. Only that clone reads this; every write comes from the main actor.
    private nonisolated(unsafe) var pathCornerRadius: CGFloat = 0

    override init() {
        super.init()
        fillColor = UIColor.clear.cgColor
    }

    override init(layer: Any) {
        super.init(layer: layer)
        if let source = layer as? KeyboardKeyBorderLayer {
            pathCornerRadius = source.pathCornerRadius
            path = source.path
            fillColor = source.fillColor
            strokeColor = source.strokeColor
            lineWidth = source.lineWidth
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func apply(
        theme: ResolvedTheme,
        traits: UITraitCollection,
        usesNativeGlass: Bool
    ) {
        isHidden = usesNativeGlass
            && !theme.surfaceEffects.glassBorderOverrideEnabled
        lineWidth = theme.borderWidth
        strokeColor = theme.border.uiColor(for: traits).cgColor
        pathCornerRadius = theme.cornerRadius
    }

    func update(in bounds: CGRect, cornerRadius: Double? = nil) {
        if let cornerRadius { pathCornerRadius = cornerRadius }
        frame = bounds
        let inset = min(lineWidth / 2, min(bounds.width, bounds.height) / 2)
        path = UIBezierPath(
            roundedRect: bounds.insetBy(dx: inset, dy: inset),
            cornerRadius: max(0, pathCornerRadius - inset)
        ).cgPath
    }
}
#endif
