import KeyboardInput
import KeyboardLayout
import PersonalSuggestions
import FunputShared
import Testing

@MainActor
struct PersonalSuggestionStressTests {
    @Test("One hundred thousand latest requests use one bounded drain")
    func queryCoalescing() {
        let slot = PersonalSuggestionQuerySlot()
        var schedules = 0
        for generation in 0..<100_000 {
            if slot.submit(.init(prefix: "ba", generation: UInt64(generation))) {
                schedules += 1
            }
        }
        #expect(schedules == 1)
        #expect(slot.takeLatest()?.generation == 99_999)
        #expect(slot.takeLatest() == nil)
    }

    @Test("One hundred thousand token transitions stay ordered")
    func tokenOrdering() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = ScriptedWriter()
        let keys = [
            KeySpec(id: "b", label: "b", role: .character),
            KeySpec(id: "a", label: "a", role: .character),
            KeySpec(id: "n", label: "n", role: .character),
            KeySpec(id: "space", label: " ", role: .space),
        ]
        var completed = 0
        var durations = [UInt64]()
        durations.reserveCapacity(100_000)
        for index in 0..<100_000 {
            let start = ContinuousClock.now
            coordinator.handle(keys[index % 4], writer: document)
            let update = coordinator.takePersonalSuggestionUpdate()
            durations.append(nanoseconds(start.duration(to: .now)))
            if update.completedToken != nil { completed += 1 }
        }
        durations.sort()
        #expect(completed == 25_000)
        #expect(document.text.count == 100_000)
        #expect(durations[94_999] < 100_000)
    }

    @Test("Enabled bookkeeping keeps median typing within three percent")
    func typingRegression() {
        let baseline = measureTyping(enabled: false)
        let enabled = measureTyping(enabled: true)
        #expect(
            Double(enabled) <= Double(baseline) * 1.03,
            "median enabled \(enabled) ns, baseline \(baseline) ns"
        )
    }

    private func measureTyping(enabled: Bool) -> UInt64 {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = enabled
        coordinator.apply(configuration)
        let document = ScriptedWriter()
        let keys = [
            KeySpec(id: "b", label: "b", role: .character),
            KeySpec(id: "a", label: "a", role: .character),
            KeySpec(id: "n", label: "n", role: .character),
            KeySpec(id: "space", label: " ", role: .space),
        ]
        var values = [UInt64]()
        values.reserveCapacity(20_000)
        for index in 0..<21_000 {
            let start = ContinuousClock.now
            coordinator.handle(keys[index % 4], writer: document)
            _ = coordinator.takePersonalSuggestionUpdate()
            if index >= 1_000 { values.append(nanoseconds(start.duration(to: .now))) }
        }
        values.sort()
        return values[values.count / 2]
    }

    private func nanoseconds(_ duration: Duration) -> UInt64 {
        let value = duration.components
        return UInt64(max(0, value.seconds)) * 1_000_000_000
            + UInt64(max(0, value.attoseconds)) / 1_000_000_000
    }
}
