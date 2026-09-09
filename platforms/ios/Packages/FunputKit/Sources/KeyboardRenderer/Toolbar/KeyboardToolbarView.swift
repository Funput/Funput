#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardToolbarView: UIView {
    var onEvent: ((KeyboardKeyEvent) -> Void)?
    var onSuggestionSelected: ((KeyboardSuggestionCandidate) -> Void)?
    var onClipboardPaste: ((KeyboardClipboardPaste) -> Void)?

    // Laid out across the band by `layoutContents()` in KeyboardToolbarView+Layout.
    let logoView = KeyboardBrandLogoView()
    let clipboardButton = UIButton(type: .system)
    let emojiButton = UIButton(type: .system)
    let suggestionBar = KeyboardSuggestionBarView()
    let clipboardChip = KeyboardClipboardChipView()
    private var clipboardHint: KeyboardClipboardHint?
    private var hasSuggestions = false
    private var allowsClipboardKey = true
    /// Whether the offer arrived since the user last typed. See `arbitrateContentRegion`.
    private var pasteOfferIsNew = false
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
        // The user has typed since the offer arrived, so it is no longer the newest
        // thing in the band and stops holding the region.
        pasteOfferIsNew = false
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
        pasteOfferIsNew = hint != nil && hint != clipboardHint
        clipboardHint = hint
        clipboardChip.update(hint: hint)
        arbitrateContentRegion()
    }

    /// Both want the one shared region, and each is right at a different moment.
    ///
    /// Copying while typing must expose Paste, or the offer is swallowed by the
    /// candidates and the user never learns it was there. But the offer stands until
    /// they paste or copy again, so letting it hold the region would cost them every
    /// suggestion for the rest of the session. It holds until the next keystroke and
    /// then steps aside; the clipboard key and the history panel still reach it.
    private func arbitrateContentRegion() {
        let pasteWins = clipboardHint != nil && (!hasSuggestions || pasteOfferIsNew)
        suggestionBar.isHidden = !hasSuggestions || pasteWins
        clipboardChip.isHidden = !pasteWins
        // The clipboard key yields its slot too: while the user is typing, the whole
        // toolbar belongs to suggestions.
        clipboardButton.isHidden = hasSuggestions || !allowsClipboardKey
        emojiButton.isHidden = !allowsEmojiKey
        setNeedsLayout()
    }
}
#endif
