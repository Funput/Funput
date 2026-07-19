#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardToolbarView: UIView {
    var onEvent: ((KeyboardKeyEvent) -> Void)?
    var onSystemInputModeEvent: ((UIView, UIEvent) -> Void)?
    var onSuggestionSelected: ((KeyboardSuggestionCandidate) -> Void)?

    private let logoView = KeyboardBrandLogoView()
    private let systemButton = UIButton(type: .system)
    private let emojiButton = UIButton(type: .system)
    private let suggestionBar = KeyboardSuggestionBarView()
    private var spec: KeyboardToolbarSpec?

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(logoView)
        configure(systemButton, symbol: "globe", role: .systemInputMode)
        configure(emojiButton, symbol: "face.smiling", role: .emoji)
        suggestionBar.onSelection = { [weak self] in self?.onSuggestionSelected?($0) }
        addSubview(suggestionBar)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let itemSize = min(36, bounds.height)
        let originY = (bounds.height - itemSize) / 2
        logoView.frame = CGRect(x: 0, y: originY, width: itemSize, height: itemSize)
        emojiButton.frame = CGRect(
            x: bounds.width - itemSize,
            y: originY,
            width: itemSize,
            height: itemSize
        )
        systemButton.frame = frame(before: emojiButton.frame, size: itemSize)
        let controlsMinX = systemButton.isHidden ? emojiButton.frame.minX : systemButton.frame.minX
        suggestionBar.frame = CGRect(
            x: logoView.frame.maxX + 6,
            y: 0,
            width: max(0, controlsMinX - logoView.frame.maxX - 12),
            height: bounds.height
        )
    }

    func apply(
        spec: KeyboardToolbarSpec?,
        theme: ResolvedTheme,
        traits: UITraitCollection
    ) {
        self.spec = spec
        isHidden = spec == nil
        systemButton.isHidden = spec?.systemInputModeKey == nil
        systemButton.accessibilityLabel = spec?.systemInputModeKey?.accessibilityLabel
        emojiButton.accessibilityLabel = spec?.emojiKey.accessibilityLabel

        let label = theme.label.uiColor(for: traits)
        [systemButton, emojiButton].forEach { $0.tintColor = label }
        suggestionBar.apply(theme: theme, traits: traits)
    }

    func updateSuggestions(_ candidates: [KeyboardSuggestionCandidate]) {
        suggestionBar.update(candidates)
    }

    private func frame(before frame: CGRect, size: CGFloat) -> CGRect {
        CGRect(x: frame.minX - size - 2, y: frame.minY, width: size, height: size)
    }

    private func configure(_ button: UIButton, symbol: String, role: KeyRole) {
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.accessibilityTraits = .keyboardKey
        configureInteraction(button, role: role)
        if role == .systemInputMode {
            button.addTarget(
                self,
                action: #selector(handleSystemInputModeEvent(_:with:)),
                for: .allTouchEvents
            )
        }
        addSubview(button)
    }

    private func configureInteraction(_ button: UIButton, role: KeyRole) {
        button.addAction(UIAction { [weak self] _ in
            self?.emit(role, phase: .pressed)
        }, for: .touchDown)
        button.addAction(UIAction { [weak self] _ in
            self?.emit(role, phase: .released)
        }, for: .touchUpInside)
        button.addAction(UIAction { [weak self] _ in
            self?.emit(role, phase: .cancelled)
        }, for: [.touchCancel, .touchDragExit, .touchUpOutside])
    }

    private func emit(_ role: KeyRole, phase: KeyboardKeyEvent.Phase) {
        let key: KeySpec? = switch role {
        case .systemInputMode: spec?.systemInputModeKey
        case .emoji: spec?.emojiKey
        default: nil
        }
        guard let key else { return }
        onEvent?(KeyboardKeyEvent(key: key, phase: phase))
    }

    @objc private func handleSystemInputModeEvent(_ sender: UIView, with event: UIEvent) {
        onSystemInputModeEvent?(sender, event)
    }
}
#endif
