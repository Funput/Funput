#if canImport(UIKit)
import ThemeSchema
import UIKit

extension EmojiKeyboardView {
    public func apply(presentation: KeyboardPresentation, recent: [EmojiItem]) {
        self.presentation = presentation
        reload(recent: recent)
        applyPresentation()
    }

    public func apply(
        theme: ResolvedTheme,
        blendsSystemEdge: Bool = false,
        recent: [EmojiItem]
    ) {
        var value = presentation
        value.theme = theme
        value.blendsSystemEdge = blendsSystemEdge
        apply(presentation: value, recent: recent)
    }

    func configureView() {
        registerForTraitChanges([UITraitUserInterfaceStyle.self]) { (view: Self, _) in
            view.applyPresentation()
        }
        clipsToBounds = true
        backgroundColor = .clear
        configureCollection()
        bottomBar.onCategory = { [weak self] in self?.select($0, scroll: true) }
        bottomBar.onDelete = { [weak self] in self?.onDelete?() }
        bottomBar.onReturn = { [weak self] in self?.onReturn?() }
        bottomBar.onKaomoji = { [weak self] in self?.onKaomoji?() }
        searchHeader.onActivate = { [weak self] in self?.beginSearch() }
        searchHeader.onClear = { [weak self] in self?.clearSearch() }
        searchHeader.onCancel = { [weak self] in self?.resetSearch() }
        searchResults.onSelect = { [weak self] in self?.onEmojiSelected?($0) }
        searchKeyboard.onKeyEvent = { [weak self] in self?.handleSearchKey($0) }
        searchKeyboard.backdropView.isHidden = true
        [backdropView, collectionView, bottomBar, searchHeader, searchResults, searchKeyboard]
            .forEach(addSubview)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
    }

    func reload(recent: [EmojiItem]) {
        sections = EmojiCategory.allCases.compactMap { category in
            let items = category == .recent ? recent : catalog.items(in: category)
            return items.isEmpty ? nil : (category, items)
        }
        collectionView.reloadData()
        if !sections.contains(where: { $0.category == selectedCategory }) {
            selectedCategory = sections.first?.category ?? .smileysPeople
        }
    }

    func applyPresentation() {
        let label = theme.label.uiColor(for: traitCollection)
        backdropView.apply(
            theme: theme,
            traits: traitCollection,
            image: backgroundImage,
            blendsSystemEdge: presentation.blendsSystemEdge,
            pinsAppearance: presentation.pinsAppearance
        )
        bottomBar.apply(color: label, selected: selectedCategory)
        searchHeader.apply(
            query: searchQuery,
            active: searchState != .browsing,
            theme: theme,
            traits: traitCollection
        )
        applySearchKeyboardPresentation()
        updateSearchResults()
        collectionView.reloadData()
    }

    func configureCollection() {
        collectionView.backgroundColor = .clear
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(EmojiCell.self, forCellWithReuseIdentifier: EmojiCell.reuseIdentifier)
        collectionView.register(
            PanelSectionHeaderView.self,
            forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
            withReuseIdentifier: PanelSectionHeaderView.reuseIdentifier
        )
    }

    @objc private func accessibilityAppearanceDidChange() {
        applyPresentation()
    }

    static func makeLayout() -> UICollectionViewLayout {
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
