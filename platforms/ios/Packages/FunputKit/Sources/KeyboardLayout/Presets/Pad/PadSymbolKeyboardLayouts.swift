/// The two symbol pages of the stock iPad keyboard.
///
/// ```
/// tab    1 2 3 4 5 6 7 8 9 0   delete      tab    [ ] { } # % ^ * + =   delete
/// T/V    @ # ₫ & * ( ) ' "     return      T/V    _ \ | ~ < > $ € ¥     return
/// #+=    % - + = / ; : , .     #+=         123    • … § ! ? ¿ ¡ , .     123
/// globe  ABC    space    ABC   hide        globe  ABC    space    ABC   hide
/// ```
///
/// The stock undo and redo keys have no counterpart a third-party keyboard can drive, so
/// their slot holds the Telex/VNI switch where the field composes and stays empty
/// elsewhere. The pages keep four rows but are budgeted for the letters page's height.
enum PadSymbolKeyboardLayouts {
    private typealias Keys = PadKeyFactory

    static func primary(_ page: PadPage) -> KeyboardLayout {
        layout(
            "primary",
            page: page,
            rows: PadSymbolPageContent.primaryRows,
            switchKey: (label: "#+=", role: .moreSymbols, accessibility: "Thêm ký hiệu")
        )
    }

    static func secondary(_ page: PadPage) -> KeyboardLayout {
        layout(
            "secondary",
            page: page,
            rows: PadSymbolPageContent.secondaryRows,
            switchKey: (label: "123", role: .symbols, accessibility: "Số và ký hiệu")
        )
    }

    private static func layout(
        _ name: String,
        page: PadPage,
        rows content: [[String]],
        switchKey: (label: String, role: KeyRole, accessibility: String)
    ) -> KeyboardLayout {
        let symbols = content.enumerated().map { row, labels in
            labels.enumerated().map { index, label in
                KeySpec(id: "symbol-pad-\(name)-\(row)-\(index)", label: label, role: .punctuation)
            }
        }
        let switchKey = { (id: String, placement: KeyLabelPlacement) in
            Keys.edgeKey(
                id, switchKey.label, switchKey.role, switchKey.accessibility,
                weight: Keys.wide, placement: placement
            )
        }
        return KeyboardLayout(
            id: "symbols-pad-\(name)\(page.editorID)-\(page.inputMethod.rawValue)"
                + (page.isCompact ? "-compact" : ""),
            inputMethod: page.inputMethod,
            toolbar: page.toolbar,
            rows: [
                KeyboardRow(keys: [Keys.leadingKey("tab", "tab", .tab, "Tab")] + symbols[0] + [Keys.deleteKey()]),
                KeyboardRow(keys: [inputMethodKey(page)] + symbols[1] + [Keys.returnKey()]),
                KeyboardRow(keys: [switchKey("switch", .bottomLeading)] + symbols[2]
                    + [switchKey("switch-right", .bottomTrailing)]),
                Keys.actionRow(
                    switchLabel: "ABC",
                    switchRole: .letters,
                    switchAccessibility: "Chữ cái",
                    composes: page.composes
                ),
            ],
            heightRowCount: page.letterRowCount
        )
    }

    /// The Telex/VNI switch, labelled with the method in use; the page is rebuilt when it
    /// changes. Fields that do not compose have nothing for it to switch.
    private static func inputMethodKey(_ page: PadPage) -> KeySpec {
        guard page.composes else { return Keys.spacer("inputmethod", weight: Keys.wide) }
        let name = page.inputMethod.isTelexFamily ? "Telex" : "VNI"
        return Keys.leadingKey(
            "inputmethod",
            name,
            .inputMethod,
            "Kiểu gõ \(name). Chạm để đổi",
            weight: Keys.wide
        )
    }
}
