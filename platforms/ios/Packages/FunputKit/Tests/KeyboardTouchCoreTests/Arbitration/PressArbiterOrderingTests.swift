@testable import KeyboardTouchCore
import Testing

struct PressArbiterOrderingTests {
    @Test("A sequential press emits immediately")
    func sequentialPress() {
        var arbiter = PressArbiter<String>()
        let began = arbiter.begin(contact(1), at: 0)
        #expect(began)

        let output = arbiter.resolve(contact(1), payload: "A", at: 0.01)

        #expect(emittedPayloads(output) == ["A"])
        #expect(output.first?.intentSequence == 1)
    }

    @Test("Normal rollover preserves touch-down order")
    func normalRollover() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)

        let first = arbiter.resolve(contact(1), payload: "A", at: 0.02)
        let second = arbiter.resolve(contact(2), payload: "B", at: 0.03)

        #expect(emittedPayloads(first + second) == ["A", "B"])
    }

    @Test("Reverse release inside the window preserves touch-down order")
    func reverseRelease() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(1), at: 0)
        arbiter.begin(contact(2), at: 0.01)

        let blocked = arbiter.resolve(contact(2), payload: "B", at: 0.02)
        let output = arbiter.resolve(contact(1), payload: "A", at: 0.03)

        #expect(blocked.isEmpty)
        #expect(emittedPayloads(output) == ["A", "B"])
    }

    @Test("Repeated rollover cycles keep their intent sequence")
    func repeatedRollover() {
        var arbiter = PressArbiter<String>()
        var output: [PressEmission<String>] = []
        for value in stride(from: UInt64(1), through: 20, by: 2) {
            arbiter.begin(contact(value), at: Double(value))
            arbiter.begin(contact(value + 1), at: Double(value) + 0.01)
            output += arbiter.resolve(contact(value + 1), payload: "\(value + 1)", at: 30)
            output += arbiter.resolve(contact(value), payload: "\(value)", at: 30.01)
        }

        #expect(output.map(\.intentSequence) == Array(1...20))
    }

    @Test("Begin timestamps order an unordered callback batch")
    func timestampOrdering() {
        var arbiter = PressArbiter<String>()
        arbiter.begin(contact(2), at: 0.02)
        arbiter.begin(contact(1), at: 0.01)
        let later = arbiter.resolve(contact(2), payload: "B", at: 0.03)
        let earlier = arbiter.resolve(contact(1), payload: "A", at: 0.04)

        #expect(later.isEmpty)
        #expect(emittedPayloads(earlier) == ["A", "B"])
    }
}
