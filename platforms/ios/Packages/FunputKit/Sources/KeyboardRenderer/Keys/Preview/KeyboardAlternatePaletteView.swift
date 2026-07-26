#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardAlternatePaletteView: UIView {
    private var labels: [UILabel] = []
    private var surfaceView: UIView?
    private var selectedIndex: Int?
    private var theme = ResolvedTheme.funputGlass
    private var appliedTheme: ResolvedTheme?
    private var appliedInterfaceStyle: UIUserInterfaceStyle?
    private var appliedReduceTransparency: Bool?

    override init(frame: CGRect) {
        super.init(frame: frame)
        isHidden = true
        isUserInteractionEnabled = false
        isAccessibilityElement = false
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = 0.18
        layer.shadowRadius = 5
        layer.shadowOffset = CGSize(width: 0, height: 2)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func show(
        key: KeySpec,
        layout: KeyboardAlternatePaletteLayout,
        selectedIndex: Int?,
        presentation: KeyboardPresentation,
        traits: UITraitCollection
    ) {
        frame = layout.frame
        theme = presentation.theme
        configureSurface(theme: theme, traits: traits)
        configureLabels(count: key.alternates.count)
        for (index, alternate) in key.alternates.enumerated() {
            labels[index].text = alternate.text(for: presentation.shiftState)
            labels[index].textColor = theme.label.uiColor(for: traits)
            labels[index].frame = layout.itemFrames[index]
        }
        self.selectedIndex = selectedIndex
        applySelection(traits: traits)
        isHidden = false
    }

    func hide() {
        isHidden = true
        selectedIndex = nil
    }

    private func configureLabels(count: Int) {
        guard labels.count != count else { return }
        labels.forEach { $0.removeFromSuperview() }
        labels = (0..<count).map { _ in
            let label = UILabel()
            label.font = .systemFont(ofSize: 22)
            label.textAlignment = .center
            label.layer.cornerCurve = .continuous
            label.layer.cornerRadius = 8
            addSubview(label)
            return label
        }
    }

    private func applySelection(traits: UITraitCollection) {
        for (index, label) in labels.enumerated() {
            label.backgroundColor = index == selectedIndex
                ? theme.accent.uiColor(for: traits).withAlphaComponent(0.28) : .clear
        }
    }

    private func configureSurface(theme: ResolvedTheme, traits: UITraitCollection) {
        let reducesTransparency = UIAccessibility.isReduceTransparencyEnabled
        guard appliedTheme != theme
                || appliedInterfaceStyle != traits.userInterfaceStyle
                || appliedReduceTransparency != reducesTransparency else { return }
        surfaceView?.removeFromSuperview()
        let result = KeyboardKeyPreviewSurfaceFactory.make(
            theme: theme,
            traits: traits,
            reducesTransparency: reducesTransparency
        )
        surfaceView = result.view
        result.view.frame = bounds
        result.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        if #available(iOS 26.0, *), result.usesNativeGlass {
            result.view.tintColor = .clear
            result.view.cornerConfiguration = .corners(radius: .fixed(12))
            result.view.clipsToBounds = false
        } else {
            result.view.layer.cornerCurve = .continuous
            result.view.layer.cornerRadius = 12
            result.view.clipsToBounds = true
        }
        insertSubview(result.view, at: 0)
        appliedTheme = theme
        appliedInterfaceStyle = traits.userInterfaceStyle
        appliedReduceTransparency = reducesTransparency
    }
}
#endif
