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
