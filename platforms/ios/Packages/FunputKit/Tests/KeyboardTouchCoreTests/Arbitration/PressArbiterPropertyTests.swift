@testable import KeyboardTouchCore
import Testing

struct PressArbiterPropertyTests {
    @Test("One hundred thousand transitions preserve exactly-once delivery")
    func deterministicStress() {
        var arbiter = PressArbiter<String>(
            configuration: PressArbiterConfiguration(rolloverWindow: 0.004)
        )
        var random = DeterministicRandom(seed: 0xF0_2026)
        var active: [ContactID] = []
        var resolved = Set<ContactID>()
        var emissions: [PressEmission<String>] = []
        var nextID: UInt64 = 1
        var time = 0.0

        for _ in 0..<100_000 {
            time += 0.001
            if active.isEmpty || active.count < 5 && random.next(upTo: 3) == 0 {
                let id = contact(nextID)
                nextID += 1
                let began = arbiter.begin(id, at: time)
                #expect(began)
                active.append(id)
            } else {
                let index = random.next(upTo: active.count)
                let id = active.remove(at: index)
                if random.next(upTo: 4) == 0 {
                    emissions += arbiter.cancel(id, at: time)
                } else {
                    resolved.insert(id)
                    emissions += arbiter.resolve(id, payload: "\(id.rawValue)", at: time)
                }
            }
            if random.next(upTo: 5) == 0 {
                emissions += arbiter.advance(to: time)
            }
        }

        for id in active {
            resolved.insert(id)
            time += 0.001
            emissions += arbiter.resolve(id, payload: "\(id.rawValue)", at: time)
        }
        emissions += arbiter.advance(to: time + 1)

        let emittedIDs = emissions.map(\.contactID)
        #expect(Set(emittedIDs) == resolved)
        #expect(emittedIDs.count == resolved.count)
        #expect(Set(emissions.map(\.intentSequence)).count == emissions.count)
        #expect(arbiter.nextDeadline == nil)
    }
}
