import KeyboardLayout
import KeyboardRenderer
import ThemeSchema
import UIKit

struct KeyboardLabConfiguration {
    var inputMethod: KeyboardInputMethod = .telex
    var editorMode: KeyboardEditorMode = .text
    var layoutMode: KeyboardLayoutMode = .letters
    var language: KeyboardLanguage = .vietnamese
    var enterAction = KeyboardLabEnterAction.newLine
    var showsSystemInputModeKey = false
    var heightScale = 1.0
    var keyGap = 5.0
    var cornerRadius = 10.0
    var keyOpacity = 0.72

    static let `default` = KeyboardLabConfiguration()

    var resolvedLayoutMode: KeyboardLayoutMode {
        editorMode.usesKeypad ? .letters : layoutMode
    }

    var supportsLayoutModeSelection: Bool {
        !editorMode.usesKeypad
    }

    var layout: KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: inputMethod,
            mode: resolvedLayoutMode,
            editorMode: editorMode,
            showsSystemInputModeKey: showsSystemInputModeKey
        )
    }

    var supportsLanguageSwipe: Bool {
        layout.rows.flatMap(\.keys).contains { $0.horizontalSwipeAction == .toggleLanguage }
    }

    var supportsSystemInputModePreview: Bool {
        layout.toolbar != nil
    }

    var presentation: KeyboardPresentation {
        var sizing = KeyboardSizingProfile.default
        sizing.heightScale = CGFloat(heightScale)
        sizing.horizontalGap = CGFloat(keyGap)
        sizing.verticalGap = CGFloat(keyGap + 2)

        var theme = KeyboardThemeTokens.funputGlass
        theme.cornerRadius = cornerRadius
        theme.keyOpacity = keyOpacity
        theme.specialKeyOpacity = min(1, keyOpacity + 0.1)

        return KeyboardPresentation(
            layout: layout,
            sizing: sizing,
            theme: theme,
            shiftState: .lowercase,
            language: language,
            enterAction: enterAction.value
        )
    }
}
