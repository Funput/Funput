import Testing
import ThemeRuntime
import ThemeSchema
import UIKit
@testable import KeyboardRenderer

@MainActor
@Suite("iOS system backdrop")
struct iOSSystemBackdropTests {
    private let traits = UITraitCollection(userInterfaceStyle: .dark)

    @Test("Transparent system theme borrows the keyboard host backdrop")
    func hostBackdrop() throws {
        let backdrop = KeyboardBackdropView()
        backdrop.apply(theme: ThemeRuntime.resolve(.iosSystem), traits: traits)
        let themedView = try #require(backdrop.contentView.superview)

        #expect(backdrop.usesHostMaterial)
        #expect(backdrop.effect == nil)
        #expect(themedView.isHidden)
    }

    @Test("Regular translucent themes retain their authored backdrop")
    func authoredBackdrop() throws {
        let backdrop = KeyboardBackdropView()
        backdrop.apply(theme: ThemeRuntime.resolve(.iosSystem), traits: traits)
        backdrop.apply(theme: ThemeRuntime.resolve(.classicLight), traits: traits)
        let themedView = try #require(backdrop.contentView.superview)

        #expect(!backdrop.usesHostMaterial)
        #expect(backdrop.effect is UIBlurEffect)
        #expect(!themedView.isHidden)
    }
}
