import CoreGraphics
import KeyboardLayout
import Testing

struct TelexHintGeometryTests {
    @Test("Telex hint metadata does not change character hit frames")
    func hitFramesStayStable() {
        let standard = StandardKeyboardLayouts.letters(.telex)
        let specialized = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            editorMode: .search
        )
        let size = CGSize(width: 390, height: 280)
        let standardGeometry = KeyboardGeometry.resolve(
            layout: standard,
            size: size,
            sizing: .default
        )
        let specializedGeometry = KeyboardGeometry.resolve(
            layout: specialized,
            size: size,
            sizing: .default
        )

        for label in ["s", "f", "r", "x", "j", "z"] {
            let standardKey = standardGeometry.keys.first { $0.spec.label == label }
            let specializedKey = specializedGeometry.keys.first { $0.spec.label == label }
            #expect(standardKey?.frame == specializedKey?.frame)
            #expect(standardKey?.spec.role == .character)
            #expect(standardKey?.spec.widthWeight == 1)
        }
    }

    @Test("Digit hints do not change compact character hit frames")
    func compactHitFramesStayStable() {
        let size = CGSize(width: 390, height: 240)
        let compact = KeyboardGeometry.resolve(
            layout: StandardKeyboardLayouts.letters(.telex, showsNumberRow: false),
            size: size,
            sizing: .default
        )
        let bare = KeyboardGeometry.resolve(
            layout: undecoratedCompactLayout(),
            size: size,
            sizing: .default
        )

        for label in "qwertyuiop".map(String.init) {
            let decorated = compact.keys.first { $0.spec.label == label }
            let plain = bare.keys.first { $0.spec.label == label }
            #expect(decorated?.frame == plain?.frame)
            #expect(decorated?.spec.role == .character)
            #expect(decorated?.spec.widthWeight == 1)
        }
    }

    /// The same page with the decorator's contribution stripped back out, so the
    /// comparison isolates hint and alternate metadata from every other difference.
    private func undecoratedCompactLayout() -> KeyboardLayout {
        let layout = StandardKeyboardLayouts.letters(.telex, showsNumberRow: false)
        return KeyboardLayout(
            id: layout.id,
            inputMethod: layout.inputMethod,
            toolbar: layout.toolbar,
            rows: layout.rows.map { row in
                KeyboardRow(
                    keys: row.keys.map {
                        KeySpec(
                            id: $0.id,
                            label: $0.label,
                            role: $0.role,
                            widthWeight: $0.widthWeight,
                            shiftedLabel: $0.shiftedLabel,
                            horizontalSwipeAction: $0.horizontalSwipeAction
                        )
                    },
                    horizontalInsetUnits: row.horizontalInsetUnits
                )
            }
        )
    }
}
