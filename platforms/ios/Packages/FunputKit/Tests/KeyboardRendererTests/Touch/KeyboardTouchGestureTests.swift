#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@MainActor
struct KeyboardTouchGestureTests {
    @Test("Repeat uses its own lane and suppresses base release")
    func repeatLane() {
        let fixture = KeyboardTouchFixture.singleKey(key("delete", .backspace))
        fixture.begin()
        fixture.coordinator.claim(token: 1, kind: .repeatKey)
        let repeated = fixture.event(.repeated)
        #expect(fixture.coordinator.handleInteraction(token: 1, event: repeated) != nil)
        fixture.coordinator.consume(fixture.sample(.ended, at: 0.5))
        _ = fixture.coordinator.handleInteraction(
            token: 1,
            event: fixture.event(.cancelled)
        )
        fixture.coordinator.finishUIKitContact(1)
        #expect(fixture.output.map(\.phase) == [])
        #expect(fixture.coordinator.metrics.repeatEmitted == 1)
        #expect(fixture.coordinator.pendingContactCount == 0)
    }

    @Test("Alternate and swipe are terminal touch actions")
    func alternateAndSwipe() {
        let alternate = KeyboardTouchFixture.singleKey(key("a", .character))
        alternate.begin()
        alternate.coordinator.claim(token: 1, kind: .alternate)
        _ = alternate.coordinator.handleInteraction(
            token: 1,
            event: alternate.event(.alternateSelected(.init(text: "á")))
        )
        alternate.coordinator.finishUIKitContact(1)
        #expect(alternate.output.map(\.phase) == [.alternateSelected(.init(text: "á"))])
        #expect(alternate.coordinator.metrics.alternateCommitted == 1)

        let swipe = KeyboardTouchFixture.singleKey(
            key("space", .space, swipe: .toggleLanguage)
        )
        swipe.begin()
        swipe.coordinator.claim(token: 1, kind: .swipe)
        _ = swipe.coordinator.handleInteraction(
            token: 1,
            event: swipe.event(.swiped(.toggleLanguage))
        )
        swipe.coordinator.finishUIKitContact(1)
        #expect(swipe.output.map(\.phase) == [.swiped(.toggleLanguage)])
        #expect(swipe.coordinator.metrics.swipeCommitted == 1)
    }

    @Test("Every control role releases exactly once")
    func controls() {
        let roles: [KeyRole] = [
            .shift, .backspace, .symbols, .moreSymbols, .letters,
            .inputMethod, .systemInputMode, .enter, .emoji,
        ]
        for role in roles {
            let fixture = KeyboardTouchFixture.singleKey(key(role.rawValue, role))
            fixture.begin()
            fixture.coordinator.consume(fixture.sample(.ended, at: 0.1))
            fixture.coordinator.finishUIKitContact(1)
            #expect(fixture.output.map(\.phase) == [.released])
            #expect(fixture.coordinator.metrics.committedContacts == 1)
        }
    }

    @Test("Callbacks for a claimed contact are expected, not regressions")
    func claimedContactCallbacks() {
        let fixture = KeyboardTouchFixture.singleKey(key("a", .character))
        fixture.begin()
        fixture.coordinator.claim(token: 1, kind: .alternate)
        fixture.coordinator.consume(fixture.sample(.moved, at: 0.1))
        fixture.coordinator.consume(fixture.sample(.ended, at: 0.2))

        #expect(fixture.coordinator.metrics.resolverUnknownCallback == 0)
        #expect(!fixture.coordinator.metrics.hasRegression)
    }

    @Test("A claim the pipeline already committed is refused")
    func refusedClaim() {
        let fixture = KeyboardTouchFixture.singleKey(key("a", .character))
        fixture.begin()
        fixture.coordinator.consume(fixture.sample(.ended, at: 0.1))
        fixture.coordinator.finishUIKitContact(1)

        #expect(fixture.coordinator.claim(token: 1, kind: .alternate) == false)
        #expect(fixture.coordinator.metrics.gestureConflict == 1)
    }

    private func key(
        _ id: String,
        _ role: KeyRole,
        swipe: KeySwipeAction? = nil
    ) -> KeySpec {
        KeySpec(id: id, label: id, role: role, horizontalSwipeAction: swipe)
    }
}
#endif
