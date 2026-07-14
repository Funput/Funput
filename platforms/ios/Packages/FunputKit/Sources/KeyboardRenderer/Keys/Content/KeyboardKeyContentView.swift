#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyContentView: UIView {
    private let label = UILabel()
    private let secondaryLabel = UILabel()
    private let iconView = UIImageView()
    private let spacebarView = KeyboardSpacebarContentView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        [label, secondaryLabel].forEach(configureLabel)
        configureIcon()
        spacebarView.isUserInteractionEnabled = false
        addSubview(spacebarView)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let inset = max(6, bounds.height * 0.2)
        label.frame = bounds.insetBy(dx: 5, dy: inset * 0.5)
        iconView.frame = bounds.insetBy(dx: inset, dy: inset)
        spacebarView.frame = bounds

        let hintHeight = bounds.height * 0.32
        secondaryLabel.frame = CGRect(x: 2, y: 2, width: bounds.width - 4, height: hintHeight)
        if !secondaryLabel.isHidden {
            label.frame = CGRect(
                x: 4,
                y: hintHeight * 0.7,
                width: bounds.width - 8,
                height: bounds.height - hintHeight * 0.7 - 2
            )
        }
    }

    func apply(
        spec: KeySpec,
        presentation: KeyboardPresentation,
        traits: UITraitCollection
    ) {
        let theme = presentation.theme
        let labelColor = theme.label.uiColor(for: traits)
        let hasHint = spec.secondaryLabel != nil
        let isSpace = spec.role == .space
        let enterContent = spec.role == .enter

        label.text = enterContent
            ? enterLabel(presentation.enterAction)
            : KeyboardKeyContentStyle.label(for: spec, shiftState: presentation.shiftState)
        label.textColor = enterContent ? theme.accent.uiColor(for: traits) : labelColor
        label.font = KeyboardKeyContentStyle.font(for: spec.role, scale: theme.fontScale)
        secondaryLabel.text = spec.secondaryLabel
        secondaryLabel.textColor = theme.secondaryLabel.uiColor(for: traits)
        secondaryLabel.font = .systemFont(ofSize: 9 * theme.fontScale, weight: .medium)

        iconView.image = enterContent
            ? enterIcon(presentation.enterAction)
            : KeyboardKeyContentStyle.icon(for: spec.role, shiftState: presentation.shiftState)
        iconView.tintColor = enterContent ? theme.accent.uiColor(for: traits) : labelColor

        let spaceLabel = spec.horizontalSwipeAction == nil
            ? spec.label
            : presentation.language.displayLabel
        spacebarView.apply(
            label: spaceLabel,
            color: theme.secondaryLabel.uiColor(for: traits),
            font: KeyboardKeyContentStyle.font(for: .space, scale: theme.fontScale),
            showsChevrons: spec.horizontalSwipeAction != nil
        )

        spacebarView.isHidden = !isSpace
        secondaryLabel.isHidden = !hasHint || isSpace
        label.isHidden = isSpace || iconView.image != nil
        iconView.isHidden = isSpace || iconView.image == nil
        setNeedsLayout()
    }

    private func configureLabel(_ view: UILabel) {
        view.textAlignment = .center
        view.adjustsFontSizeToFitWidth = true
        view.minimumScaleFactor = 0.65
        view.isUserInteractionEnabled = false
        addSubview(view)
    }

    private func configureIcon() {
        iconView.contentMode = .scaleAspectFit
        iconView.preferredSymbolConfiguration = UIImage.SymbolConfiguration(weight: .medium)
        iconView.isUserInteractionEnabled = false
        addSubview(iconView)
    }

    private func enterIcon(_ action: KeyboardEnterAction) -> UIImage? {
        let name: String? = switch action {
        case .newLine: "return"
        case .go: "arrow.right"
        case .search: "magnifyingglass"
        case .send: "paperplane"
        case .next: "arrow.right.to.line"
        case .done: "checkmark"
        case .previous: "arrow.left.to.line"
        case .custom: nil
        }
        return name.flatMap { UIImage(systemName: $0) }
    }

    private func enterLabel(_ action: KeyboardEnterAction) -> String {
        if case let .custom(label) = action { return label }
        return ""
    }
}
#endif
