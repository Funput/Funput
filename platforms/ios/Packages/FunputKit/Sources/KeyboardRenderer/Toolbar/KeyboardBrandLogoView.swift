#if canImport(UIKit)
import UIKit

/// The Funput brand mark shown at the leading edge of the toolbar.
///
/// Rendered without a surrounding surface or border so the gradient mark sits
/// naturally on Liquid Glass. Purely decorative: the input method is chosen in
/// the containing app's settings, not from the toolbar.
@MainActor
final class KeyboardBrandLogoView: UIView {
    private let imageView = UIImageView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        isAccessibilityElement = false
        imageView.contentMode = .scaleAspectFit
        imageView.image = Self.logoImage
        addSubview(imageView)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        imageView.frame = bounds
    }

    private static let logoImage: UIImage? = {
        guard let url = Bundle.module.url(forResource: "FunputLogo", withExtension: "png") else {
            return nil
        }
        return UIImage(contentsOfFile: url.path)
    }()
}
#endif
