#if canImport(UIKit)
import UIKit

@MainActor
final class KaomojiCell: UICollectionViewCell {
    static let reuseIdentifier = "KaomojiCell"

    /// Shared by the cell and by the size measurement in `sizeForItemAt`, so the
    /// width a cell is given always matches the width its text needs.
    static let font = UIFont.systemFont(ofSize: 17)

    private let label = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.font = Self.font
        label.textAlignment = .center
        label.lineBreakMode = .byClipping
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = 0.6
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

    /// Kaomoji are plain text, so the label has to be tinted by the keyboard
    /// theme rather than by the system light/dark colour.
    func apply(_ item: KaomojiItem, color: UIColor) {
        label.text = item.text
        label.textColor = color
        isAccessibilityElement = true
        accessibilityLabel = item.name
        accessibilityTraits = .keyboardKey
    }
}
#endif
