#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct PersonalSuggestionStressTests {
    @Test("One hundred thousand authored transitions stay ordered and bounded")
    func stress() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .vni)
        let document = TestKeyboardWriter()
        document.exposesContext = false
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
            coordinator.handle(keys[index % keys.count], writer: document)
            let update = coordinator.takePersonalSuggestionUpdate()
            let elapsed = start.duration(to: .now)
            durations.append(nanoseconds(elapsed))
            if update.completedToken != nil { completed += 1 }
        }

        durations.sort()
        let p95 = durations[94_999]
        #expect(completed == 25_000)
        #expect(document.text.count == 100_000)
        #expect(document.text.hasSuffix("ban "))
        #expect(p95 < 100_000, "main-thread bookkeeping p95 was \(p95) ns")
    }

    private func nanoseconds(_ duration: Duration) -> UInt64 {
        let components = duration.components
        let seconds = UInt64(max(0, components.seconds)) * 1_000_000_000
        let attoseconds = UInt64(max(0, components.attoseconds))
        return seconds + attoseconds / 1_000_000_000
    }
}
#endif
