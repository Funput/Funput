import SwiftUI

/// Where the haptic sheet's options sit.
///
/// On iOS 26 the sheet itself is Liquid Glass, so the rows go straight onto it: an opaque
/// card on top reads as grey on grey. Older systems, and Reduce Transparency, get an opaque
/// grouped sheet with a card, like a Settings list.
enum HapticFeedbackSurface {
    static func isGlass(reduceTransparency: Bool) -> Bool {
        guard #available(iOS 26, *) else { return false }
        return !reduceTransparency
    }
}

extension View {
    /// Opaque grouped background for the fallback; the system glass otherwise.
    @ViewBuilder
    func hapticFeedbackSheetBackground(reduceTransparency: Bool) -> some View {
        if HapticFeedbackSurface.isGlass(reduceTransparency: reduceTransparency) {
            self
        } else if #available(iOS 16.4, *) {
            presentationBackground(Color(uiColor: .systemGroupedBackground))
        } else {
            background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
        }
    }
}
