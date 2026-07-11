#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyPreviewView: UIView {
    private let label = UILabel()
    private var effectView: UIVisualEffectView?

    override init(frame: CGRect) {
        super.init(frame: frame)
        isHidden = true
        isUserInteractionEnabled = false
        isAccessibilityElement = false
        label.textAlignment = .center
        label.font = .systemFont(ofSize: 30, weight: .regular)
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = 0.18
        layer.shadowRadius = 5
        layer.shadowOffset = CGSize(width: 0, height: 2)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        effectView?.frame = bounds
        label.frame = bounds
        effectView?.layer.cornerRadius = 12
    }

    func show(
        key: KeySpec,
        sourceFrame: CGRect,
        presentation: KeyboardPresentation,
        traits: UITraitCollection,
        containerBounds: CGRect
    ) {
        guard isEligible(key.role), !key.label.isEmpty else {
            hide()
            return
        }

        configureEffect(theme: presentation.theme, traits: traits)
        label.text = KeyboardKeyContentStyle.label(
            for: key,
            shiftState: presentation.shiftState
        )
        let width = max(52, sourceFrame.width + 10)
        let height: CGFloat = 62
        let originX = min(
            max(containerBounds.minX, sourceFrame.midX - width / 2),
            containerBounds.maxX - width
        )
        let originY = max(containerBounds.minY, sourceFrame.minY - height + 8)
        frame = CGRect(x: originX, y: originY, width: width, height: height)
        isHidden = false
        setNeedsLayout()
    }

    func hide() {
        isHidden = true
        label.text = nil
    }

    private func isEligible(_ role: KeyRole) -> Bool {
        role == .character || role == .vniModifier || role == .punctuation
    }

    private func configureEffect(
        theme: KeyboardThemeTokens,
        traits: UITraitCollection
    ) {
        effectView?.removeFromSuperview()
        let effect: UIVisualEffect
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !UIAccessibility.isReduceTransparencyEnabled {
            let glass = UIGlassEffect(style: .regular)
            glass.isInteractive = false
            glass.tintColor = theme.characterKey.uiColor(for: traits)
            effect = glass
        } else {
            effect = UIBlurEffect(style: .systemMaterial)
        }
        let view = UIVisualEffectView(effect: effect)
        view.clipsToBounds = true
        insertSubview(view, at: 0)
        addSubview(label)
        effectView = view
        label.textColor = theme.label.uiColor(for: traits)
    }
}
#endif
