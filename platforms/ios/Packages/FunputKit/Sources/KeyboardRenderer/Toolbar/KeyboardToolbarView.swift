#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
final class KeyboardToolbarView: UIView {
    var onEvent: ((KeyboardKeyEvent) -> Void)?
    var onSuggestionSelected: ((KeyboardSuggestionCandidate) -> Void)?
    var onClipboardPaste: ((String) -> Void)?

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
        let itemSize = min(36, bounds.height)
        let originY = (bounds.height - itemSize) / 2
        logoView.frame = CGRect(x: 0, y: originY, width: itemSize, height: itemSize)
        // The right-hand controls stack inwards from the trailing edge, and either of
        // them can step aside — the clipboard key while the user is typing, the emoji
        // key when the layout already carries one in its rows.
        var trailing = bounds.width
        var placedAControl = false
        if !emojiButton.isHidden {
            emojiButton.frame = CGRect(
                x: trailing - itemSize,
                y: originY,
                width: itemSize,
                height: itemSize
            )
            trailing = emojiButton.frame.minX
            placedAControl = true
        }
        if !clipboardButton.isHidden {
            // The 2pt separator belongs *between* two controls, so it only applies when
            // something was placed before this one — otherwise the content region would
            // silently lose those 2pt whenever the clipboard key is the one to step aside.
            clipboardButton.frame = CGRect(
                x: trailing - itemSize - (placedAControl ? 2 : 0),
                y: originY,
                width: itemSize,
                height: itemSize
            )
            trailing = clipboardButton.frame.minX
        }
        // Suggestions and the clipboard chip share one region and never show at the
        // same time, so they get the same frame. It ends wherever the controls begin.
        let contentRegion = CGRect(
            x: logoView.frame.maxX + 6,
            y: 0,
            width: max(0, trailing - logoView.frame.maxX - 12),
            height: bounds.height
        )
        suggestionBar.frame = contentRegion
        clipboardChip.frame = contentRegion
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
