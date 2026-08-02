#if canImport(UIKit)
import ThemeSchema
import UIKit

/// Browser for text emoticons, reached from the emoji panel's bottom bar.
///
/// It deliberately does not reuse ``EmojiKeyboardView``: emoji are single square
/// glyphs laid out on a fixed 44×44 grid, while kaomoji are strings of wildly
/// different widths and need cells measured from their text.
@MainActor
public final class KaomojiKeyboardView: UIView {
    public var onKaomojiSelected: ((KaomojiItem) -> Void)?
    public var onDelete: (() -> Void)?
    public var onReturn: (() -> Void)?
    public var onEmoji: (() -> Void)?

    static let itemHeight: CGFloat = 44
    static let minimumItemWidth: CGFloat = 44
    static let horizontalPadding: CGFloat = 16
    static let horizontalInset: CGFloat = 8
    static let interitemSpacing: CGFloat = 14
    static let bottomBarHeight: CGFloat = 46

    let backdropView = KeyboardBackdropView()
    let collectionView: UICollectionView
    let bottomBar = KaomojiBottomBar()
    let catalog: KaomojiCatalog
    var sections: [(category: KaomojiCategory, items: [KaomojiItem])] = []
    var selectedCategory = KaomojiCategory.happy
    var presentation = KeyboardPresentation()
    var measuredWidths: [String: CGFloat] = [:]
    var theme: ResolvedTheme { presentation.theme }
    public var backgroundImage: UIImage? { didSet { applyPresentation() } }

    public init(catalog: KaomojiCatalog = .bundled) {
        self.catalog = catalog
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: Self.makeLayout())
        super.init(frame: .zero)
        configureView()
        reload(recent: [])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        backdropView.frame = bounds
        collectionView.frame = CGRect(
            x: 0, y: 0,
            width: bounds.width,
            height: max(0, bounds.height - Self.bottomBarHeight)
        )
        bottomBar.frame = CGRect(
            x: 0, y: bounds.height - Self.bottomBarHeight,
            width: bounds.width, height: Self.bottomBarHeight
        )
    }

    /// Returns the browser to the top so re-opening the panel starts fresh.
    public func reset() {
        guard let first = sections.first else { return }
        selectedCategory = first.category
        bottomBar.apply(color: theme.label.uiColor(for: traitCollection), selected: selectedCategory)
        collectionView.setContentOffset(.zero, animated: false)
    }

    func select(_ category: KaomojiCategory, scroll: Bool) {
        let target = sections.firstIndex { $0.category == category }
            ?? sections.firstIndex { $0.category == .happy }
        guard let target else { return }
        selectedCategory = sections[target].category
        bottomBar.apply(color: theme.label.uiColor(for: traitCollection), selected: selectedCategory)
        if scroll, !sections[target].items.isEmpty {
            collectionView.scrollToItem(at: IndexPath(item: 0, section: target), at: .top, animated: true)
        }
    }
}
#endif
