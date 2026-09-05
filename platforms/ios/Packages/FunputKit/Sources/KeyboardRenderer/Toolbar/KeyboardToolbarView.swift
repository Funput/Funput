#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardToolbarView: UIView {
    var onEvent: ((KeyboardKeyEvent) -> Void)?
    var onSuggestionSelected: ((KeyboardSuggestionCandidate) -> Void)?
    var onClipboardPaste: ((String) -> Void)?

    // Laid out across the band by `layoutContents()` in KeyboardToolbarView+Layout.
    let logoView = KeyboardBrandLogoView()
    let clipboardButton = UIButton(type: .system)
    let emojiButton = UIButton(type: .system)
    let suggestionBar = KeyboardSuggestionBarView()
    let clipboardChip = KeyboardClipboardChipView()
    private var clipboardHint: KeyboardClipboardHint?
    private var hasSuggestions = false
    private var allowsClipboardKey = true
    private var allowsEmojiKey = true
    var spec: KeyboardToolbarSpec?

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(logoView)
        configure(clipboardButton, symbol: "clipboard", role: .clipboard)
        configure(emojiButton, symbol: "face.smiling", role: .emoji)
        suggestionBar.onSelection = { [weak self] in self?.onSuggestionSelected?($0) }
        clipboardChip.onPaste = { [weak self] in self?.onClipboardPaste?($0) }
        clipboardChip.isHidden = true
        addSubview(suggestionBar)
        addSubview(clipboardChip)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layoutContents()
    }

    /// Folds a tap in the padding above the band onto the band itself. The band is drawn
    /// no taller than the text and icons it carries, so its touch target has to reach past
    /// what is painted for a suggestion to stay as easy to hit as a key.
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard point.y < 0, point.y >= -Metrics.topTouchOutset else {
            return super.hitTest(point, with: event)
        }
        return super.hitTest(CGPoint(x: point.x, y: 0), with: event)
    }

    func apply(
        spec: KeyboardToolbarSpec?,
        theme: ResolvedTheme,
        traits: UITraitCollection
    ) {
        self.spec = spec
        isHidden = spec == nil
        emojiButton.accessibilityLabel = spec?.emojiKey.accessibilityLabel
        clipboardButton.accessibilityLabel = spec?.clipboardKey.accessibilityLabel

        let label = theme.label.uiColor(for: traits)
        [emojiButton, clipboardButton].forEach { $0.tintColor = label }
        suggestionBar.apply(theme: theme, traits: traits)
        clipboardChip.apply(theme: theme, traits: traits)
    }

    func updateSuggestions(_ candidates: [KeyboardSuggestionCandidate]) {
        suggestionBar.update(candidates)
        hasSuggestions = !candidates.isEmpty
        arbitrateContentRegion()
    }

    func updateClipboardKeyVisible(_ visible: Bool) {
        allowsClipboardKey = visible
        arbitrateContentRegion()
    }

    func updateEmojiKeyVisible(_ visible: Bool) {
        allowsEmojiKey = visible
        arbitrateContentRegion()
    }

    func updateClipboardHint(_ hint: KeyboardClipboardHint?) {
        clipboardHint = hint
        clipboardChip.update(hint: hint)
        arbitrateContentRegion()
    }

    /// Suggestions win the shared region: they are about what the user is typing
    /// right now, while the clipboard chip is a standing offer they can also reach
    /// from the clipboard panel.
    private func arbitrateContentRegion() {
        suggestionBar.isHidden = !hasSuggestions
        clipboardChip.isHidden = hasSuggestions || clipboardHint == nil
        // The clipboard key yields its slot too: while the user is typing, the whole
        // toolbar belongs to suggestions.
        clipboardButton.isHidden = hasSuggestions || !allowsClipboardKey
        emojiButton.isHidden = !allowsEmojiKey
        setNeedsLayout()
    }
}
#endif
