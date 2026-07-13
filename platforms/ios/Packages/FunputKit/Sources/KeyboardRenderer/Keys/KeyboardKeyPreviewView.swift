#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardKeyPreviewView: UIView {
    private let label = UILabel()
    private var effectView: UIVisualEffectView?
    private var usesNativeGlass = false
    private var appliedTheme: ResolvedTheme?
    private var appliedInterfaceStyle: UIUserInterfaceStyle?
    private var appliedReduceTransparency: Bool?
    private var visibilityGeneration = 0

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
        visibilityGeneration += 1
        guard isEligible(key.role), !key.label.isEmpty else {
            hideImmediately()
            return
        }

        configureEffect(theme: presentation.theme, traits: traits)
        label.text = KeyboardKeyContentStyle.label(
            for: key,
            shiftState: presentation.shiftState
        )
        let safeBounds = containerBounds.insetBy(dx: 6, dy: 4)
        let width = min(max(52, sourceFrame.width + 10), safeBounds.width)
        let height = min(62, safeBounds.height)
        let originX = min(
            max(safeBounds.minX, sourceFrame.midX - width / 2),
            safeBounds.maxX - width
        )
        let originY = max(safeBounds.minY, sourceFrame.minY - height + 8)
        frame = CGRect(x: originX, y: originY, width: width, height: height)
        isHidden = false
        setNeedsLayout()
    }

    func hide() {
        let generation = visibilityGeneration
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.09) { [weak self] in
            guard let self, self.visibilityGeneration == generation else { return }
            self.hideImmediately()
        }
    }

    func apply(theme: ResolvedTheme, traits: UITraitCollection) {
        configureEffect(theme: theme, traits: traits)
    }

    private func hideImmediately() {
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
        let reduceTransparency = UIAccessibility.isReduceTransparencyEnabled
        label.textColor = theme.label.uiColor(for: traits)
        guard appliedTheme != theme
                || appliedInterfaceStyle != traits.userInterfaceStyle
                || appliedReduceTransparency != reduceTransparency else { return }
        effectView?.removeFromSuperview()
        let effect: UIVisualEffect
        if #available(iOS 26.0, *),
           theme.material == .glass,
           !reduceTransparency {
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
        appliedTheme = theme
        appliedInterfaceStyle = traits.userInterfaceStyle
        appliedReduceTransparency = reduceTransparency
    }
}
#endif
