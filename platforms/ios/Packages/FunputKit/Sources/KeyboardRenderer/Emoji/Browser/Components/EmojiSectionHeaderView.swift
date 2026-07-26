#if canImport(UIKit)
import UIKit

@MainActor
final class EmojiSectionHeaderView: UICollectionReusableView {
    static let reuseIdentifier = "EmojiSectionHeaderView"
    private let label = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.font = .preferredFont(forTextStyle: .caption1)
        label.adjustsFontForContentSizeCategory = true
        addSubview(label)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        label.frame = bounds.insetBy(dx: 8, dy: 0)
    }

    func apply(category: EmojiCategory, color: UIColor) {
        label.text = category.accessibilityLabel
        label.textColor = color
    }
}
#endif
