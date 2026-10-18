#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import KeyboardLayout
import Testing

/// Pulling down on an iPad key types the hint printed above it, while a hold still opens
/// the alternates. `GestureTestSubject` keys are 44pt tall, so a full pull is 20pt.
@MainActor
struct PadSwipeDownGestureTests {
    private static let padKey = KeySpec(
        id: "q", label: "q", role: .character, secondaryLabel: "1",
        alternates: [KeyAlternate(text: "1")], labelPlacement: .bottomCenter
    )

    private func selected(_ subject: GestureTestSubject) -> [String] {
        subject.phases.compactMap {
            if case let .alternateSelected(value) = $0 { value.text } else { nil }
        }
    }

    @Test("A full pull types the hint")
    func fullPull() {
        let subject = GestureTestSubject(key: Self.padKey)
        subject.begin()
        subject.move(to: 120, y: 240)
        subject.controller.endTouch(token: 1)
        #expect(subject.claims == [.alternate])
        #expect(selected(subject) == ["1"])
        #expect(subject.swipeProgress.first == 1 && subject.swipeProgress.last == 0)
    }

    @Test("The preview bubble steps aside while the key is pulled, and returns if the pull is let go")
    func previewHidden() {
        let subject = GestureTestSubject(key: Self.padKey)
        subject.begin()
        subject.move(to: 120, y: 238)
        #expect(subject.previews.last == .some(nil))
        subject.move(to: 120, y: 221)
        #expect(subject.previews.last == "q")
    }

    @Test("A short pull leaves the letter to the pipeline")
    func shortPull() {
        let subject = GestureTestSubject(key: Self.padKey)
        subject.begin()
        subject.move(to: 120, y: 238)
        subject.controller.endTouch(token: 1)
        #expect(subject.claims.isEmpty)
        #expect(selected(subject).isEmpty)
        #expect(subject.swipeProgress.last == 0)
    }

    @Test("Backing out after a full pull types the letter")
    func backOut() {
        let subject = GestureTestSubject(key: Self.padKey)
        subject.begin()
        subject.move(to: 120, y: 240)
        subject.move(to: 120, y: 224)
        subject.controller.endTouch(token: 1)
        #expect(selected(subject) == ["q"])
    }

    @Test("A refused claim drops the pull and types nothing extra")
    func refusedClaim() {
        let subject = GestureTestSubject(key: Self.padKey)
        subject.grantsClaims = false
        subject.begin()
        subject.move(to: 120, y: 240)
        subject.controller.endTouch(token: 1)
        #expect(selected(subject).isEmpty)
        #expect(subject.swipeProgress.last == 0)
    }

    @Test("Holding still still opens the alternates")
    func holdStillOpensPalette() {
        let subject = GestureTestSubject(key: Self.padKey)
        subject.begin()
        subject.scheduler.fire(after: 0.5)
        subject.controller.endTouch(token: 1)
        #expect(subject.claims == [.alternate])
        #expect(selected(subject) == ["1"])
        #expect(subject.swipeProgress.isEmpty)
    }

    @Test("A phone key ignores the pull")
    func phoneKey() {
        let phone = KeySpec(id: "q", label: "q", role: .character, secondaryLabel: "1")
        let subject = GestureTestSubject(key: phone)
        subject.begin()
        subject.move(to: 120, y: 240)
        subject.controller.endTouch(token: 1)
        #expect(subject.claims.isEmpty)
        #expect(subject.swipeProgress.isEmpty)
    }
}
#endif
