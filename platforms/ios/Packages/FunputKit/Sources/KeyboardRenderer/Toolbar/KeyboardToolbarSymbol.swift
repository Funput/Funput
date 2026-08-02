#if canImport(UIKit)
import UIKit

/// Sizes the toolbar's SF Symbols so they all take up the same height.
///
/// SF Symbols take their point size from cap height rather than from the bounding
/// box, so a tall narrow glyph like a clipboard renders visibly bigger than a round
/// one like a smiley at the very same size. Scaling anything taller than the
/// reference down keeps the row even without a hand-tuned number per symbol.
enum KeyboardToolbarSymbol {
    /// A plain circle, so its bounding box and its cap height are the same thing.
    static let reference = "face.smiling"
    static let pointSize: CGFloat = 17

    static func image(_ name: String) -> UIImage? {
        let configuration = UIImage.SymbolConfiguration(pointSize: pointSize)
        let image = UIImage(systemName: name, withConfiguration: configuration)
        guard let image,
              let target = UIImage(systemName: reference, withConfiguration: configuration),
              image.size.height > target.size.height
        else { return image }
        return UIImage(
            systemName: name,
            withConfiguration: UIImage.SymbolConfiguration(
                pointSize: pointSize * target.size.height / image.size.height
            )
        )
    }
}
#endif
