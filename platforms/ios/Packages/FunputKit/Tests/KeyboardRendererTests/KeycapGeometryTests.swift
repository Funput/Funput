#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
struct KeycapGeometryTests {
    @Test("Keycap height changes while the interaction area stays full-size")
    func visualHeightDoesNotShrinkHitArea() throws {
        let key = KeyboardLayout.funputQWERTY.rows[1].keys[0]
        let control = KeyboardKeyControl(spec: key)
        control.frame = CGRect(x: 0, y: 0, width: 38, height: 52)
        var theme = ResolvedTheme.funputGlass
        theme.keycapHeightScale = 0.82
        control.apply(
            presentation: KeyboardPresentation(theme: theme),
            traits: UITraitCollection(userInterfaceStyle: .light)
        )
        control.layoutIfNeeded()

        let surface = try #require(control.subviews.first { $0 is KeyboardKeySurfaceView })
        let hitArea = try #require(control.subviews.first {
            $0 is UIControl && !($0 is KeyboardKeySurfaceView)
        })
        #expect(abs(surface.frame.height - 42.64) < 0.01)
        #expect(surface.frame.midY == control.bounds.midY)
        #expect(hitArea.frame == control.bounds)
    }
}
#endif
