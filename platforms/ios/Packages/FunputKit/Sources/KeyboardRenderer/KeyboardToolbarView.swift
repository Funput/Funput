#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardToolbarView: UIView {
    var onEvent: ((KeyboardKeyEvent) -> Void)?

    private let brandLabel = UILabel()
    private let suggestions = UIStackView()
    private let emojiButton = UIButton(type: .system)
    private let settingsButton = UIButton(type: .system)
    private let emojiSpec = KeySpec(
        id: "toolbar-emoji",
        label: "",
        role: .emoji,
        accessibilityLabel: "Biểu tượng cảm xúc"
    )
    private let settingsSpec = KeySpec(
        id: "toolbar-settings",
        label: "",
        role: .settings,
        accessibilityLabel: "Cài đặt bàn phím"
    )

    override init(frame: CGRect) {
        super.init(frame: frame)

        brandLabel.text = "F"
        brandLabel.textAlignment = .center
        brandLabel.font = .systemFont(ofSize: 17, weight: .black)
        brandLabel.layer.cornerCurve = .continuous
        addSubview(brandLabel)

        suggestions.axis = .horizontal
        suggestions.distribution = .fillEqually
        suggestions.alignment = .fill
        suggestions.spacing = 1
        ["xin", "chào", "bạn"].forEach { text in
            let label = UILabel()
            label.text = text
            label.textAlignment = .center
            label.font = .systemFont(ofSize: 14, weight: .medium)
            suggestions.addArrangedSubview(label)
        }
        addSubview(suggestions)

        configure(emojiButton, symbol: "face.smiling", spec: emojiSpec)
        configure(settingsButton, symbol: "gearshape", spec: settingsSpec)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let itemSize = min(36, bounds.height)
        brandLabel.frame = CGRect(x: 0, y: (bounds.height - itemSize) / 2, width: itemSize, height: itemSize)
        settingsButton.frame = CGRect(
            x: bounds.width - itemSize,
            y: (bounds.height - itemSize) / 2,
            width: itemSize,
            height: itemSize
        )
        emojiButton.frame = CGRect(
            x: settingsButton.frame.minX - itemSize - 2,
            y: (bounds.height - itemSize) / 2,
            width: itemSize,
            height: itemSize
        )
        suggestions.frame = CGRect(
            x: brandLabel.frame.maxX + 6,
            y: 0,
            width: max(1, emojiButton.frame.minX - brandLabel.frame.maxX - 12),
            height: bounds.height
        )
        brandLabel.layer.cornerRadius = itemSize / 2
    }

    func apply(theme: KeyboardThemeTokens, traits: UITraitCollection) {
        let accent = theme.accent.uiColor(for: traits)
        let label = theme.label.uiColor(for: traits)
        let secondary = theme.secondaryLabel.uiColor(for: traits)
        brandLabel.textColor = traits.userInterfaceStyle == .dark ? .black : .white
        brandLabel.backgroundColor = accent
        emojiButton.tintColor = label
        settingsButton.tintColor = label
        suggestions.arrangedSubviews
            .compactMap { $0 as? UILabel }
            .forEach { $0.textColor = secondary }
    }

    private func configure(_ button: UIButton, symbol: String, spec: KeySpec) {
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.accessibilityLabel = spec.accessibilityLabel
        button.accessibilityTraits = .keyboardKey
        button.addAction(UIAction { [weak self] _ in
            self?.onEvent?(KeyboardKeyEvent(key: spec, phase: .released))
        }, for: .touchUpInside)
        addSubview(button)
    }
}
#endif
