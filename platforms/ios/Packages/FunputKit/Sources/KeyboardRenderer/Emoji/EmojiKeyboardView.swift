#if canImport(UIKit)
import ThemeSchema
import UIKit

@MainActor
public final class EmojiKeyboardView: UIView {
    public var onEmojiSelected: ((EmojiItem) -> Void)?
    public var onDelete: (() -> Void)?
    public var onReturn: (() -> Void)?

    let backdropView = KeyboardBackdropView()
    let collectionView: UICollectionView
    let bottomBar = EmojiBottomBar()
    var sections: [(category: EmojiCategory, items: [EmojiItem])] = []
    var selectedCategory = EmojiCategory.smileysPeople
    var theme = ResolvedTheme.funputGlass
    var blendsSystemEdge = false
    public var backgroundImage: UIImage? { didSet { applyTheme() } }
    private let catalog: EmojiCatalog

    public init(catalog: EmojiCatalog = .bundled) {
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
        let barHeight: CGFloat = 46
        collectionView.frame = CGRect(x: 0, y: 0, width: bounds.width, height: max(0, bounds.height - barHeight))
        bottomBar.frame = CGRect(x: 0, y: bounds.height - barHeight, width: bounds.width, height: barHeight)
    }

    public override func traitCollectionDidChange(_ previousTraits: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraits)
        guard previousTraits?.userInterfaceStyle != traitCollection.userInterfaceStyle else { return }
        applyTheme()
    }

    public func apply(
        theme: ResolvedTheme,
        blendsSystemEdge: Bool = false,
        recent: [EmojiItem]
    ) {
        self.theme = theme
        self.blendsSystemEdge = blendsSystemEdge
        reload(recent: recent)
        applyTheme()
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

    private func configureView() {
        clipsToBounds = true
        backgroundColor = .clear
        collectionView.backgroundColor = .clear
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(EmojiCell.self, forCellWithReuseIdentifier: EmojiCell.reuseIdentifier)
        collectionView.register(
            EmojiSectionHeaderView.self,
            forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
            withReuseIdentifier: EmojiSectionHeaderView.reuseIdentifier
        )
        bottomBar.onCategory = { [weak self] in self?.select($0, scroll: true) }
        bottomBar.onDelete = { [weak self] in self?.onDelete?() }
        bottomBar.onReturn = { [weak self] in self?.onReturn?() }
        addSubview(backdropView)
        addSubview(collectionView)
        addSubview(bottomBar)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
    }

    @objc private func accessibilityAppearanceDidChange() {
        applyTheme()
    }

    private func reload(recent: [EmojiItem]) {
        sections = EmojiCategory.allCases.compactMap { category in
            let items = category == .recent ? recent : catalog.items(in: category)
            return items.isEmpty ? nil : (category, items)
        }
        collectionView.reloadData()
        if !sections.contains(where: { $0.category == selectedCategory }) {
            selectedCategory = sections.first?.category ?? .smileysPeople
        }
    }

    private func applyTheme() {
        let label = theme.label.uiColor(for: traitCollection)
        backdropView.apply(
            theme: theme,
            traits: traitCollection,
            image: backgroundImage,
            blendsSystemEdge: blendsSystemEdge
        )
        bottomBar.apply(color: label, selected: selectedCategory)
        collectionView.reloadData()
    }

    private static func makeLayout() -> UICollectionViewLayout {
        let layout = UICollectionViewFlowLayout()
        layout.itemSize = CGSize(width: 44, height: 44)
        layout.minimumInteritemSpacing = 0
        layout.minimumLineSpacing = 2
        layout.sectionInset = UIEdgeInsets(top: 0, left: 8, bottom: 10, right: 8)
        layout.headerReferenceSize = CGSize(width: 1, height: 28)
        return layout
    }
}
#endif
