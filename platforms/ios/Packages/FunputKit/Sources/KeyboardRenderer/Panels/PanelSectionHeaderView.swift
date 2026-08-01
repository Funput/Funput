#if canImport(UIKit)
import UIKit

/// Section header shared by the emoji and kaomoji browsers.
@MainActor
final class PanelSectionHeaderView: UICollectionReusableView {
    static let reuseIdentifier = "PanelSectionHeaderView"
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

    func apply(title: String, color: UIColor) {
        label.text = title
        label.textColor = color
    }
}
#endif
