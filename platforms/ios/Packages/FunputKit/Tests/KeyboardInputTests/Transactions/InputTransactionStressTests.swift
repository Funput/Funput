#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct InputTransactionStressTests {
    @Test("One hundred thousand actions never duplicate a transaction sequence")
    func deterministicActions() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        let writer = TestKeyboardWriter()
        var mutatedActionCount = 0

        for index in 0..<100_000 {
            let role: KeyRole
            switch index % 10 {
            case 0: role = .shift
            case 1: role = .symbols
            case 2: role = .letters
            case 3: role = .backspace
            case 4: role = .space
            default: role = .character
            }
            let before = writer.transactions.count
            coordinator.handle(testKey(role, label: "a"), writer: writer)
            let produced = writer.transactions.count - before
            #expect(produced == 0 || produced == 1)
            mutatedActionCount += produced
        }

        #expect(writer.transactions.count == mutatedActionCount)
        #expect(
            writer.transactions.map(\.sequence)
                == Array(1...UInt64(mutatedActionCount))
        )
        #expect(writer.transactions.allSatisfy { !$0.mutations.isEmpty })
    }
}
#endif
