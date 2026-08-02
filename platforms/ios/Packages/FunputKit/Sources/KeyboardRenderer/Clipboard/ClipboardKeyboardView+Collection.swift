#if canImport(UIKit)
import UIKit

extension ClipboardKeyboardView: UICollectionViewDataSource, UICollectionViewDelegate {
    public func numberOfSections(in collectionView: UICollectionView) -> Int {
        groups.count
    }

    public func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        groups[section].entries.count
    }

    public func collectionView(
        _ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath
    ) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(
            withReuseIdentifier: ClipboardRowCell.reuseIdentifier, for: indexPath
        ) as? ClipboardRowCell, let entry = entry(at: indexPath) else {
            return UICollectionViewCell()
        }
        cell.apply(
            entry: entry,
            now: now,
            theme: theme,
            traits: traitCollection,
            showsSeparator: indexPath.item < groups[indexPath.section].entries.count - 1
        )
        cell.onTogglePin = { [weak self] in self?.onTogglePin?(entry) }
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
            title: groups[indexPath.section].title,
            color: theme.secondaryLabel.uiColor(for: traitCollection)
        )
        return header
    }

    public func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        entry(at: indexPath).map { onSelect?($0) }
    }

    /// Swipe from the trailing edge to delete one entry.
    ///
    /// Exposed as a method rather than buried in the layout closure so a test can
    /// hold it to account without synthesising a swipe.
    func trailingSwipeActions(at indexPath: IndexPath) -> UISwipeActionsConfiguration? {
        guard let entry = entry(at: indexPath) else { return nil }
        let delete = UIContextualAction(style: .destructive, title: "Xoá") { [weak self] _, _, done in
            self?.onRemove?(entry)
            done(true)
        }
        delete.image = UIImage(systemName: "trash")
        return UISwipeActionsConfiguration(actions: [delete])
    }

    /// Pinning has a visible button on the row; deleting one entry lives here so a
    /// destructive action needs a deliberate press rather than sitting a finger-width
    /// from "paste this".
    public func collectionView(
        _ collectionView: UICollectionView,
        contextMenuConfigurationForItemsAt indexPaths: [IndexPath],
        point: CGPoint
    ) -> UIContextMenuConfiguration? {
        guard let indexPath = indexPaths.first, let entry = entry(at: indexPath) else { return nil }
        return UIContextMenuConfiguration(identifier: nil, previewProvider: nil) { [weak self] _ in
            UIMenu(children: [
                UIAction(
                    title: entry.isPinned ? "Bỏ ghim" : "Ghim",
                    image: UIImage(systemName: entry.isPinned ? "pin.slash" : "pin")
                ) { _ in self?.onTogglePin?(entry) },
                UIAction(
                    title: "Xoá",
                    image: UIImage(systemName: "trash"),
                    attributes: .destructive
                ) { _ in self?.onRemove?(entry) },
            ])
        }
    }
}
#endif
