@testable import KeyboardTouchCore
import Testing

struct PressArbiterCancellationTests {
    @Test("Cancelling the head unblocks a resolved follower")
    func cancellationUnblocks() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)
        _ = arbiter.resolve(contact(2), payload: "B", at: 0.02)

        let output = arbiter.cancel(contact(1), at: 0.03)

        #expect(emittedPayloads(output) == ["B"])
    }

    @Test("Cancellation never emits a payload")
    func cancellationDoesNotEmit() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(1), at: 0)

        let cancellation = arbiter.cancel(contact(1), at: 0.01)
        let later = arbiter.advance(to: 1)
        #expect(cancellation.isEmpty)
        #expect(later.isEmpty)
    }

    @Test("Duplicate and unknown contacts are harmless")
    func invalidTransitionsAreIgnored() {
        var arbiter = PressArbiter<String>()
        let began = arbiter.begin(contact(1), at: 0)
        let duplicate = arbiter.begin(contact(1), at: 0.01)
        let unknownResolve = arbiter.resolve(contact(99), payload: "X", at: 0.02)
        let unknownCancel = arbiter.cancel(contact(99), at: 0.03)
        let output = arbiter.resolve(contact(1), payload: "A", at: 0.04)
        #expect(began)
        #expect(!duplicate)
        #expect(unknownResolve.isEmpty)
        #expect(unknownCancel.isEmpty)
        #expect(emittedPayloads(output) == ["A"])
    }

    @Test("Reset clears pending state without reusing intent sequences")
    func resetClearsState() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(1), at: 0)
        arbiter.reset()
        arbiter.begin(contact(2), at: 1)

        let output = arbiter.resolve(contact(2), payload: "B", at: 1.01)

        #expect(output.first?.intentSequence == 2)
        let stale = arbiter.resolve(contact(1), payload: "A", at: 2)
        #expect(stale.isEmpty)
    }

    @Test("Equal timestamps still emit every payload exactly once")
    func equalTimestamps() {
        var arbiter = PressArbiter<String>()
        for value in UInt64(1)...5 { arbiter.begin(contact(value), at: 0) }
        var output: [PressEmission<String>] = []
        for value in (UInt64(1)...5).reversed() {
            output += arbiter.resolve(contact(value), payload: "\(value)", at: 0.01)
        }
        output += arbiter.advance(to: 1)

        #expect(Set(emittedPayloads(output)) == Set(["1", "2", "3", "4", "5"]))
        #expect(output.count == 5)
    }

    @Test("A duplicate terminal event cannot revoke a resolved follower")
    func duplicateTerminalDoesNotRevoke() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)
        _ = arbiter.resolve(contact(2), payload: "B", at: 0.02)

        let duplicateResolve = arbiter.resolve(contact(2), payload: "X", at: 0.03)
        let duplicateCancel = arbiter.cancel(contact(2), at: 0.04)
        let output = arbiter.cancel(contact(1), at: 0.05)

        #expect(duplicateResolve.isEmpty)
        #expect(duplicateCancel.isEmpty)
        #expect(emittedPayloads(output) == ["B"])
    }
}
