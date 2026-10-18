import CoreGraphics
import KeyboardLayout
import Testing

struct PadLabelPlacementTests {
    @Test("Typing keys sit on the bottom line, function keys keep their corners")
    func placements() {
        for mode in KeyboardLayoutMode.allCases {
            let keys = pad(mode: mode).rows.flatMap(\.keys)
            for key in keys where [.character, .punctuation, .vniModifier].contains(key.role) {
                #expect(key.labelPlacement == .bottomCenter, "\(key.id)")
            }
            for key in keys where [.tab, .backspace, .enter, .shift, .capsLock].contains(key.role) {
                #expect(key.labelPlacement.isCorner, "\(key.id)")
            }
        }
    }

    @Test("Globe and emoji sit bottom-left, hide sits bottom-right, with or without the toolbar")
    func symbolCorners() {
        for mode in KeyboardLayoutMode.allCases {
            for showsToolbar in [true, false] {
                let action = pad(mode: mode, showsToolbar: showsToolbar).rows.last?.keys ?? []
                let corners = Dictionary(action.map { ($0.role, $0.labelPlacement) }) { first, _ in first }
                #expect(corners[.globe] == .bottomLeading, "\(mode)")
                #expect(corners[.dismissKeyboard] == .bottomTrailing, "\(mode)")
                #expect(corners[.emoji] == (showsToolbar ? nil : .bottomLeading), "\(mode)")
            }
        }
    }

    @Test("Compact digit hints keep the bottom line")
    func compactDigits() {
        let top = pad(showsNumberRow: false).rows[0].keys.filter { $0.role == .character }
        #expect(top.allSatisfy { $0.labelPlacement == .bottomCenter && $0.secondaryLabel != nil })
    }

    @Test("The left shift is longer than the right and the rows still add up")
    func shiftWidths() {
        let layout = pad(showsNumberRow: false)
        let shifts = layout.rows[2].keys.filter { $0.role == .shift }
        #expect(shifts.count == 2)
        #expect(shifts[0].widthWeight > shifts[1].widthWeight)
        let totals = layout.rows.prefix(3).map { $0.keys.map(\.widthWeight).reduce(0, +) }
        #expect(totals.allSatisfy { abs($0 - totals[0]) < 0.001 })
    }

    @Test("The phone and the Funput preset keep centered labels")
    func othersUnchanged() {
        for showsToolbar in [true, false] {
            let phone = KeyboardLayoutResolver.resolve(
                inputMethod: .telex, mode: .letters, preset: .system, showsToolbar: showsToolbar, formFactor: .phone
            )
            let funput = KeyboardLayoutResolver.resolve(
                inputMethod: .telex, mode: .letters, preset: .funput, showsToolbar: showsToolbar, formFactor: .pad
            )
            for layout in [phone, funput] {
                #expect(layout.rows.flatMap(\.keys).allSatisfy { $0.labelPlacement == .center })
            }
        }
    }

    private func pad(
        mode: KeyboardLayoutMode = .letters,
        showsNumberRow: Bool = true,
        showsToolbar: Bool = true
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: mode,
            showsNumberRow: showsNumberRow,
            preset: .system,
            showsToolbar: showsToolbar,
            formFactor: .pad
        )
    }
}
