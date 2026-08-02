#if canImport(UIKit)
import ThemeSchema
import UIKit

/// Panel listing what the user has pasted through Funput.
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

    static let bottomBarHeight: CGFloat = 46

    let backdropView = KeyboardBackdropView()
    let collectionView: UICollectionView
    let bottomBar = ClipboardBottomBar()
    let emptyStateView = ClipboardEmptyStateView()
    var groups: [ClipboardHistorySection.Group] = []
    var presentation = KeyboardPresentation()
    var emptyState = ClipboardEmptyState.nothingSaved
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

    public override func layoutSubviews() {
        super.layoutSubviews()
        backdropView.frame = bounds
        let listHeight = max(0, bounds.height - Self.bottomBarHeight)
        collectionView.frame = CGRect(x: 0, y: 0, width: bounds.width, height: listHeight)
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
