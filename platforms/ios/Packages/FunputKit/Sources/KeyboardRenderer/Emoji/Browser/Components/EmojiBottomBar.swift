#if canImport(UIKit)
import UIKit

@MainActor
final class EmojiBottomBar: UIView {
    var onReturn: (() -> Void)?
    var onDelete: (() -> Void)?
    var onCategory: ((EmojiCategory) -> Void)?

    private let returnButton = UIButton(type: .system)
    private let deleteButton = UIButton(type: .system)
    private let categoryStack = UIStackView()
    private let categoryScroll = UIScrollView()
    private var categoryButtons: [EmojiCategory: UIButton] = [:]

    override init(frame: CGRect) {
        super.init(frame: frame)
        configureAction(returnButton, symbol: "keyboard", label: "Trở về bàn phím Funput") {
            [weak self] in self?.onReturn?()
        }
        configureAction(deleteButton, symbol: "delete.left", label: "Xóa") {
            [weak self] in self?.onDelete?()
        }
        categoryStack.axis = .horizontal
        categoryStack.distribution = .fillEqually
        EmojiCategory.allCases.forEach(configureCategory)
        categoryScroll.showsHorizontalScrollIndicator = false
        categoryScroll.addSubview(categoryStack)
        addSubview(returnButton)
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
        deleteButton.frame = CGRect(
            x: bounds.width - actionWidth, y: 0, width: actionWidth, height: bounds.height
        )
        categoryScroll.frame = CGRect(
            x: actionWidth, y: 0, width: max(0, bounds.width - actionWidth * 2), height: bounds.height
        )
        let width = max(categoryScroll.bounds.width, CGFloat(categoryButtons.count) * 38)
        categoryStack.frame = CGRect(x: 0, y: 0, width: width, height: bounds.height)
        categoryScroll.contentSize = categoryStack.bounds.size
    }

    func apply(color: UIColor, selected: EmojiCategory) {
        returnButton.tintColor = color
        deleteButton.tintColor = color
        categoryButtons.forEach { category, button in
            button.tintColor = category == selected ? color : color.withAlphaComponent(0.45)
            button.accessibilityTraits = category == selected ? [.button, .selected] : .button
        }
    }

    private func configureCategory(_ category: EmojiCategory) {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: category.symbolName), for: .normal)
        button.accessibilityLabel = category.accessibilityLabel
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

private extension EmojiCategory {
    var symbolName: String {
        switch self {
        case .recent: "clock"
        case .smileysPeople: "face.smiling"
        case .animalsNature: "pawprint"
        case .foodDrink: "fork.knife"
        case .activities: "sportscourt"
        case .travelPlaces: "car"
        case .objects: "lightbulb"
        case .symbols: "heart"
        case .flags: "flag"
        }
    }
}
#endif
