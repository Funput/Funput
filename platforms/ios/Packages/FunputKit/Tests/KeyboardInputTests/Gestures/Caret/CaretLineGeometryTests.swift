#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import Testing

/// The line arithmetic behind vertical caret panning, exercised without a document.
///
/// Offsets are stated as the index the caret lands on in `before + after`, which is what
/// the proxy ends up applying.
struct CaretLineGeometryTests {
    @Test("Moving up keeps the column when the line above is long enough")
    func movesUpKeepingColumn() {
        let geometry = geometry(before: "abcdefgh\nij\nklmno", after: "p")

        let step = geometry.resolve(columns: 0, lines: -1, desiredColumn: nil)

        // Column 5 does not exist on "ij", so the caret settles on its end.
        #expect(step == .init(offset: -6, column: 5))
    }

    @Test("A remembered column survives a pass through a short line")
    func rememberedColumnSurvivesShortLine() {
        let geometry = geometry(before: "abcdefgh\nij", after: "\nklmnop")

        let step = geometry.resolve(columns: 0, lines: -1, desiredColumn: 5)

        #expect(step == .init(offset: -6, column: 5))
    }

    @Test("A diagonal drag applies its horizontal component too")
    func diagonalAppliesBothAxes() {
        let geometry = geometry(before: "abcd\nefg", after: "hi\njklmnop")

        let step = geometry.resolve(columns: 2, lines: 1, desiredColumn: nil)

        #expect(step == .init(offset: 8, column: 5))
    }

    @Test("Up on the first line stays put rather than sliding to its start")
    func upOnFirstLineStaysPut() {
        let geometry = geometry(before: "abc", after: "def\nghi")

        #expect(
            geometry.resolve(columns: 0, lines: -1, desiredColumn: nil)
                == .init(offset: 0, column: nil)
        )
    }

    @Test("Down on the last line stays put rather than sliding to its end")
    func downOnLastLineStaysPut() {
        let geometry = geometry(before: "abc\ndef", after: "gh")

        #expect(
            geometry.resolve(columns: 0, lines: 1, desiredColumn: nil)
                == .init(offset: 0, column: nil)
        )
    }

    @Test("A single-line field has nowhere to go and does not move")
    func singleLineFieldDoesNotMove() {
        let geometry = geometry(before: "hello", after: " world")

        #expect(geometry.resolve(columns: 0, lines: -1, desiredColumn: nil).offset == 0)
        #expect(geometry.resolve(columns: 0, lines: 1, desiredColumn: nil).offset == 0)
    }

    @Test("A blocked vertical step still carries its horizontal half")
    func blockedVerticalStepKeepsItsColumns() {
        let geometry = geometry(before: "abc", after: "def\nghi")

        #expect(
            geometry.resolve(columns: 2, lines: -1, desiredColumn: nil)
                == .init(offset: 2, column: nil)
        )
    }

    @Test("An empty document moves nowhere")
    func emptyDocumentStaysPut() {
        let geometry = geometry(before: "", after: "")

        #expect(geometry.resolve(columns: 0, lines: -1, desiredColumn: nil).offset == 0)
        #expect(geometry.resolve(columns: 0, lines: 1, desiredColumn: nil).offset == 0)
    }

    @Test("More lines than the document has clamps to the outermost one")
    func clampsToOutermostLine() {
        let geometry = geometry(before: "a\nbc", after: "d")

        #expect(
            geometry.resolve(columns: 0, lines: -5, desiredColumn: nil)
                == .init(offset: -3, column: 2)
        )
    }

    @Test("A horizontal-only step is the offset it was asked for, with no column")
    func horizontalStepPassesThrough() {
        let geometry = geometry(before: "abc\ndef", after: "gh")

        #expect(
            geometry.resolve(columns: -3, lines: 0, desiredColumn: 7)
                == .init(offset: -3, column: nil)
        )
    }

    private func geometry(before: String, after: String) -> CaretLineGeometry {
        CaretLineGeometry(KeyboardCaretContext(before: before, after: after))
    }
}
#endif
