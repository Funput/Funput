#if os(iOS)
import PersonalSuggestions
import Testing

@Suite("Latest-value slot")
struct LatestValueSlotTests {
    @Test("Keeps only the newest value, and taking it empties the slot")
    func keepsNewestOnce() {
        let slot = LatestValueSlot<Int>()
        #expect(slot.take() == nil)
        slot.submit(1)
        slot.submit(2)
        #expect(slot.take() == 2)
        #expect(slot.take() == nil)
    }
}
#endif
