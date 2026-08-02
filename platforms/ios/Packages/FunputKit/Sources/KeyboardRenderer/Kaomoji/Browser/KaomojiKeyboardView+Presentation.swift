#if canImport(UIKit)
import ThemeSchema
import UIKit

extension KaomojiKeyboardView {
    public func apply(presentation: KeyboardPresentation, recent: [KaomojiItem]) {
        self.presentation = presentation
        reload(recent: recent)
        applyPresentation()
    }

    public func apply(
        theme: ResolvedTheme,
        blendsSystemEdge: Bool = false,
        recent: [KaomojiItem]
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
        bottomBar.onEmoji = { [weak self] in self?.onEmoji?() }
        [backdropView, collectionView, bottomBar].forEach(addSubview)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
    }

    func reload(recent: [KaomojiItem]) {
        sections = KaomojiCategory.allCases.compactMap { category in
            let items = category == .recent ? recent : catalog.items(in: category)
            return items.isEmpty ? nil : (category, items)
        }
        collectionView.reloadData()
        if !sections.contains(where: { $0.category == selectedCategory }) {
            selectedCategory = sections.first?.category ?? .happy
        }
    }

    func applyPresentation() {
        backdropView.apply(
            theme: theme,
            traits: traitCollection,
            image: backgroundImage,
            blendsSystemEdge: presentation.blendsSystemEdge
        )
        bottomBar.apply(color: theme.label.uiColor(for: traitCollection), selected: selectedCategory)
        collectionView.reloadData()
    }

    func configureCollection() {
        collectionView.backgroundColor = .clear
        // The panel lays its own subviews out by frame, so an automatic safe-area
        // inset can only push the grid out of place.
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(KaomojiCell.self, forCellWithReuseIdentifier: KaomojiCell.reuseIdentifier)
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
        let layout = KaomojiFlowLayout()
        layout.estimatedItemSize = .zero
        layout.minimumInteritemSpacing = interitemSpacing
        layout.minimumLineSpacing = 6
        layout.sectionInset = UIEdgeInsets(
            top: 0, left: horizontalInset, bottom: 10, right: horizontalInset
        )
        layout.headerReferenceSize = CGSize(width: 1, height: 28)
        return layout
    }
}
#endif
