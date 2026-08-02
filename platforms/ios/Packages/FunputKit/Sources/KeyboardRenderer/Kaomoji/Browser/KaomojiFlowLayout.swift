#if canImport(UIKit)
import UIKit

/// Flow layout that packs each row from the left instead of justifying it.
///
/// `UICollectionViewFlowLayout` spreads a row's leftover width across the gaps
/// between its items. With uniform emoji cells that is invisible, but kaomoji
/// widths vary enormously: a row holding two wide items has far more leftover
/// space than a row holding five narrow ones, so every row ends up with a
/// different, arbitrary-looking gap.
///
/// Only the horizontal origin is rewritten here — the line breaking super
/// computed is already correct, because flow layout fills each row greedily
/// before it justifies.
final class KaomojiFlowLayout: UICollectionViewFlowLayout {
    override func layoutAttributesForElements(in rect: CGRect) -> [UICollectionViewLayoutAttributes]? {
        super.layoutAttributesForElements(in: rect)?.compactMap { attributes in
            guard attributes.representedElementCategory == .cell else { return attributes }
            return layoutAttributesForItem(at: attributes.indexPath)
        }
    }

    override func layoutAttributesForItem(at indexPath: IndexPath) -> UICollectionViewLayoutAttributes? {
        guard let attributes = super.layoutAttributesForItem(at: indexPath)?
            .copy() as? UICollectionViewLayoutAttributes
        else { return nil }
        guard indexPath.item > 0 else {
            attributes.frame.origin.x = sectionInset.left
            return attributes
        }
        let previousPath = IndexPath(item: indexPath.item - 1, section: indexPath.section)
        guard let previous = layoutAttributesForItem(at: previousPath) else { return attributes }
        let startsNewRow = attributes.frame.minY >= previous.frame.maxY
        attributes.frame.origin.x = startsNewRow
            ? sectionInset.left
            : previous.frame.maxX + minimumInteritemSpacing
        return attributes
    }
}
#endif
