#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
struct KeyboardSurfaceBatchTests {
    @Test("Initializer builds the final layout and background once")
    func initialFinalState() throws {
        let layout = PasswordKeyboardLayouts.pin(.vni)
        let image = try #require(UIImage(systemName: "circle.fill"))
        let presentation = KeyboardPresentation(layout: layout)

        let surface = KeyboardSurfaceView(
            presentation: presentation,
            backgroundImage: image
        )

        #expect(surface.presentation == presentation)
        #expect(surface.backgroundImage === image)
        #expect(surface.keyRebuildCount == 1)
        #expect(surface.presentationApplyCount == 1)
        #expect(Set(surface.keyControls.keys) == Set(layout.rows.flatMap(\.keys).map(\.id)))
    }

    @Test("Theme and image reactivation use one presentation pass")
    func batchedThemeAndImage() throws {
        let surface = KeyboardSurfaceView()
        let rebuilds = surface.keyRebuildCount
        let applies = surface.presentationApplyCount
        let image = try #require(UIImage(systemName: "square.fill"))
        var next = surface.presentation
        next.theme.material = .solid

        surface.apply(presentation: next, backgroundImage: image)

        #expect(surface.keyRebuildCount == rebuilds)
        #expect(surface.presentationApplyCount == applies + 1)
        #expect(surface.backgroundImage === image)
    }

    @Test("State-only changes preserve controls and layout swaps rebuild once")
    func rebuildPolicy() {
        let surface = KeyboardSurfaceView()
        let initialControls = surface.keyControls.mapValues(ObjectIdentifier.init)
        var stateOnly = surface.presentation
        stateOnly.shiftState = .uppercase

        surface.apply(presentation: stateOnly, backgroundImage: nil)

        #expect(surface.keyControls.mapValues(ObjectIdentifier.init) == initialControls)
        let rebuilds = surface.keyRebuildCount
        var layoutChange = stateOnly
        layoutChange.layout = NumberKeyboardLayouts.resolve(.vni, mode: .numberDecimal)
        surface.apply(presentation: layoutChange, backgroundImage: nil)
        #expect(surface.keyRebuildCount == rebuilds + 1)

        let finalControls = surface.keyControls.mapValues(ObjectIdentifier.init)
        surface.apply(presentation: layoutChange, backgroundImage: nil)
        #expect(surface.keyRebuildCount == rebuilds + 1)
        #expect(surface.keyControls.mapValues(ObjectIdentifier.init) == finalControls)
    }
}
#endif
