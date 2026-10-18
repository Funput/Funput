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
    private var labelPlacement = KeyLabelPlacement.center
    private var iconRole = KeyRole.character
    /// The label font as the theme sets it, before `layoutSubviews` fits it to the key's height.
    private var themedLabelFont = UIFont.systemFont(ofSize: 17)
    private var fontScale = 1.0

    override init(frame: CGRect) {
        super.init(frame: frame)
        [label, secondaryLabel].forEach(configureLabel)
        secondaryLabel.textAlignment = .right
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
        let frames = KeyboardKeyContentGeometry.frames(
            in: bounds,
            hintLineHeight: secondaryLabel.font.lineHeight
        )
        label.frame = KeyboardKeyContentGeometry.labelFrame(
            for: labelPlacement,
            in: bounds,
            lineHeight: label.font.lineHeight,
            fallback: frames.primaryLabel
        )
        if labelPlacement == .center {
            label.font = KeyLabelTypography.fitting(themedLabelFont, scale: fontScale, height: label.frame.height)
        }
        iconView.frame = KeyboardKeyIconSizing.frame(
            for: iconRole,
            placement: labelPlacement,
            in: bounds,
            imageSize: iconView.image?.size,
            fallback: frames.icon
        )
        secondaryLabel.frame = KeyboardKeyContentGeometry.hintFrame(
            for: labelPlacement, in: bounds, fallback: frames.hint
        )
        spacebarView.frame = bounds
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
        label.font = KeyboardKeyContentStyle.font(for: spec.role, theme: theme)
        secondaryLabel.text = spec.secondaryLabel
        secondaryLabel.textColor = theme.secondaryLabel.uiColor(for: traits)
        secondaryLabel.font = KeyboardKeyContentStyle.hintFont(theme: theme, placement: spec.labelPlacement)

        iconView.image = enterContent
            ? KeyboardKeyContentStyle.enterIcon(presentation.enterAction)
            : KeyboardKeyContentStyle.icon(for: spec.role, shiftState: presentation.shiftState)
        iconView.tintColor = enterContent ? theme.accent.uiColor(for: traits) : labelColor

        labelPlacement = spec.labelPlacement
        iconRole = spec.role
        label.textAlignment = switch spec.labelPlacement {
        case .center, .bottomCenter: .center
        case .bottomLeading: .left
        case .bottomTrailing: .right
        }
        secondaryLabel.textAlignment = spec.labelPlacement == .bottomCenter ? .center : .right
        // A symbol-only key keeps its symbol in the corner; the rest become words.
        if spec.labelPlacement.isCorner, !KeyboardKeyIconSizing.isFixedSize(spec.role) {
            let isLit = KeyboardKeyContentStyle.isLit(spec.role, shiftState: presentation.shiftState)
            label.text = KeyboardKeyContentStyle.cornerLabel(for: spec, enterAction: presentation.enterAction)
            label.font = KeyboardKeyContentStyle.cornerFont(theme: theme)
            label.textColor = isLit || enterContent ? theme.accent.uiColor(for: traits) : labelColor
            iconView.image = nil
        }

        let spaceLabel = spec.horizontalSwipeAction == nil
            ? spec.label
            : presentation.language.displayLabel
        spacebarView.apply(
            label: spaceLabel,
            color: theme.secondaryLabel.uiColor(for: traits),
            font: KeyboardKeyContentStyle.font(for: .space, theme: theme),
            showsChevrons: spec.horizontalSwipeAction != nil
        )

        themedLabelFont = label.font
        fontScale = theme.fontScale
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

    private func enterLabel(_ action: KeyboardEnterAction) -> String {
        if case let .custom(label) = action { return label }
        return ""
    }
}
#endif
