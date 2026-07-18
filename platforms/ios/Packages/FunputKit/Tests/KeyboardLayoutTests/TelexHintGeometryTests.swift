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
}
