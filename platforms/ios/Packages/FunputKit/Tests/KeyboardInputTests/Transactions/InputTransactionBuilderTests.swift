#if os(iOS) && canImport(FunputCore)
import Testing
@testable import KeyboardInput

struct InputTransactionBuilderTests {
    @Test("Empty and invalid mutations are omitted")
    func filtersEmptyMutations() {
        var builder = InputTransactionBuilder()
        builder.deleteBackward(count: 0)
        builder.deleteBackward(count: -1)
        builder.insert("")
        #expect(builder.isEmpty)
    }

    @Test("Adjacent mutations coalesce without reordering")
    func coalescesAdjacentMutations() {
        var builder = InputTransactionBuilder()
        builder.deleteBackward(count: 1)
        builder.deleteBackward(count: 2)
        builder.insert("a")
        builder.insert("b")
        builder.deleteBackward()

        #expect(builder.mutations == [
            .deleteBackward(count: 3),
            .insert("ab"),
            .deleteBackward(count: 1),
        ])
    }
}
#endif
