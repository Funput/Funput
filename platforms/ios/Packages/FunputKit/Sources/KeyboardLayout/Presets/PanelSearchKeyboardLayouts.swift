import Foundation

/// The letters page for a search field that lives inside a keyboard panel (emoji search
/// today), shaped by the input method so it composes like the document does.
///
/// VNI brings its modifier digit row, Telex its key hints. There is no toolbar and no
/// symbols page: the bottom row leaves the panel, spaces or closes the keyboard.
public enum PanelSearchKeyboardLayouts {
    public static func letters(
        _ inputMethod: KeyboardInputMethod,
        spaceLabel: String,
        doneLabel: String = "Xong"
    ) -> KeyboardLayout {
        let pageID = "panel-search-\(inputMethod.rawValue)"
        return qwertyLayout(
            id: pageID,
            inputMethod: inputMethod,
            leadingRows: inputMethod == .vni ? [topNumberRow(for: .vni, pageID: pageID)] : [],
            actionKeys: [
                specialKey("emoji", "", .emoji, weight: 1.7, accessibilityLabel: "Thoát tìm kiếm"),
                KeySpec(
                    id: "space", label: spaceLabel, role: .space,
                    widthWeight: 5.8, accessibilityLabel: "Dấu cách"
                ),
                specialKey("done", doneLabel, .enter, weight: 1.7, accessibilityLabel: doneLabel),
            ],
            showsTelexHints: true,
            showsToolbar: false
        )
    }
}
