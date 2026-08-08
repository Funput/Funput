#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardToolbarView {
    func configure(_ button: UIButton, symbol: String, role: KeyRole) {
        button.setImage(KeyboardToolbarSymbol.image(symbol), for: .normal)
        button.accessibilityTraits = .keyboardKey
        configureInteraction(button, role: role)
        addSubview(button)
    }

    func configureInteraction(_ button: UIButton, role: KeyRole) {
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

    func emit(_ role: KeyRole, phase: KeyboardKeyEvent.Phase) {
        let key: KeySpec? = switch role {
        case .emoji: spec?.emojiKey
        case .clipboard: spec?.clipboardKey
        default: nil
        }
        guard let key else { return }
        onEvent?(KeyboardKeyEvent(key: key, phase: phase))
    }
}
#endif
