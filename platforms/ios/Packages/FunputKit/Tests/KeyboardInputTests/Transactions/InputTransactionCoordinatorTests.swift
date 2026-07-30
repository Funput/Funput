#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct InputTransactionCoordinatorTests {
    @Test("Composition replacement is one ordered transaction")
    func compositionReplacement() throws {
        let coordinator = KeyboardInputCoordinator()
        let writer = TestKeyboardWriter()

        type("a", with: coordinator, into: writer)
        type("1", role: .vniModifier, with: coordinator, into: writer)

        let transaction = try #require(writer.transactions.last)
        #expect(transaction.mutations == [
            .deleteBackward(count: 1),
            .insert("á"),
        ])
        #expect(transaction.resultingState == coordinator.state)
        #expect(writer.text == "á")
    }

    @Test("State-only actions do not allocate transactions")
    func stateOnlyAction() {
        let coordinator = KeyboardInputCoordinator()
        let writer = TestKeyboardWriter()

        let effects = coordinator.handle(testKey(.shift), writer: writer)

        #expect(writer.transactions.isEmpty)
        #expect(effects.presentationChanged)
        #expect(!effects.suggestionsChanged)
    }

    @Test("Suggestion replacement is one atomic transaction")
    func suggestionReplacement() {
        let coordinator = KeyboardInputCoordinator()
        let writer = TestKeyboardWriter()
        type("ban", with: coordinator, into: writer)
        let countBeforeAcceptance = writer.transactions.count

        let effects = coordinator.acceptSuggestion(
            "bạn",
            replacing: "ban",
            writer: writer
        )

        #expect(effects != nil)
        #expect(writer.transactions.count == countBeforeAcceptance + 1)
        #expect(writer.transactions.last?.mutations == [
            .deleteBackward(count: 3),
            .insert("bạn "),
        ])
        #expect(writer.text == "bạn ")
    }

    @Test("Sequences are unique and monotonic per mutated action")
    func monotonicSequence() {
        let coordinator = KeyboardInputCoordinator()
        let writer = TestKeyboardWriter()

        for _ in 0..<1_000 {
            coordinator.handle(testKey(.space, label: " "), writer: writer)
        }

        #expect(writer.transactions.count == 1_000)
        #expect(writer.transactions.map(\.sequence) == Array(1...1_000))
        #expect(writer.transactions.allSatisfy { !$0.mutations.isEmpty })
    }
}
#endif
