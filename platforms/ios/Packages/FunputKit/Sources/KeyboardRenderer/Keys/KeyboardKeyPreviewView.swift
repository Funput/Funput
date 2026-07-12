#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyPreviewView: UIView {
    private let label = UILabel()
    private var effectView: UIVisualEffectView?
    private var usesNativeGlass = false

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
        if #available(iOS 26.0, *), usesNativeGlass {
            effectView?.cornerConfiguration = .corners(radius: .fixed(12))
        } else {
            effectView?.layer.cornerRadius = 12
        }
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
        theme: ResolvedTheme,
        traits: UITraitCollection
    ) {
        effectView?.removeFromSuperview()
        let effect: UIVisualEffect
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !UIAccessibility.isReduceTransparencyEnabled {
            let glass = UIGlassEffect(style: .regular)
            glass.isInteractive = false
            glass.tintColor = .clear
            effect = glass
            usesNativeGlass = true
        } else {
            effect = UIBlurEffect(style: .systemMaterial)
            usesNativeGlass = false
        }
        let view = UIVisualEffectView(effect: effect)
        if #available(iOS 26.0, *), usesNativeGlass {
            view.tintColor = .clear
            view.cornerConfiguration = .corners(radius: .fixed(12))
            view.clipsToBounds = false
        } else {
            view.layer.cornerCurve = .continuous
            view.layer.cornerRadius = 12
            view.clipsToBounds = true
        }
        insertSubview(view, at: 0)
        addSubview(label)
        effectView = view
        label.textColor = theme.label.uiColor(for: traits)
    }
}
#endif
