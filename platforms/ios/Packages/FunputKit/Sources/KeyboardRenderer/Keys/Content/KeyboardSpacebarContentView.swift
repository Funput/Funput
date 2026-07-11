#if canImport(UIKit)
import UIKit

@MainActor
final class KeyboardSpacebarContentView: UIView {
    private let label = UILabel()
    private let leadingChevron = UIImageView()
    private let trailingChevron = UIImageView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.textAlignment = .center
        addSubview(label)
        configure(leadingChevron, symbol: "chevron.compact.left")
        configure(trailingChevron, symbol: "chevron.compact.right")
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        label.frame = bounds

        let height = min(bounds.height * 0.22, 9)
        let width = max(5, height * 0.62)
        let offset = min(bounds.width * 0.34, 68)
        let originY = (bounds.height - height) / 2
        leadingChevron.frame = CGRect(
            x: bounds.midX - offset - width / 2,
            y: originY,
            width: width,
            height: height
        )
        trailingChevron.frame = CGRect(
            x: bounds.midX + offset - width / 2,
            y: originY,
            width: width,
            height: height
        )
    }

    func apply(label text: String, color: UIColor, font: UIFont) {
        label.text = text
        label.textColor = color
        label.font = font
        leadingChevron.tintColor = color.withAlphaComponent(0.6)
        trailingChevron.tintColor = color.withAlphaComponent(0.6)
    }

    private func configure(_ imageView: UIImageView, symbol: String) {
        imageView.image = UIImage(systemName: symbol)
        imageView.contentMode = .scaleAspectFit
        imageView.preferredSymbolConfiguration = UIImage.SymbolConfiguration(weight: .bold)
        addSubview(imageView)
    }
}
#endif
