#if canImport(UIKit)
import ThemeSchema
import UIKit

/// A list cell so the row gets iOS's own swipe-to-delete; the visuals are still
/// drawn here by frame, and the list's default background is cleared so the panel
/// backdrop keeps showing through on every theme.
@MainActor
final class ClipboardRowCell: UICollectionViewListCell {
    static let reuseIdentifier = "ClipboardRowCell"
    static let height: CGFloat = 44

    var onTogglePin: (() -> Void)?

    private static let horizontalInset: CGFloat = 12
    /// Full 44pt tap target even though the glyph inside it is small.
    private static let pinSide: CGFloat = 44

    private let previewLabel = UILabel()
    private let timeLabel = UILabel()
    private let pinButton = UIButton(type: .system)
    private let separator = UIView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundConfiguration = .clear()
        // The list sizes rows itself, and nothing in here has an intrinsic height.
        contentView.heightAnchor.constraint(equalToConstant: Self.height).isActive = true
        previewLabel.numberOfLines = 1
        previewLabel.font = .systemFont(ofSize: 15)
        previewLabel.lineBreakMode = .byTruncatingTail
        timeLabel.font = .systemFont(ofSize: 11)
        pinButton.accessibilityLabel = "Ghim"
        pinButton.addAction(UIAction { [weak self] _ in self?.onTogglePin?() }, for: .touchUpInside)
        [previewLabel, timeLabel, pinButton, separator].forEach(contentView.addSubview)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let bounds = contentView.bounds
        let pinX = bounds.width - Self.pinSide
        pinButton.frame = CGRect(x: pinX, y: 0, width: Self.pinSide, height: bounds.height)
        let textWidth = max(0, pinX - Self.horizontalInset * 2)
        previewLabel.frame = CGRect(x: Self.horizontalInset, y: 5, width: textWidth, height: 19)
        timeLabel.frame = CGRect(
            x: Self.horizontalInset, y: previewLabel.frame.maxY + 1, width: textWidth, height: 13
        )
        separator.frame = CGRect(
            x: Self.horizontalInset,
            y: bounds.height - 0.5,
            width: max(0, bounds.width - Self.horizontalInset * 2),
            height: 0.5
        )
    }

    func apply(
        entry: KeyboardClipboardEntry,
        now: Date,
        theme: ResolvedTheme,
        traits: UITraitCollection,
        showsSeparator: Bool
    ) {
        let label = theme.label.uiColor(for: traits)
        let preview = ClipboardRowText.preview(entry.text)
        previewLabel.text = preview
        previewLabel.textColor = label
        timeLabel.text = ClipboardRowText.relativeTime(from: entry.capturedAt, now: now)
        timeLabel.textColor = theme.secondaryLabel.uiColor(for: traits)
        pinButton.setImage(
            UIImage(systemName: entry.isPinned ? "pin.fill" : "pin"),
            for: .normal
        )
        pinButton.tintColor = entry.isPinned ? label : label.withAlphaComponent(0.4)
        pinButton.accessibilityLabel = entry.isPinned ? "Bỏ ghim" : "Ghim"
        // Same hairline the suggestion bar uses between candidates, so the panels
        // share one idea of what a divider looks like on every theme.
        separator.backgroundColor = label.withAlphaComponent(0.18)
        separator.isHidden = !showsSeparator
        isAccessibilityElement = true
        accessibilityLabel = preview
        accessibilityTraits = .button
        // Making the row one element is what lets VoiceOver read the clip and paste
        // it, but it also swallows the pin button — so pinning comes back as a rotor
        // action rather than being unreachable.
        accessibilityCustomActions = [
            UIAccessibilityCustomAction(
                name: entry.isPinned ? "Bỏ ghim" : "Ghim"
            ) { [weak self] _ in
                self?.onTogglePin?()
                return true
            },
        ]
    }
}
#endif
