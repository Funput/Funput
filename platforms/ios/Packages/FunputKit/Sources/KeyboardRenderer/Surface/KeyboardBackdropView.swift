#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class KeyboardBackdropView: UIVisualEffectView {
    private let gradientLayer = CAGradientLayer()
    private(set) var usesHostMaterial = false

    override init(effect: UIVisualEffect?) {
        super.init(effect: effect)
        isOpaque = false
        backgroundColor = .clear
        contentView.backgroundColor = .clear
        isUserInteractionEnabled = false
        contentView.layer.addSublayer(gradientLayer)
    }

    convenience init() {
        self.init(effect: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        gradientLayer.frame = bounds
    }

    func apply(theme: ResolvedTheme, traits: UITraitCollection) {
        let reducesTransparency = UIAccessibility.isReduceTransparencyEnabled

        if #available(iOS 26.0, *),
           theme.material == .glass,
           !reducesTransparency {
            // Let the keyboard host's system material show through. Adding another
            // blur or gradient here produces a seam above the globe/dictation bar.
            usesHostMaterial = true
            effect = nil
            gradientLayer.isHidden = !theme.colorEffects.glassBackgroundTintEnabled
            if theme.colorEffects.glassBackgroundTintEnabled {
                applyGradient(theme: theme, traits: traits, opaque: false)
            }
            return
        }

        usesHostMaterial = false
        gradientLayer.isHidden = false
        let isSolid = theme.material == .solid || reducesTransparency
        effect = isSolid ? nil : UIBlurEffect(style: .systemChromeMaterial)
        applyGradient(theme: theme, traits: traits, opaque: isSolid)
    }

    private func applyGradient(
        theme: ResolvedTheme,
        traits: UITraitCollection,
        opaque: Bool
    ) {
        let startColor = theme.backgroundStart.uiColor(for: traits)
        let endColor = theme.backgroundEnd.uiColor(for: traits)
        gradientLayer.colors = [
            resolved(startColor, opaque: opaque).cgColor,
            resolved(endColor, opaque: opaque).cgColor,
        ]
        gradientLayer.locations = [0, 1]
        gradientLayer.startPoint = CGPoint(x: 0.08, y: 0)
        gradientLayer.endPoint = CGPoint(x: 0.92, y: 1)
    }

    private func resolved(_ color: UIColor, opaque: Bool) -> UIColor {
        opaque ? color.withAlphaComponent(1) : color
    }
}
#endif
