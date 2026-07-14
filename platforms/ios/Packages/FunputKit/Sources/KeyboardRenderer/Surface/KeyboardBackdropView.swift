#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
final class KeyboardBackdropView: UIView {
    private let materialView = UIVisualEffectView()
    private let imageView = UIImageView()
    private let gradientView = UIView()
    private let overlayView = UIView()
    private let gradientLayer = CAGradientLayer()
    private var theme = ResolvedTheme.funputGlass
    private(set) var usesHostMaterial = false
    var contentView: UIView { gradientView }
    var effect: UIVisualEffect? { materialView.effect }

    override init(frame: CGRect) {
        super.init(frame: frame)
        isOpaque = false
        isUserInteractionEnabled = false
        [materialView, imageView, gradientView, overlayView].forEach(addSubview)
        gradientView.layer.addSublayer(gradientLayer)
        imageView.clipsToBounds = true
        imageView.contentMode = .scaleToFill
    }

    convenience init() { self.init(frame: .zero) }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func layoutSubviews() {
        super.layoutSubviews()
        [materialView, imageView, gradientView, overlayView].forEach { $0.frame = bounds }
        gradientLayer.frame = bounds
        updateImageCrop()
    }

    func apply(theme: ResolvedTheme, traits: UITraitCollection, image: UIImage? = nil) {
        self.theme = theme
        imageView.image = image
        let usesImage = theme.backgroundEffects.mode == .image && image != nil
        imageView.isHidden = !usesImage
        overlayView.isHidden = !usesImage
        gradientView.isHidden = usesImage
        configureMaterial(theme: theme, usesImage: usesImage)
        gradientLayer.isHidden = gradientView.isHidden
        if usesImage {
            overlayView.backgroundColor = theme.backgroundEffects.overlay.uiColor(for: traits)
            updateImageCrop()
        } else {
            applyGradient(theme: theme, traits: traits)
        }
    }

    private func configureMaterial(theme: ResolvedTheme, usesImage: Bool) {
        let reducesTransparency = UIAccessibility.isReduceTransparencyEnabled
        if #available(iOS 26.0, *), theme.material == .glass, !reducesTransparency {
            usesHostMaterial = true
            materialView.effect = nil
        } else {
            usesHostMaterial = false
            let solid = theme.material == .solid || reducesTransparency || usesImage
            materialView.effect = solid ? nil : UIBlurEffect(style: .systemChromeMaterial)
        }
        if !usesImage, theme.material == .glass {
            gradientView.isHidden = !theme.colorEffects.glassBackgroundTintEnabled
        }
    }

    private func applyGradient(theme: ResolvedTheme, traits: UITraitCollection) {
        let opaque = theme.material == .solid || UIAccessibility.isReduceTransparencyEnabled
        let start = theme.backgroundStart.uiColor(for: traits)
        let end = theme.backgroundEnd.uiColor(for: traits)
        gradientLayer.colors = [resolved(start, opaque: opaque).cgColor, resolved(end, opaque: opaque).cgColor]
        let points = theme.gradientDirection.layerPoints
        gradientLayer.locations = [0, 1]
        gradientLayer.startPoint = points.start
        gradientLayer.endPoint = points.end
    }

    private func updateImageCrop() {
        guard let image = imageView.image,
              let authored = theme.backgroundEffects.image,
              bounds.width > 0, bounds.height > 0 else { return }
        imageView.layer.contentsRect = ThemeImageCrop.contentsRect(
            imageSize: image.size,
            targetSize: bounds.size,
            focalX: authored.focalX,
            focalY: authored.focalY,
            zoom: authored.zoom
        )
    }

    private func resolved(_ color: UIColor, opaque: Bool) -> UIColor {
        opaque ? color.withAlphaComponent(1) : color
    }
}
#endif
