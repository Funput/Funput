import CoreGraphics
import KeyboardLayout
import Testing

struct GeometryVariantTests {
    @Test("All layout variants stay valid")
    func completeGeometryMatrix() {
        for method in KeyboardInputMethod.allCases {
            for editor in KeyboardEditorMode.allCases {
                verify(resolve(method, .letters, editor))
            }
            verify(resolve(method, .symbolsPrimary, .text))
            verify(resolve(method, .symbolsSecondary, .text))
            verify(resolve(method, .symbolsPrimary, .password))
            verify(resolve(method, .symbolsSecondary, .password))
        }
    }

    @Test("All system-preset variants stay valid")
    func systemPresetGeometryMatrix() {
        for method in KeyboardInputMethod.allCases {
            for editor in KeyboardEditorMode.allCases where editor.usesSystemPreset {
                for mode in KeyboardLayoutMode.allCases {
                    for showsNumberRow in [true, false] {
                        verify(resolve(
                            method,
                            mode,
                            editor,
                            showsNumberRow: showsNumberRow,
                            preset: .system
                        ))
                    }
                }
            }
        }
    }

    @Test("Layouts without toolbar fill the entire key surface")
    func layoutsWithoutToolbarUseFullSurface() {
        let layouts = [
            resolve(.telex, .letters, .phone),
            resolve(.telex, .letters, .pin),
            resolve(.telex, .letters, .number),
            resolve(.telex, .letters, .numberDecimal),
            resolve(.telex, .letters, .numberSigned),
            resolve(.telex, .letters, .numberSignedDecimal),
            resolve(.telex, .letters, .password),
            resolve(.telex, .symbolsPrimary, .password),
            resolve(.telex, .symbolsSecondary, .password),
        ]
        for layout in layouts {
            let result = geometry(layout)
            #expect(result.toolbarFrame == nil)
            #expect(result.rows[0][0].frame.minY == KeyboardSizingProfile.default.verticalPadding)
            let expectedBottom = result.size.height - KeyboardSizingProfile.default.verticalPadding
            let actualBottom = result.rows.last?[0].frame.maxY ?? 0
            #expect(abs(actualBottom - expectedBottom) <= 0.5)
        }
    }

    @Test("Layouts with toolbar reserve toolbar geometry")
    func layoutsWithToolbarReserveToolbarSpace() {
        for mode in [KeyboardLayoutMode.letters, .symbolsPrimary, .symbolsSecondary] {
            let result = geometry(resolve(.vni, mode, .text))
            #expect(result.toolbarFrame != nil)
            #expect(result.rows[0][0].frame.minY > KeyboardSizingProfile.default.verticalPadding)
        }
    }

    @Test("Placeholders preserve equal keypad slots")
    func placeholderSlots() {
        let result = geometry(resolve(.telex, .letters, .pin))
        for row in result.rows {
            #expect(Set(row.map { $0.frame.width }).count == 1)
        }
        #expect(result.keys.filter { $0.spec.role == .placeholder }.count == 4)
    }

    private func resolve(
        _ method: KeyboardInputMethod,
        _ mode: KeyboardLayoutMode,
        _ editor: KeyboardEditorMode,
        showsNumberRow: Bool = true,
        preset: KeyboardLayoutPreset = .funput
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: mode,
            editorMode: editor,
            showsNumberRow: showsNumberRow,
            preset: preset
        )
    }

    private func geometry(_ layout: KeyboardLayout) -> ResolvedKeyboard {
        KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: 390, height: layout.rows.count == 4 ? 204 : 304),
            sizing: .default
        )
    }

    private func verify(_ layout: KeyboardLayout) {
        let result = geometry(layout)
        #expect(Set(result.keys.map { $0.spec.id }).count == result.keys.count)
        #expect(result.keys.allSatisfy { $0.frame.width > 0 && $0.frame.height > 0 })
        #expect(result.keys.allSatisfy { $0.frame.minX >= 0 && $0.frame.maxX <= result.size.width + 0.5 })
        for row in result.rows {
            for pair in zip(row, row.dropFirst()) {
                #expect(pair.0.frame.maxX <= pair.1.frame.minX)
            }
        }
    }
}
