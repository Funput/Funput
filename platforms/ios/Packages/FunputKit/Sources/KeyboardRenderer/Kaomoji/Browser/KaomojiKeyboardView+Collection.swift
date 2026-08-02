#if canImport(UIKit)
import UIKit

extension KaomojiKeyboardView: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    public func numberOfSections(in collectionView: UICollectionView) -> Int {
        sections.count
    }

    public func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        sections[section].items.count
    }

    public func collectionView(
        _ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath
    ) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(
            withReuseIdentifier: KaomojiCell.reuseIdentifier, for: indexPath
        ) as? KaomojiCell else { return UICollectionViewCell() }
        cell.apply(
            sections[indexPath.section].items[indexPath.item],
            color: theme.label.uiColor(for: traitCollection)
        )
        return cell
    }

    public func collectionView(
        _ collectionView: UICollectionView,
        viewForSupplementaryElementOfKind kind: String,
        at indexPath: IndexPath
    ) -> UICollectionReusableView {
        guard kind == UICollectionView.elementKindSectionHeader,
              let header = collectionView.dequeueReusableSupplementaryView(
                ofKind: kind,
                withReuseIdentifier: PanelSectionHeaderView.reuseIdentifier,
                for: indexPath
              ) as? PanelSectionHeaderView
        else { return UICollectionReusableView() }
        header.apply(
            title: sections[indexPath.section].category.displayName,
            color: theme.secondaryLabel.uiColor(for: traitCollection)
        )
        return header
    }

    public func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        onKaomojiSelected?(sections[indexPath.section].items[indexPath.item])
    }

    /// Cells are as wide as their text needs, so the flow layout packs short and
    /// long kaomoji into ragged rows instead of a wasteful fixed grid.
    public func collectionView(
        _ collectionView: UICollectionView,
        layout collectionViewLayout: UICollectionViewLayout,
        sizeForItemAt indexPath: IndexPath
    ) -> CGSize {
        let item = sections[indexPath.section].items[indexPath.item]
        return CGSize(
            width: itemWidth(for: item.text, available: collectionView.bounds.width),
            height: Self.itemHeight
        )
    }

    public func scrollViewDidScroll(_ scrollView: UIScrollView) {
        guard !scrollView.isTracking || !collectionView.indexPathsForVisibleItems.isEmpty else { return }
        let top = collectionView.indexPathsForVisibleItems.min()
        guard let section = top?.section, sections.indices.contains(section) else { return }
        let category = sections[section].category
        if category != selectedCategory {
            selectedCategory = category
            bottomBar.apply(color: theme.label.uiColor(for: traitCollection), selected: category)
        }
    }

    func itemWidth(for text: String, available: CGFloat) -> CGFloat {
        let widest = max(available - Self.horizontalInset * 2, Self.minimumItemWidth)
        let measured = measuredWidth(for: text) + Self.horizontalPadding
        return min(max(measured, Self.minimumItemWidth), widest)
    }

    /// Measurements are cached because the flow layout asks for every item's size
    /// on each invalidation, and the catalog never changes at runtime.
    private func measuredWidth(for text: String) -> CGFloat {
        if let cached = measuredWidths[text] { return cached }
        let width = (text as NSString)
            .size(withAttributes: [.font: KaomojiCell.font])
            .width
        measuredWidths[text] = width
        return width
    }
}
#endif
