#if canImport(UIKit)
import Foundation
import KeyboardLayout

enum EmojiSearchKeyboardLayout {
    static let layout = KeyboardLayout(
        id: "emoji-search-qwerty",
        inputMethod: .telex,
        toolbar: nil,
        rows: [
            row("qwertyuiop"),
            row("asdfghjkl", inset: 0.5),
            KeyboardRow(keys: [special("shift", .shift, weight: 1.5)]
                + keys("zxcvbnm")
                + [special("backspace", .backspace, weight: 1.5)]),
            KeyboardRow(keys: [
                special("emoji", .emoji, weight: 1.7),
                KeySpec(
                    id: "space", label: "Tìm emoji", role: .space,
                    widthWeight: 5.8, accessibilityLabel: "Dấu cách"
                ),
                special("done", .enter, weight: 1.7, label: "Xong"),
            ]),
        ]
    )

    private static func row(_ value: String, inset: CGFloat = 0) -> KeyboardRow {
        KeyboardRow(keys: keys(value), horizontalInsetUnits: inset)
    }

    private static func keys(_ value: String) -> [KeySpec] {
        value.map { character in
            let label = String(character)
            return KeySpec(
                id: "search-\(label)", label: label, role: .character,
                shiftedLabel: label.uppercased(), accessibilityLabel: label.uppercased()
            )
        }
    }

    private static func special(
        _ id: String, _ role: KeyRole, weight: CGFloat, label: String = ""
    ) -> KeySpec {
        KeySpec(
            id: "search-\(id)", label: label, role: role,
            widthWeight: weight, accessibilityLabel: label.isEmpty ? id : label
        )
    }
}
#endif
