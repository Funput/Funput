#if canImport(UIKit)
import UIKit

@MainActor
final class EmojiCell: UICollectionViewCell {
    static let reuseIdentifier = "EmojiCell"
    let glyphLayer = EmojiGlyphLayer()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.layer.addSublayer(glyphLayer)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        glyphLayer.frame = contentView.bounds
    }

    func apply(_ item: EmojiItem) {
        glyphLayer.glyph = item.glyph
        isAccessibilityElement = true
        accessibilityLabel = item.localizedName ?? item.name
        accessibilityTraits = .keyboardKey
    }
}
#endif
