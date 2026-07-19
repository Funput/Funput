#if os(iOS)
import PersonalSuggestions
import Testing

@Suite("Suggestion query coalescing")
struct PersonalSuggestionQuerySlotTests {
    @Test("One hundred thousand requests remain one bounded drain")
    func stress() {
        let slot = PersonalSuggestionQuerySlot()
        var schedules = 0
        for generation in 0..<100_000 {
            let request = PersonalSuggestionQueryRequest(
                prefix: "pr\(generation)",
                generation: UInt64(generation)
            )
            if slot.submit(request) { schedules += 1 }
        }
        #expect(schedules == 1)
        #expect(slot.takeLatest()?.generation == 99_999)
        #expect(slot.takeLatest() == nil)
        #expect(slot.submit(.init(prefix: "next", generation: 100_000)))
    }

    @Test("Detects a request that supersedes active work")
    func superseding() {
        let slot = PersonalSuggestionQuerySlot()
        _ = slot.submit(.init(prefix: "ba", generation: 1))
        let active = slot.takeLatest()
        #expect(active?.generation == 1)
        _ = slot.submit(.init(prefix: "ban", generation: 2))
        #expect(slot.hasNewer(than: 1))
    }
}
#endif
