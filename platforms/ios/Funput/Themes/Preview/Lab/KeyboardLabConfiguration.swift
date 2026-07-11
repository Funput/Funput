import KeyboardLayout
import KeyboardRenderer
import ThemeSchema
import UIKit

struct KeyboardLabConfiguration {
    var heightScale: Double
    var keyGap: Double
    var cornerRadius: Double
    var keyOpacity: Double

    static let `default` = KeyboardLabConfiguration(
        heightScale: 1,
        keyGap: 5,
        cornerRadius: 10,
        keyOpacity: 0.72
    )

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
            layout: .funputQWERTY,
            sizing: sizing,
            theme: theme,
            shiftState: .lowercase,
            showsInputModeKey: true
        )
    }
}
