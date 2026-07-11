#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class KeyboardBackdropView: UIVisualEffectView {
    private let gradientLayer = CAGradientLayer()

    override init(effect: UIVisualEffect?) {
        super.init(effect: effect)
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

    func apply(theme: KeyboardThemeTokens, traits: UITraitCollection) {
        let reducesTransparency = UIAccessibility.isReduceTransparencyEnabled
        effect = reducesTransparency ? nil : UIBlurEffect(style: .systemChromeMaterial)

        let startColor = theme.backgroundStart.uiColor(for: traits)
        let endColor = theme.backgroundEnd.uiColor(for: traits)
        gradientLayer.colors = [
            resolved(startColor, opaque: reducesTransparency).cgColor,
            resolved(endColor, opaque: reducesTransparency).cgColor,
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
