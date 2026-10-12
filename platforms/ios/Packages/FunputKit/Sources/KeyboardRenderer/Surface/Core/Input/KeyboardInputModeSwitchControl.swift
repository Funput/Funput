#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// The touch target of the globe key.
///
/// `UIInputViewController.handleInputModeList(from:with:)` has to see every event of the
/// touch — a tap switches to the next keyboard, a hold opens the system list and a drag
/// picks from it — and the shared touch overlay never hands raw events upward. So the globe
/// gets its own control above the overlay, and the keycap underneath only draws.
@MainActor
final class KeyboardInputModeSwitchControl: UIControl {
    var onHighlightChange: ((Bool) -> Void)?

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        isHidden = true
        // VoiceOver reaches the keycap instead, which falls back to the next keyboard.
        isAccessibilityElement = false
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override var isHighlighted: Bool {
        didSet {
            guard isHighlighted != oldValue else { return }
            onHighlightChange?(isHighlighted)
        }
    }
}

extension KeyboardSurfaceView {
    /// Hands the globe's touches to `target`, normally the input view controller with
    /// `handleInputModeList(from:with:)`. Passing nil returns the globe to the overlay path.
    public func setInputModeSwitch(target: AnyObject?, action: Selector?) {
        inputModeSwitchControl.removeTarget(nil, action: nil, for: .allEvents)
        if let target, let action {
            inputModeSwitchControl.addTarget(target, action: action, for: .allTouchEvents)
        }
        setNeedsLayout()
    }

    func configureInputModeSwitch() {
        contentHost.addSubview(inputModeSwitchControl)
        inputModeSwitchControl.onHighlightChange = { [weak self] highlighted in
            guard let self, let globe = globeKeyID else { return }
            keyControls[globe]?.setPressed(highlighted, presentation: presentation)
        }
    }

    /// Lays the control over the globe keycap's touch area, or hides it when the page has
    /// no globe or nobody is listening.
    func layoutInputModeSwitch() {
        let control = globeKeyID.flatMap { keyControls[$0] }
        let isActive = control != nil && !inputModeSwitchControl.allTargets.isEmpty
        inputModeSwitchControl.isHidden = !isActive
        inputModeSwitchControl.frame = isActive ? control?.frame ?? .zero : .zero
    }

    private var globeKeyID: String? {
        presentation.layout.rows.lazy.flatMap(\.keys).first { $0.role == .globe }?.id
    }
}
#endif
