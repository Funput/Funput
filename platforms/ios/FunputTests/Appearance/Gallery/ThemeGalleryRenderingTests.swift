import SwiftUI
import Testing
import ThemeRuntime
import UIKit
@testable import Funput

@MainActor
struct ThemeGalleryRenderingTests {
    @Test("Gallery cards do not mount production keyboard renderers")
    func galleryCardUsesLightweightThumbnail() {
        let card = ThemeCard(
            theme: .funputGlass,
            resolvedTheme: ThemeRuntime.resolve(.funputGlass),
            backgroundImageData: nil,
            interfaceStyle: .light,
            isPreviewed: true,
            isApplied: true,
            isCustom: false,
            editAction: {},
            deleteAction: {},
            action: {}
        )
        let host = UIHostingController(rootView: card)

        host.view.frame = CGRect(x: 0, y: 0, width: 390, height: 300)
        host.view.layoutIfNeeded()

        #expect(!containsKeyboardPreview(in: host.view))
    }

    /// A card that grew to the height it was offered pinned the custom theme menu to the
    /// top of that space, above the visible card, so Edit and Delete could not be reached.
    @Test("A custom theme card keeps its own height, so its menu stays on the card")
    func customCardDoesNotGrowToOfferedHeight() {
        let card = ThemeCard(
            theme: .funputGlass,
            resolvedTheme: ThemeRuntime.resolve(.funputGlass),
            backgroundImageData: nil,
            interfaceStyle: .dark,
            isPreviewed: false,
            isApplied: false,
            isCustom: true,
            editAction: {},
            deleteAction: {},
            action: {}
        )
        let host = UIHostingController(rootView: card)

        let size = host.sizeThatFits(in: CGSize(width: 402, height: 2000))

        #expect(size.height < 400)
    }

    @Test("Passive content cards do not install visual effects")
    func contentCardUsesSolidSurface() {
        let host = UIHostingController(rootView: ContentCard { Text("Content") })

        host.view.frame = CGRect(x: 0, y: 0, width: 390, height: 200)
        host.view.layoutIfNeeded()

        #expect(!containsVisualEffect(in: host.view))
    }

    private func containsKeyboardPreview(in view: UIView) -> Bool {
        view is KeyboardPreviewSurface
            || view.subviews.contains(where: containsKeyboardPreview)
    }

    private func containsVisualEffect(in view: UIView) -> Bool {
        view is UIVisualEffectView
            || view.subviews.contains(where: containsVisualEffect)
    }
}
