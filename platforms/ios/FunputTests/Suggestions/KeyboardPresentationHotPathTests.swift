import KeyboardLayout
import KeyboardRenderer
import Testing
import ThemeSchema
import UIKit

@MainActor
struct KeyboardPresentationHotPathTests {
    @Test("Shift, language, and Enter updates preserve key control identity")
    func stateOnlyUpdatesDoNotRebuildControls() {
        let layout = StandardKeyboardLayouts.letters(.vni)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        let before = Set(keyControls(in: surface).map(ObjectIdentifier.init))

        var presentation = surface.presentation
        presentation.shiftState = .uppercase
        presentation.language = .english
        presentation.enterAction = .search
        surface.presentation = presentation
        surface.layoutIfNeeded()

        let after = Set(keyControls(in: surface).map(ObjectIdentifier.init))
        #expect(!before.isEmpty)
        #expect(before == after)
    }

    @Test("State-only updates preserve Liquid Glass effect instances")
    func stateOnlyUpdatesPreserveGlass() {
        guard #available(iOS 26, *) else { return }
        let surface = KeyboardSurfaceView()
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        let before = Set(effectViews(in: surface).map(ObjectIdentifier.init))

        var presentation = surface.presentation
        presentation.shiftState = .uppercase
        surface.presentation = presentation
        surface.layoutIfNeeded()

        let after = Set(effectViews(in: surface).map(ObjectIdentifier.init))
        #expect(!before.isEmpty)
        #expect(before == after)
    }

    @Test("Batched theme and background preserve key controls")
    func batchedThemeAndBackground() throws {
        let surface = KeyboardSurfaceView()
        let before = Set(keyControls(in: surface).map(ObjectIdentifier.init))
        let image = try #require(UIImage(systemName: "circle.fill"))
        var presentation = surface.presentation
        presentation.theme.material = .solid

        surface.apply(presentation: presentation, backgroundImage: image)

        #expect(surface.presentation == presentation)
        #expect(surface.backgroundImage === image)
        #expect(Set(keyControls(in: surface).map(ObjectIdentifier.init)) == before)
    }

    @Test("Layout replacement rebuilds controls only for a real change")
    func layoutReplacement() {
        let surface = KeyboardSurfaceView()
        let before = Set(keyControls(in: surface).map(ObjectIdentifier.init))
        var presentation = surface.presentation
        presentation.layout = PasswordKeyboardLayouts.pin(.vni)

        surface.apply(presentation: presentation, backgroundImage: nil)
        let replaced = Set(keyControls(in: surface).map(ObjectIdentifier.init))
        surface.apply(presentation: presentation, backgroundImage: nil)

        #expect(!before.isEmpty)
        #expect(before != replaced)
        #expect(Set(keyControls(in: surface).map(ObjectIdentifier.init)) == replaced)
    }

    private func keyControls(in view: UIView) -> [UIControl] {
        view.subviews.flatMap { child in
            let own = (child as? UIControl).map { $0.isAccessibilityElement ? [$0] : [] } ?? []
            return own + keyControls(in: child)
        }
    }

    private func effectViews(in view: UIView) -> [UIVisualEffectView] {
        view.subviews.flatMap { child in
            let own = (child as? UIVisualEffectView).map { [$0] } ?? []
            return own + effectViews(in: child)
        }
    }
}
