#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

/// A layout change hands the surface's controls to the new layout instead of
/// building new ones. A reused control has to be indistinguishable from one built
/// for the key it now is.
@MainActor
@Suite("Key control reuse")
struct KeyboardKeyReuseTests {
    private let letters = StandardKeyboardLayouts.letters(.telex)
    private let symbols = SymbolKeyboardLayouts.primary(.telex)
    private let moreSymbols = SymbolKeyboardLayouts.secondary(.telex)
    private let pin = PasswordKeyboardLayouts.pin(.telex)

    @Test("Paging through the layouts builds no control after the first round")
    func pagingBuildsNothingNew() {
        let surface = makeSurface(letters)
        [symbols, moreSymbols, letters].forEach { show($0, on: surface) }
        let built = surface.keyPool.controls.count
        let controls = Set(surface.keyPool.controls.map(ObjectIdentifier.init))

        [symbols, moreSymbols, letters, pin, letters].forEach { show($0, on: surface) }

        #expect(surface.keyPool.controls.count == built)
        #expect(Set(surface.keyPool.controls.map(ObjectIdentifier.init)) == controls)
        #expect(built == [letters, symbols, moreSymbols].map(keyCount).max())
    }

    @Test("A reused control is the key its new spec describes", arguments: [0, 1, 2])
    func reusedControlMatchesAFreshOne(target: Int) {
        let layout = [symbols, moreSymbols, pin][target]
        let surface = makeSurface(letters)
        show(layout, on: surface)

        #expect(Set(surface.keyControls.keys) == Set(layout.rows.flatMap(\.keys).map(\.id)))
        for spec in layout.rows.flatMap(\.keys) {
            let reused = surface.keyControls[spec.id]
            let fresh = KeyboardKeyControl(spec: spec)
            fresh.apply(presentation: surface.presentation, traits: surface.traitCollection)
            #expect(reused?.role == spec.role)
            #expect(reused?.accessibilityLabel == fresh.accessibilityLabel)
            #expect(reused?.isHidden == fresh.isHidden)
            #expect(reused?.isUserInteractionEnabled == fresh.isUserInteractionEnabled)
            #expect(reused?.isAccessibilityElement == fresh.isAccessibilityElement)
            #expect(reused.map(labels) == labels(in: fresh))
            #expect(
                reused?.accessibilityCustomActions?.map(\.name)
                    == fresh.accessibilityCustomActions?.map(\.name)
            )
            #expect((reused?.swipeRecognizer?.isEnabled ?? false) == (spec.horizontalSwipeAction != nil))
        }
    }

    @Test("A keypad's blank cells turn back into keys")
    func placeholdersComeBack() {
        let surface = makeSurface(pin)
        show(letters, on: surface)
        for spec in letters.rows.flatMap(\.keys) {
            let control = surface.keyControls[spec.id]
            #expect(control?.isHidden == false)
            #expect(control?.isUserInteractionEnabled == true)
            #expect(control?.isAccessibilityElement == true)
        }
    }

    @Test("Controls a smaller layout does not need are parked, not shown")
    func surplusControlsAreParked() {
        let surface = makeSurface(letters)
        show(pin, on: surface)
        let inUse = Set(surface.keyControls.values.map(ObjectIdentifier.init))
        let parked = surface.keyPool.controls.filter { !inUse.contains(ObjectIdentifier($0)) }

        #expect(parked.count == keyCount(letters) - keyCount(pin))
        #expect(parked.allSatisfy { $0.isHidden && !$0.isUserInteractionEnabled && !$0.isAccessibilityElement })
    }

    @Test("A key held through the layout change does not stay pressed as another key")
    func pressedStateDoesNotCarryOver() {
        let surface = makeSurface(letters)
        surface.keyPool.controls.forEach { $0.setPressed(true, presentation: surface.presentation) }
        show(symbols, on: surface)
        #expect(surface.keyControls.values.allSatisfy { $0.surface.transform == .identity })
    }

    private func makeSurface(_ layout: KeyboardLayout) -> KeyboardSurfaceView {
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()
        return surface
    }

    private func show(_ layout: KeyboardLayout, on surface: KeyboardSurfaceView) {
        var presentation = surface.presentation
        presentation.layout = layout
        surface.apply(presentation: presentation, backgroundImage: nil)
        surface.layoutIfNeeded()
    }

    private func keyCount(_ layout: KeyboardLayout) -> Int {
        layout.rows.flatMap(\.keys).count
    }

    private func labels(in view: UIView) -> [String] {
        view.subviews.flatMap { child -> [String] in
            let own = (child as? UILabel).flatMap { $0.isHidden ? nil : $0.text }.map { [$0] } ?? []
            return own + labels(in: child)
        }
    }
}
#endif
