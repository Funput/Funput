import KeyboardLayout
import Testing

@Suite("Panel search keyboard layout")
struct PanelSearchKeyboardLayoutTests {
    @Test("Telex keeps four rows with its key hints")
    func telex() {
        let layout = PanelSearchKeyboardLayouts.letters(.telex, spaceLabel: "Tìm emoji")
        #expect(layout.toolbar == nil)
        #expect(layout.rows.count == 4)
        #expect(layout.rows[0].keys.map(\.label).joined() == "qwertyuiop")
        #expect(layout.rows[0].keys.first { $0.label == "r" }?.secondaryLabel != nil)
        #expect(layout.rows[2].keys.first?.role == .shift)
        #expect(layout.rows[2].keys.last?.role == .backspace)
        #expect(layout.rows[3].keys.map(\.role) == [.emoji, .space, .enter])
        #expect(layout.rows[3].keys[1].label == "Tìm emoji")
    }

    @Test("VNI adds its modifier digit row")
    func vni() {
        let layout = PanelSearchKeyboardLayouts.letters(.vni, spaceLabel: "Tìm emoji")
        #expect(layout.rows.count == 5)
        #expect(layout.rows[0].isNumberRow)
        #expect(layout.rows[0].keys.allSatisfy { $0.role == .vniModifier })
        #expect(layout.rows[1].keys.map(\.label).joined() == "qwertyuiop")
        #expect(layout.rows[1].keys.allSatisfy { $0.secondaryLabel == nil })
    }

    @Test("Each method gets its own page identity")
    func identity() {
        let ids = KeyboardInputMethod.allCases.map {
            PanelSearchKeyboardLayouts.letters($0, spaceLabel: "").id
        }
        #expect(Set(ids).count == ids.count)
    }
}
