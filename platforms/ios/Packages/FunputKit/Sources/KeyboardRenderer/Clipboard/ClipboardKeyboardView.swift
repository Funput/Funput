#if canImport(UIKit)
import ThemeSchema
import UIKit

/// Panel listing text pasted through Funput.
///
/// When the pasteboard holds text not yet pasted, a paste row sits above the list. It
/// is the toolbar's own chip, a system `UIPasteControl`, so pasting from here raises
/// no prompt either: the panel never reads the pasteboard to show what is on it.
///
/// Built on the same bones as ``KaomojiKeyboardView`` — `KeyboardBackdropView` for
/// the background, a collection above a 46pt bottom bar — so every theme, image
/// background and material carries over without this panel knowing about them.
///
/// Rows are bare on the backdrop, separated by hairlines, rather than each sitting
/// on its own keycap surface: a large glass surface outside the key grid's
/// `UIGlassContainerEffect` does not adapt, and emoji and kaomoji cells have no
/// backing of their own either.
@MainActor
public final class ClipboardKeyboardView: UIView {
    public var onSelect: ((KeyboardClipboardEntry) -> Void)?
    public var onTogglePin: ((KeyboardClipboardEntry) -> Void)?
    public var onRemove: ((KeyboardClipboardEntry) -> Void)?
    public var onClearAll: (() -> Void)?
    public var onDelete: (() -> Void)?
    public var onReturn: (() -> Void)?
    public var onPaste: ((KeyboardClipboardPaste) -> Void)?

    static let bottomBarHeight: CGFloat = 46
    static let pasteRowHeight: CGFloat = 52
    static let pasteRowInset: CGFloat = 12

    let pasteChip = KeyboardClipboardChipView()
    let backdropView = KeyboardBackdropView()
    let collectionView: UICollectionView
    let bottomBar = ClipboardBottomBar()
    let emptyStateView = ClipboardEmptyStateView()
    var groups: [ClipboardHistorySection.Group] = []
    var presentation = KeyboardPresentation()
    var emptyState = ClipboardEmptyState.nothingSaved
    var hasFullAccess = true
    var pasteHint: KeyboardClipboardHint?
    /// Frozen per refresh so every row in one pass agrees on what "now" is.
    var now = Date()
    var theme: ResolvedTheme { presentation.theme }
    public var backgroundImage: UIImage? { didSet { applyPresentation() } }

    public init() {
        // Replaced with the real list layout in `configureView`, which is the first
        // point where `self` exists for the swipe-action provider to capture.
        collectionView = UICollectionView(
            frame: .zero,
            collectionViewLayout: UICollectionViewFlowLayout()
        )
        super.init(frame: .zero)
        configureView()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    /// The keyboard builds this panel hidden and shows it later. `UIPasteControl` only
    /// measures itself once it can be seen, so the paste row laid out while hidden
    /// keeps a control collapsed to nothing; lay it out again when the panel appears.
    public override var isHidden: Bool {
        didSet {
            guard oldValue, !isHidden else { return }
            pasteChip.remeasure()
        }
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        backdropView.frame = bounds
        let pasteHeight = pasteChip.isHidden ? 0 : Self.pasteRowHeight
        pasteChip.frame = CGRect(x: 0, y: 0, width: bounds.width, height: pasteHeight)
            .insetBy(dx: Self.pasteRowInset, dy: 0)
        let listHeight = max(0, bounds.height - Self.bottomBarHeight - pasteHeight)
        collectionView.frame = CGRect(x: 0, y: pasteHeight, width: bounds.width, height: listHeight)
        emptyStateView.frame = collectionView.frame
        bottomBar.frame = CGRect(
            x: 0, y: bounds.height - Self.bottomBarHeight,
            width: bounds.width, height: Self.bottomBarHeight
        )
    }

    /// Returns the list to the top so re-opening the panel starts fresh.
    public func reset() {
        collectionView.setContentOffset(.zero, animated: false)
    }

    func entry(at indexPath: IndexPath) -> KeyboardClipboardEntry? {
        guard groups.indices.contains(indexPath.section),
              groups[indexPath.section].entries.indices.contains(indexPath.item)
        else { return nil }
        return groups[indexPath.section].entries[indexPath.item]
    }
}
#endif
