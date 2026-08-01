import KeyboardTouchCore
import Testing

@Suite("Contact resolver stress")
struct ContactResolverStressTests {
    @Test func oneHundredThousandDeterministicTransitions() {
        var resolver = ContactResolver<Int>()
        var resolved = 0
        var cancelled = 0

        for transition in 0..<100_000 {
            let rawID = UInt64(transition % 5 + 1)
            let cycle = transition / 5
            let timestamp = Double(transition) / 1_000
            let phase: ContactPhase
            switch cycle % 4 {
            case 0: phase = .began
            case 1: phase = .moved
            case 2: phase = .ended
            default: phase = .cancelled
            }
            let result = resolver.consume(
                contactSample(rawID, phase, at: timestamp),
                hit: Int(rawID)
            )
            switch result {
            case .resolved: resolved += 1
            case .cancelled: cancelled += 1
            case .began, .noOp: break
            }
        }

        #expect(resolved + cancelled > 0)
        resolver.reset()
        #expect(resolver.activeContactCount == 0)
    }
}
