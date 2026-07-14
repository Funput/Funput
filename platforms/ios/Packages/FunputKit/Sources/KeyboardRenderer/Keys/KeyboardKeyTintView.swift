#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyTintView: UIView {
    private let baseTint = UIView()
    private let pressedTint = UIView()
    private(set) var hasPressedOverlay = false

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        [baseTint, pressedTint].forEach(addSubview)
        pressedTint.alpha = 0
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func layoutSubviews() {
        super.layoutSubviews()
        baseTint.frame = bounds
        pressedTint.frame = bounds
    }

    func apply(
        theme: ResolvedTheme,
        specIsSpecial: Bool,
        traits: UITraitCollection,
        usesNativeGlass: Bool
    ) {
        let keyColor = specIsSpecial ? theme.specialKey : theme.characterKey
        let opacity = specIsSpecial ? theme.specialKeyOpacity : theme.keyOpacity
        let showsBaseTint = usesNativeGlass && theme.colorEffects.glassKeyTintEnabled
        baseTint.backgroundColor = keyColor.uiColor(for: traits).withAlphaComponent(
            showsBaseTint ? min(0.30, opacity * 0.30) : 0
        )
        hasPressedOverlay = theme.colorEffects.pressedOverlayEnabled
        pressedTint.backgroundColor = theme.colorEffects.pressedOverlay
            .uiColor(for: traits).withAlphaComponent(0.18)
        layer.cornerCurve = .continuous
        layer.cornerRadius = theme.cornerRadius
        clipsToBounds = true
    }

    func setPressed(_ pressed: Bool) {
        pressedTint.alpha = hasPressedOverlay && pressed ? 1 : 0
    }
}
#endif
