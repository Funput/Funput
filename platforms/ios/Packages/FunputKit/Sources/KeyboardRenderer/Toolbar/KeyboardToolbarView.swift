#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardToolbarView: UIView {
    var onEvent: ((KeyboardKeyEvent) -> Void)?

    private let brandLabel = UILabel()
    private let systemButton = UIButton(type: .system)
    private let settingsButton = UIButton(type: .system)
    private let emojiButton = UIButton(type: .system)
    private var spec: KeyboardToolbarSpec?

    override init(frame: CGRect) {
        super.init(frame: frame)
        brandLabel.text = "F"
        brandLabel.textAlignment = .center
        brandLabel.font = .systemFont(ofSize: 17, weight: .black)
        brandLabel.layer.cornerCurve = .continuous
        addSubview(brandLabel)

        configure(systemButton, symbol: "globe", role: .systemInputMode)
        configure(settingsButton, symbol: "gearshape", role: .settings)
        configure(emojiButton, symbol: "face.smiling", role: .emoji)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let itemSize = min(36, bounds.height)
        let originY = (bounds.height - itemSize) / 2
        brandLabel.frame = CGRect(x: 0, y: originY, width: itemSize, height: itemSize)
        emojiButton.frame = CGRect(
            x: bounds.width - itemSize,
            y: originY,
            width: itemSize,
            height: itemSize
        )
        settingsButton.frame = frame(before: emojiButton.frame, size: itemSize)
        systemButton.frame = frame(before: settingsButton.frame, size: itemSize)
        brandLabel.layer.cornerRadius = itemSize / 2
    }

    func apply(
        spec: KeyboardToolbarSpec?,
        theme: KeyboardThemeTokens,
        traits: UITraitCollection
    ) {
        self.spec = spec
        isHidden = spec == nil
        systemButton.isHidden = spec?.systemInputModeKey == nil
        systemButton.accessibilityLabel = spec?.systemInputModeKey?.accessibilityLabel
        settingsButton.accessibilityLabel = spec?.settingsKey.accessibilityLabel
        emojiButton.accessibilityLabel = spec?.emojiKey.accessibilityLabel

        let accent = theme.accent.uiColor(for: traits)
        let label = theme.label.uiColor(for: traits)
        brandLabel.textColor = traits.userInterfaceStyle == .dark ? .black : .white
        brandLabel.backgroundColor = accent
        [systemButton, settingsButton, emojiButton].forEach { $0.tintColor = label }
    }

    private func frame(before frame: CGRect, size: CGFloat) -> CGRect {
        CGRect(x: frame.minX - size - 2, y: frame.minY, width: size, height: size)
    }

    private func configure(_ button: UIButton, symbol: String, role: KeyRole) {
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.accessibilityTraits = .keyboardKey
        button.addAction(UIAction { [weak self] _ in self?.emit(role) }, for: .touchUpInside)
        addSubview(button)
    }

    private func emit(_ role: KeyRole) {
        let key: KeySpec? = switch role {
        case .systemInputMode: spec?.systemInputModeKey
        case .settings: spec?.settingsKey
        case .emoji: spec?.emojiKey
        default: nil
        }
        guard let key else { return }
        onEvent?(KeyboardKeyEvent(key: key, phase: .released))
    }
}
#endif
