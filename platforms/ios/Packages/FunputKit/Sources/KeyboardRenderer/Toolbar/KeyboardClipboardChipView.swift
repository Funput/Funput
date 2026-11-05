#if canImport(UIKit)
import FunputShared
import ThemeSchema
import UIKit

/// Toolbar chip offering to paste what is on the pasteboard.
///
/// The button is a system `UIPasteControl`: the user's tap on it *is* the
/// authorisation, so nothing here ever reads the pasteboard and no paste alert is
/// raised. Verified on device — the control works inside a keyboard extension and
/// delivers its provider in ~29ms.
@MainActor
final class KeyboardClipboardChipView: UIView {
    var onPaste: ((KeyboardClipboardPaste) -> Void)?

    private let hintLabel = UILabel()
    private var pasteControl: UIPasteControl?
    private var accent: UIColor = .systemBlue
    private var foreground: UIColor = .white
    private var measureAttempts = 0
    private static let maxMeasureAttempts = 3

    override init(frame: CGRect) {
        super.init(frame: frame)
        pasteConfiguration = UIPasteConfiguration(
            acceptableTypeIdentifiers: ClipboardPlainText.typeIdentifiers
        )
        hintLabel.font = .preferredFont(forTextStyle: .subheadline)
        hintLabel.adjustsFontForContentSizeCategory = true
        hintLabel.lineBreakMode = .byTruncatingTail
        hintLabel.isAccessibilityElement = true
        addSubview(hintLabel)
        rebuildPasteControl()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let pasteControl else { return }
        let intrinsic = pasteControl.intrinsicContentSize
        guard intrinsic.width > 0 else {
            requestMeasuredLayout()
            return
        }
        measureAttempts = 0
        // The compact toolbar band is shorter than the 38.33pt the control asks for,
        // so it is scaled down rather than clipped. Under a transform the frame is
        // meaningless — position with bounds and center.
        let scale = min(1, bounds.height / max(intrinsic.height, 1))
        pasteControl.transform = .identity
        pasteControl.bounds = CGRect(origin: .zero, size: intrinsic)
        pasteControl.transform = CGAffineTransform(scaleX: scale, y: scale)
        let width = intrinsic.width * scale
        pasteControl.center = CGPoint(x: width / 2, y: bounds.midY)
        let textX = width + 8
        hintLabel.frame = CGRect(
            x: textX, y: 0, width: max(0, bounds.width - textX), height: bounds.height
        )
    }

    func update(hint: KeyboardClipboardHint?) {
        isHidden = hint == nil
        hintLabel.text = hint?.title
        hintLabel.accessibilityLabel = hint?.title
        remeasure()
    }

    /// Lays the control out again from scratch, for a host that has just put the chip
    /// on screen after laying it out out of sight.
    func remeasure() {
        measureAttempts = 0
        setNeedsLayout()
    }

    /// `UIPasteControl` reports no size until it has been through a layout pass of its
    /// own on screen, and that pass runs after this view's. Laying it out as nothing
    /// would remove it from the screen, so ask for one more pass instead. Bounded: a
    /// control that never measures, such as one with no window, must not spin.
    private func requestMeasuredLayout() {
        guard window != nil, measureAttempts < Self.maxMeasureAttempts else { return }
        measureAttempts += 1
        DispatchQueue.main.async { [weak self] in self?.setNeedsLayout() }
    }

    func apply(theme: ResolvedTheme, traits: UITraitCollection) {
        hintLabel.textColor = theme.secondaryLabel.uiColor(for: traits)
        let newAccent = theme.accent.uiColor(for: traits)
        let newForeground = Self.readableForeground(on: newAccent)
        // `UIPasteControl.Configuration` is only honoured at init, so a theme change
        // means building a new control and re-attaching its target.
        guard newAccent != accent || newForeground != foreground else { return }
        accent = newAccent
        foreground = newForeground
        rebuildPasteControl()
    }

    /// The control's label sits on the accent colour, which the theme author picks
    /// freely, so the readable foreground has to be derived rather than assumed.
    private static func readableForeground(on background: UIColor) -> UIColor {
        var white: CGFloat = 0
        var alpha: CGFloat = 0
        guard background.getWhite(&white, alpha: &alpha) else { return .white }
        return white > 0.6 ? .black : .white
    }

    private func rebuildPasteControl() {
        pasteControl?.removeFromSuperview()
        let configuration = UIPasteControl.Configuration()
        configuration.displayMode = .iconAndLabel
        configuration.cornerStyle = .capsule
        configuration.baseBackgroundColor = accent
        configuration.baseForegroundColor = foreground
        let control = UIPasteControl(configuration: configuration)
        control.accessibilityIdentifier = "funput.clipboard.paste"
        control.target = self
        addSubview(control)
        pasteControl = control
        setNeedsLayout()
    }

    override func paste(itemProviders: [NSItemProvider]) {
        guard let provider = itemProviders.first(where: {
            ClipboardPlainText.canLoad(from: $0)
        }) else { return }
        let changeCount = UIPasteboard.general.changeCount
        _ = provider.loadObject(ofClass: NSString.self) { [weak self] object, _ in
            guard let text = (object as? NSString) as String? else { return }
            Task { @MainActor in
                let stable = UIPasteboard.general.changeCount == changeCount ? changeCount : nil
                self?.onPaste?(KeyboardClipboardPaste(text: text, changeCount: stable))
            }
        }
    }
}
#endif
