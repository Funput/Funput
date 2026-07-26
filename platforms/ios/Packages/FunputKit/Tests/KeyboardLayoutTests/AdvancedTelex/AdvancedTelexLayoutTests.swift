import CoreGraphics
import KeyboardLayout
import Testing

struct AdvancedTelexLayoutTests {
    @Test("Advanced Telex shares compact Telex geometry and hints")
    func compactGeometryAndHints() {
        let telex = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            showsNumberRow: false
        )
        let advanced = KeyboardLayoutResolver.resolve(
            inputMethod: .telexAdvanced,
            mode: .letters,
            showsNumberRow: false
        )

        #expect(advanced.rows.count == 4)
        #expect(advanced.rows.flatMap(\.keys).first { $0.label == "s" }?.secondaryLabel == "´")
        #expect(frames(telex) == frames(advanced))
    }

    @Test("Advanced Telex keeps brackets on the second symbol page")
    func bracketPlacement() {
        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: .telexAdvanced,
            mode: .symbolsSecondary,
            showsNumberRow: false
        )
        let labels = layout.rows.flatMap(\.keys).map(\.label)

        #expect(labels.contains("["))
        #expect(labels.contains("]"))
        #expect(!KeyboardLayoutResolver.resolve(
            inputMethod: .telexAdvanced,
            mode: .letters,
            showsNumberRow: false
        ).rows.flatMap(\.keys).map(\.label).contains("["))
    }

    private func frames(_ layout: KeyboardLayout) -> [CGRect] {
        KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: 390, height: 204),
            sizing: .default
        ).keys.map(\.frame)
    }
}
