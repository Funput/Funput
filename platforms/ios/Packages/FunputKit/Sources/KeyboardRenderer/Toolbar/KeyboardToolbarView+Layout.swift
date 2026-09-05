#if canImport(UIKit)
import UIKit

extension KeyboardToolbarView {
    enum Metrics {
        /// Utility keys take the band's full height and a fixed width. The compact band
        /// has no height to give away to an inset, and a wider key buys back what the
        /// shorter band costs the tap target.
        static let controlWidth: CGFloat = 40
        /// Separates two adjacent utility keys.
        static let controlSpacing: CGFloat = 2
        /// The brand mark is decorative, so it sits inside the band instead of filling it.
        static let logoRatio: CGFloat = 0.82
        /// Keeps the shared content region clear of the logo and the utility keys.
        static let contentInset: CGFloat = 6
        /// How far above the band a tap still counts, covering the keyboard's top padding.
        /// Nothing else owns that strip: the keycap touch surface starts below the band.
        static let topTouchOutset: CGFloat = 6
    }

    /// Places the brand mark, the utility keys and the shared content region
    /// across the toolbar band.
    func layoutContents() {
        let logoSize = (bounds.height * Metrics.logoRatio).rounded()
        logoView.frame = CGRect(
            x: 0,
            y: (bounds.height - logoSize) / 2,
            width: logoSize,
            height: logoSize
        )
        // The right-hand controls stack inwards from the trailing edge, and either of
        // them can step aside — the clipboard key while the user is typing, the emoji
        // key when the layout already carries one in its rows.
        var trailing = bounds.width
        var placedAControl = false
        if !emojiButton.isHidden {
            emojiButton.frame = controlFrame(endingAt: trailing)
            trailing = emojiButton.frame.minX
            placedAControl = true
        }
        if !clipboardButton.isHidden {
            // The separator belongs *between* two controls, so it only applies when
            // something was placed before this one — otherwise the content region would
            // silently lose those points whenever the clipboard key is the one to step aside.
            clipboardButton.frame = controlFrame(
                endingAt: trailing - (placedAControl ? Metrics.controlSpacing : 0)
            )
            trailing = clipboardButton.frame.minX
        }
        // Suggestions and the clipboard chip share one region and never show at the
        // same time, so they get the same frame. It ends wherever the controls begin.
        let contentRegion = CGRect(
            x: logoView.frame.maxX + Metrics.contentInset,
            y: 0,
            width: max(0, trailing - logoView.frame.maxX - Metrics.contentInset * 2),
            height: bounds.height
        )
        suggestionBar.frame = contentRegion
        clipboardChip.frame = contentRegion
    }

    private func controlFrame(endingAt trailing: CGFloat) -> CGRect {
        CGRect(
            x: trailing - Metrics.controlWidth,
            y: 0,
            width: Metrics.controlWidth,
            height: bounds.height
        )
    }
}
#endif
