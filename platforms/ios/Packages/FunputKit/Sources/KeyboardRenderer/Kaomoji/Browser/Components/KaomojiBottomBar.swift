#if canImport(UIKit)
import UIKit

@MainActor
final class KaomojiBottomBar: UIView {
    var onReturn: (() -> Void)?
    var onDelete: (() -> Void)?
    var onEmoji: (() -> Void)?
    var onCategory: ((KaomojiCategory) -> Void)?

    private let returnButton = UIButton(type: .system)
    private let emojiButton = UIButton(type: .system)
    private let deleteButton = UIButton(type: .system)
    private let categoryStack = UIStackView()
    private let categoryScroll = UIScrollView()
    private var categoryButtons: [KaomojiCategory: UIButton] = [:]

    override init(frame: CGRect) {
        super.init(frame: frame)
        configureAction(returnButton, symbol: "keyboard", label: "Trở về bàn phím Funput") {
            [weak self] in self?.onReturn?()
        }
        configureEmoji()
        configureAction(deleteButton, symbol: "delete.left", label: "Xóa") {
            [weak self] in self?.onDelete?()
        }
        categoryStack.axis = .horizontal
        categoryStack.distribution = .fillEqually
        KaomojiCategory.allCases.forEach(configureCategory)
        categoryScroll.showsHorizontalScrollIndicator = false
        categoryScroll.addSubview(categoryStack)
        addSubview(returnButton)
        addSubview(emojiButton)
        addSubview(categoryScroll)
        addSubview(deleteButton)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let actionWidth: CGFloat = 46
        returnButton.frame = CGRect(x: 0, y: 0, width: actionWidth, height: bounds.height)
        emojiButton.frame = CGRect(
            x: actionWidth, y: 0, width: actionWidth, height: bounds.height
        )
        deleteButton.frame = CGRect(
            x: bounds.width - actionWidth, y: 0, width: actionWidth, height: bounds.height
        )
        categoryScroll.frame = CGRect(
            x: actionWidth * 2, y: 0,
            width: max(0, bounds.width - actionWidth * 3), height: bounds.height
        )
        let width = max(categoryScroll.bounds.width, CGFloat(categoryButtons.count) * 38)
        categoryStack.frame = CGRect(x: 0, y: 0, width: width, height: bounds.height)
        categoryScroll.contentSize = categoryStack.bounds.size
    }

    /// A real emoji glyph rather than `face.smiling`, which would be identical to
    /// the "Vui vẻ" category symbol sitting two buttons away.
    private func configureEmoji() {
        emojiButton.setTitle("😀", for: .normal)
        emojiButton.titleLabel?.font = .systemFont(ofSize: 20)
        emojiButton.accessibilityLabel = "Biểu tượng cảm xúc"
        emojiButton.accessibilityTraits = .keyboardKey
        emojiButton.addAction(
            UIAction { [weak self] _ in self?.onEmoji?() }, for: .touchUpInside
        )
    }

    func apply(color: UIColor, selected: KaomojiCategory) {
        [returnButton, deleteButton].forEach { $0.tintColor = color }
        categoryButtons.forEach { category, button in
            button.tintColor = category == selected ? color : color.withAlphaComponent(0.45)
            button.accessibilityTraits = category == selected ? [.button, .selected] : .button
        }
        revealSelectedCategory(selected)
    }

    /// Ten categories do not fit the strip, so the highlighted one is scrolled into
    /// view — otherwise browsing marks a tab the user cannot see.
    private func revealSelectedCategory(_ category: KaomojiCategory) {
        guard let button = categoryButtons[category], !categoryScroll.bounds.isEmpty else { return }
        categoryScroll.scrollRectToVisible(button.frame, animated: false)
    }

    private func configureCategory(_ category: KaomojiCategory) {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: category.symbolName), for: .normal)
        button.accessibilityLabel = category.displayName
        button.addAction(UIAction { [weak self] _ in self?.onCategory?(category) }, for: .touchUpInside)
        categoryButtons[category] = button
        categoryStack.addArrangedSubview(button)
    }

    private func configureAction(
        _ button: UIButton, symbol: String, label: String, action: @escaping () -> Void
    ) {
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.accessibilityLabel = label
        button.accessibilityTraits = .keyboardKey
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
    }
}

private extension KaomojiCategory {
    var symbolName: String {
        switch self {
        case .recent: "clock"
        case .happy: "face.smiling"
        case .sad: "cloud.rain"
        case .angry: "flame"
        case .love: "heart"
        case .surprised: "exclamationmark.bubble"
        case .confused: "questionmark.circle"
        case .action: "figure.run"
        case .animal: "pawprint"
        case .greeting: "hand.wave"
        }
    }
}
#endif
