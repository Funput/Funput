#if canImport(UIKit)
import ThemeSchema
import UIKit

extension ClipboardKeyboardView {
    public func apply(
        presentation: KeyboardPresentation,
        entries: [KeyboardClipboardEntry],
        hasFullAccess: Bool,
        now: Date = Date()
    ) {
        self.presentation = presentation
        self.now = now
        emptyState = hasFullAccess ? .nothingSaved : .needsFullAccess
        groups = hasFullAccess ? ClipboardHistorySection.make(from: entries) : []
        applyPresentation()
    }

    public func apply(
        theme: ResolvedTheme,
        entries: [KeyboardClipboardEntry],
        hasFullAccess: Bool = true,
        now: Date = Date()
    ) {
        var value = presentation
        value.theme = theme
        apply(presentation: value, entries: entries, hasFullAccess: hasFullAccess, now: now)
    }

    func configureView() {
        registerForTraitChanges([UITraitUserInterfaceStyle.self]) { (view: Self, _) in
            view.applyPresentation()
        }
        clipsToBounds = true
        backgroundColor = .clear
        configureCollection()
        bottomBar.onReturn = { [weak self] in self?.onReturn?() }
        bottomBar.onDelete = { [weak self] in self?.onDelete?() }
        bottomBar.onClearAll = { [weak self] in self?.onClearAll?() }
        [backdropView, collectionView, emptyStateView, bottomBar].forEach(addSubview)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityAppearanceDidChange),
            name: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
            object: nil
        )
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
        let isEmpty = groups.isEmpty
        emptyStateView.isHidden = !isEmpty
        collectionView.isHidden = isEmpty
        emptyStateView.apply(state: emptyState, theme: theme, traits: traitCollection)
        bottomBar.apply(color: label, canClear: !isEmpty)
        collectionView.reloadData()
    }

    func configureCollection() {
        collectionView.setCollectionViewLayout(makeListLayout(), animated: false)
        collectionView.backgroundColor = .clear
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.alwaysBounceVertical = true
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.register(
            ClipboardRowCell.self,
            forCellWithReuseIdentifier: ClipboardRowCell.reuseIdentifier
        )
        collectionView.register(
            PanelSectionHeaderView.self,
            forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
            withReuseIdentifier: PanelSectionHeaderView.reuseIdentifier
        )
    }

    @objc private func accessibilityAppearanceDidChange() {
        applyPresentation()
    }

    /// A list layout purely for the swipe actions it brings: separators and
    /// background are turned off so the panel keeps drawing its own themed hairline
    /// over the shared backdrop.
    ///
    /// Built here rather than at `init` because the swipe provider needs `self`.
    func makeListLayout() -> UICollectionViewCompositionalLayout {
        var configuration = UICollectionLayoutListConfiguration(appearance: .plain)
        configuration.backgroundColor = .clear
        configuration.showsSeparators = false
        configuration.headerMode = .supplementary
        configuration.trailingSwipeActionsConfigurationProvider = { [weak self] indexPath in
            self?.trailingSwipeActions(at: indexPath)
        }
        return UICollectionViewCompositionalLayout.list(using: configuration)
    }
}
#endif
