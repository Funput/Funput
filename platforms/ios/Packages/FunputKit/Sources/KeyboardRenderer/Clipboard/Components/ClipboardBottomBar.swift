#if canImport(UIKit)
import UIKit

@MainActor
final class ClipboardBottomBar: UIView {
    var onReturn: (() -> Void)?
    var onClearAll: (() -> Void)?
    var onDelete: (() -> Void)?

    private static let actionWidth: CGFloat = 46

    private let returnButton = UIButton(type: .system)
    private let clearButton = UIButton(type: .system)
    private let deleteButton = UIButton(type: .system)

    override init(frame: CGRect) {
        super.init(frame: frame)
        configure(returnButton, symbol: "keyboard", label: "Trở về bàn phím Funput") {
            [weak self] in self?.onReturn?()
        }
        configure(clearButton, symbol: "trash", label: "Xoá tất cả") {
            [weak self] in self?.onClearAll?()
        }
        configure(deleteButton, symbol: "delete.left", label: "Xóa") {
            [weak self] in self?.onDelete?()
        }
        [returnButton, clearButton, deleteButton].forEach(addSubview)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let width = Self.actionWidth
        returnButton.frame = CGRect(x: 0, y: 0, width: width, height: bounds.height)
        deleteButton.frame = CGRect(
            x: bounds.width - width, y: 0, width: width, height: bounds.height
        )
        clearButton.frame = CGRect(
            x: bounds.width - width * 2, y: 0, width: width, height: bounds.height
        )
    }

    /// Clear-all is tinted apart from the rest: it is the one destructive action here,
    /// and it is deliberately in plain sight rather than buried in Settings.
    func apply(color: UIColor, canClear: Bool) {
        [returnButton, deleteButton].forEach { $0.tintColor = color }
        clearButton.tintColor = canClear ? .systemRed : color.withAlphaComponent(0.3)
        clearButton.isEnabled = canClear
    }

    private func configure(
        _ button: UIButton, symbol: String, label: String, action: @escaping () -> Void
    ) {
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.accessibilityLabel = label
        button.accessibilityTraits = .keyboardKey
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
    }
}
#endif
