#if canImport(UIKit)
import UIKit

@MainActor
final class EmojiCell: UICollectionViewCell {
    static let reuseIdentifier = "EmojiCell"
    private let label = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.font = .systemFont(ofSize: 32)
        label.textAlignment = .center
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.7
        contentView.addSubview(label)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        label.frame = contentView.bounds
    }

    func apply(_ item: EmojiItem) {
        label.text = item.glyph
        isAccessibilityElement = true
        accessibilityLabel = item.name
        accessibilityTraits = .keyboardKey
    }
}
#endif
