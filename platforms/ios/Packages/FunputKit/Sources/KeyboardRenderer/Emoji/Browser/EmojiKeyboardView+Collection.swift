#if canImport(UIKit)
import UIKit

extension EmojiKeyboardView: UICollectionViewDataSource, UICollectionViewDelegate {
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
            withReuseIdentifier: EmojiCell.reuseIdentifier, for: indexPath
        ) as? EmojiCell else { return UICollectionViewCell() }
        cell.apply(sections[indexPath.section].items[indexPath.item])
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
            title: sections[indexPath.section].category.accessibilityLabel,
            color: theme.secondaryLabel.uiColor(for: traitCollection)
        )
        return header
    }

    public func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let section = sections[indexPath.section]
        let item = section.items[indexPath.item]
        if section.category == .recent {
            (onRecentEmojiSelected ?? onEmojiSelected)?(item)
        } else {
            onEmojiSelected?(item)
        }
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
}
#endif
