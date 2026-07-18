@_spi(Testing) import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardTouchPipelineTests {
    @Test("A touch commits the key under the finger at release")
    func movesBetweenKeys() {
        let driver = KeyboardTouchTestDriver()
        let a = key("a"), b = key("b")

        driver.begin(token: 1, key: a)
        driver.move(token: 1, key: b, point: CGPoint(x: 20, y: 0))
        driver.end(token: 1)

        #expect(driver.events.last?.phase == .released)
        #expect(driver.events.last?.key.id == b.id)
    }

    @Test("Ending outside the tracking region cancels instead of committing")
    func outsideReleaseCancels() {
        let driver = KeyboardTouchTestDriver()
        let a = key("a")

        driver.begin(token: 1, key: a)
        driver.move(token: 1, key: nil, point: CGPoint(x: -100, y: -100))
        driver.end(token: 1)

        #expect(driver.events.last?.phase == .cancelled)
        #expect(!driver.events.contains { $0.phase == .released })
    }

    @Test("Missing terminal event is reconciled without blocking successors")
    func orphanedTouchDoesNotBlock() {
        let driver = KeyboardTouchTestDriver()
        let a = key("a"), b = key("b")

        driver.begin(token: 1, key: a)
        driver.begin(token: 2, key: b)
        driver.reconcile(activeTokens: [2])
        driver.end(token: 2)

        #expect(driver.events.suffix(2).map(\.phase) == [.cancelled, .released])
        #expect(driver.events.suffix(2).map(\.key.id) == [a.id, b.id])
        #expect(driver.queueDepth == 0)
    }

    @Test("One hundred thousand overlapping transitions preserve ordering")
    func hundredThousandTransitions() {
        let driver = KeyboardTouchTestDriver()
        let a = key("a"), b = key("b")
        let cycles = 25_000

        for cycle in 0..<cycles {
            let first = UInt64(cycle * 2 + 1)
            let second = first + 1
            driver.begin(token: first, key: a)
            driver.begin(token: second, key: b)
            driver.end(token: second)
            driver.end(token: first)
        }

        let releases = driver.events.lazy.filter { $0.phase == .released }.map(\.key.id)
        #expect(releases.count == cycles * 2)
        #expect(Array(releases.prefix(4)) == [a.id, b.id, a.id, b.id])
        #expect(driver.queueDepth == 0)
    }

    @Test("Geometry assigns horizontal and vertical gaps to the nearest key")
    func gapHitTesting() {
        let a = key("a"), b = key("b")
        let geometry = ResolvedKeyboard(
            size: CGSize(width: 100, height: 50),
            toolbarFrame: nil,
            rows: [[
                ResolvedKey(spec: a, frame: CGRect(x: 0, y: 0, width: 40, height: 40)),
                ResolvedKey(spec: b, frame: CGRect(x: 50, y: 0, width: 40, height: 40)),
            ]]
        )
        #expect(KeyboardTouchTestDriver.hitKey(in: geometry, at: CGPoint(x: 44, y: 20))?.id == a.id)
        #expect(KeyboardTouchTestDriver.hitKey(in: geometry, at: CGPoint(x: 46, y: 20))?.id == b.id)
        #expect(KeyboardTouchTestDriver.hitKey(in: geometry, at: CGPoint(x: -10, y: 20))?.id == a.id)
        #expect(KeyboardTouchTestDriver.hitKey(in: geometry, at: CGPoint(x: -13, y: 20)) == nil)
    }

    private func key(_ label: String) -> KeySpec {
        KeySpec(id: "key-\(label)", label: label, role: .character)
    }
}
