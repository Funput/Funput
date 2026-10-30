#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

@MainActor
public final class EmojiKeyboardView: UIView {
    public var onEmojiSelected: ((EmojiItem) -> Void)?
    public var onRecentEmojiSelected: ((EmojiItem) -> Void)?
    public var onDelete: (() -> Void)?
    public var onReturn: (() -> Void)?
    public var onKaomoji: (() -> Void)?

    let backdropView = KeyboardBackdropView()
    let collectionView: UICollectionView
    let bottomBar = EmojiBottomBar()
    let searchHeader = EmojiSearchHeaderView()
    let searchResults = EmojiSearchResultsView()
    /// The letters the query is typed on. Built when a search starts and dropped when
    /// it ends, like the field and the index below: a panel that is only browsed
    /// carries none of the three.
    var searchKeyboard: KeyboardSurfaceView?
    var sections: [(category: EmojiCategory, items: [EmojiItem])] = []
    static let recentLimit = 8
    var latestRecents: [EmojiItem] = []
    // Keep tap targets stable while retaining the latest history for the next visit.
    var freezesRecents = false
    var selectedCategory = EmojiCategory.smileysPeople
    var presentation = KeyboardPresentation()
    var searchState = EmojiSearchState.browsing
    /// Builds the field search keys land in, once per search. The keyboard injects an
    /// engine-backed field so the query composes Vietnamese; the default keeps keys
    /// exactly as typed.
    public var makeSearchComposer: () -> any LocalTextComposing = { PlainLocalTextComposer() }
    var searchComposer: (any LocalTextComposing)?
    var searchQuery: String { searchComposer?.text ?? "" }
    var searchShiftState = ShiftState.lowercase
    let catalog: EmojiCatalog
    var searchIndex: EmojiSearchIndex?
    var theme: ResolvedTheme { presentation.theme }
    public var backgroundImage: UIImage? { didSet { applyBackdrop() } }

    public init(catalog: EmojiCatalog = .bundled) {
        self.catalog = catalog
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: Self.makeLayout())
        super.init(frame: .zero)
        configureView()
        reload(recent: [])
        resetSearch()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        backdropView.frame = bounds
        searchHeader.frame = CGRect(x: 0, y: 2, width: bounds.width, height: 42)
        searchState == .browsing ? layoutBrowser() : layoutSearch()
    }

    public func reset() {
        freezesRecents = false
        reload(recent: latestRecents)
        resetSearch()
    }

    func select(_ category: EmojiCategory, scroll: Bool) {
        let target = sections.firstIndex { $0.category == category }
            ?? sections.firstIndex { $0.category == .smileysPeople }
        guard let target else { return }
        selectedCategory = sections[target].category
        bottomBar.apply(color: theme.label.uiColor(for: traitCollection), selected: selectedCategory)
        if scroll, !sections[target].items.isEmpty {
            collectionView.scrollToItem(at: IndexPath(item: 0, section: target), at: .top, animated: true)
        }
    }

    private func layoutBrowser() {
        let barHeight: CGFloat = 46
        collectionView.frame = CGRect(
            x: 0, y: 46, width: bounds.width, height: max(0, bounds.height - barHeight - 46)
        )
        bottomBar.frame = CGRect(x: 0, y: bounds.height - barHeight, width: bounds.width, height: barHeight)
    }

    private func layoutSearch() {
        let resultsY: CGFloat = 46
        if searchState == .editing {
            let resultHeight = min(54, max(46, bounds.height * 0.18))
            searchResults.frame = CGRect(x: 0, y: resultsY, width: bounds.width, height: resultHeight)
            searchKeyboard?.frame = CGRect(
                x: 0, y: resultsY + resultHeight, width: bounds.width,
                height: max(0, bounds.height - resultsY - resultHeight)
            )
        } else {
            searchResults.frame = CGRect(
                x: 0, y: resultsY, width: bounds.width, height: max(0, bounds.height - resultsY)
            )
        }
    }
}
#endif
