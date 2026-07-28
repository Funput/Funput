@testable import KeyboardTouchCore
import Testing

struct PressArbiterProgressTests {
    private let configuration = PressArbiterConfiguration(rolloverWindow: 0.04)

    @Test("A follower progresses at the deadline without another input event")
    func deadlineProgress() {
        var arbiter = PressArbiter<String>(configuration: configuration)
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)
        let initiallyBlocked = arbiter.resolve(contact(2), payload: "B", at: 0.02)
        #expect(initiallyBlocked.isEmpty)
        #expect(arbiter.nextDeadline == 0.06)

        let beforeDeadline = arbiter.advance(to: 0.059)
        #expect(beforeDeadline.isEmpty)
        let output = arbiter.advance(to: 0.06)

        #expect(emittedPayloads(output) == ["B"])
        #expect(arbiter.nextDeadline == nil)
    }

    @Test("A detached held contact emits when it eventually resolves")
    func heldContactResolvesLater() {
        var arbiter = PressArbiter<String>(configuration: configuration)
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)
        _ = arbiter.resolve(contact(2), payload: "B", at: 0.02)
        let follower = arbiter.advance(to: 0.06)
        let held = arbiter.resolve(contact(1), payload: "A", at: 0.2)

        #expect(emittedPayloads(follower + held) == ["B", "A"])
    }

    @Test("Several held contacts detach in the same expired window")
    func severalHeldContacts() {
        var arbiter = PressArbiter<String>(configuration: configuration)
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)
        arbiter.begin(contact(3), at: 0.02)
        _ = arbiter.resolve(contact(3), payload: "C", at: 0.03)

        let output = arbiter.advance(to: 0.07)

        #expect(emittedPayloads(output) == ["C"])
        #expect(arbiter.detached.count == 2)
    }
}
