@_spi(Testing) import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardSpaceInteractionTests {
    @Test("A Space swipe emits one language action and no Space release")
    func spaceSwipeSuppressesRelease() {
        let driver = KeyboardTouchTestDriver()
        driver.begin(token: 1, key: space)
        driver.move(token: 1, key: space, point: CGPoint(x: 40, y: 0))
        driver.end(token: 1)

        #expect(driver.events.filter { $0.phase == .swiped(.toggleLanguage) }.count == 1)
        #expect(!driver.events.contains { $0.phase == .released })
        #expect(driver.queueDepth == 0)
    }

    @Test("Accessibility Space swipe emits exactly one semantic action")
    func accessibilitySpaceSwipe() {
        let driver = KeyboardTouchTestDriver()
        driver.performAccessibilitySwipe(key: space, action: .toggleLanguage)

        #expect(driver.events.map(\.phase) == [.swiped(.toggleLanguage)])
        #expect(driver.queueDepth == 0)
    }

    @Test("Holding Space repeats and does not add an extra Space on release")
    func spaceHoldRepeats() {
        let driver = KeyboardTouchTestDriver()
        driver.begin(token: 1, key: space)
        driver.runNextRepeat()
        driver.runNextRepeat()
        driver.end(token: 1)

        #expect(driver.events.map(\.phase) == [.pressed, .repeated, .repeated])
        #expect(driver.queueDepth == 0)
    }

    @Test("A repeated Space cannot also toggle language")
    func repeatedSpaceSuppressesLateSwipe() {
        let driver = KeyboardTouchTestDriver()
        driver.begin(token: 1, key: space)
        driver.runNextRepeat()
        driver.move(token: 1, key: space, point: CGPoint(x: 40, y: 0))

        #expect(driver.events.map(\.phase) == [.pressed, .repeated])
        #expect(driver.queueDepth == 0)
    }

    private var space: KeySpec {
        KeySpec(
            id: "space",
            label: "Tiếng Việt",
            role: .space,
            horizontalSwipeAction: .toggleLanguage
        )
    }
}
