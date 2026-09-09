#if canImport(UIKit)
import UIKit

@MainActor
final class ClipboardRetryBanner: UIView {
    var onRetry: (() -> Void)?
    private let label = UILabel()
    let button = UIButton(type: .system)

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.text = "Chưa đọc được clipboard"
        label.font = .preferredFont(forTextStyle: .caption1)
        label.adjustsFontSizeToFitWidth = true
        button.setTitle("Thử lại", for: .normal)
        button.accessibilityLabel = "Thử đọc và lưu clipboard lại"
        button.addAction(UIAction { [weak self] _ in self?.onRetry?() }, for: .touchUpInside)
        [label, button].forEach(addSubview)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func layoutSubviews() {
        super.layoutSubviews()
        label.textColor = tintColor
        label.frame = CGRect(x: 12, y: 0, width: max(0, bounds.width - 104), height: bounds.height)
        button.frame = CGRect(x: bounds.width - 84, y: 0, width: 72, height: bounds.height)
    }
}
#endif
