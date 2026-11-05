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
        self.hasFullAccess = hasFullAccess
        emptyState = hasFullAccess ? .nothingSaved : .needsFullAccess
        groups = hasFullAccess ? ClipboardHistorySection.make(from: entries) : []
        applyPasteHint()
        applyPresentation()
    }

    /// What the paste row may say, worked out by the keyboard alongside the toolbar's
    /// own offer. Without full access nothing pasted could be kept, so there is no row.
    public func updatePasteHint(_ hint: KeyboardClipboardHint?) {
        pasteHint = hint
        applyPasteHint()
    }

    private func applyPasteHint() {
        pasteChip.update(hint: hasFullAccess ? pasteHint : nil)
        setNeedsLayout()
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
        if #available(iOS 17, *) {
            registerForTraitChanges([UITraitUserInterfaceStyle.self]) { (view: Self, _) in
                view.applyPresentation()
            }
        }
        clipsToBounds = true
        backgroundColor = .clear
        configureCollection()
        bottomBar.onReturn = { [weak self] in self?.onReturn?() }
        bottomBar.onDelete = { [weak self] in self?.onDelete?() }
        bottomBar.onClearAll = { [weak self] in self?.onClearAll?() }
        pasteChip.update(hint: nil)
        pasteChip.onPaste = { [weak self] in self?.onPaste?($0) }
        [backdropView, collectionView, emptyStateView, bottomBar, pasteChip].forEach(addSubview)
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
        pasteChip.apply(theme: theme, traits: traitCollection)
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

    public override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #unavailable(iOS 17) { applyPresentation() }
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
