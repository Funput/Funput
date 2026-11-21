import KeyboardInput
import PersonalSuggestions
import Testing

/// What the service does with words the tracker read from the document: it asks the
/// engine about them and keeps them out of what the store learns.
@MainActor
@Suite("Borrowed context in the suggestion service")
struct BorrowedContextServiceTests {
    @Test("A word typed after a borrowed one is learned with nothing before it")
    func borrowedWordsAreNotLearnedFrom() {
        let probe = ServiceProbe()
        probe.service.update(
            .init(prefix: "", completedToken: nil, context: "cảm", borrowed: true), canQuery: true
        )
        probe.service.update(.init(prefix: "", completedToken: "ơn", context: "ơn"), canQuery: true)

        #expect(probe.learned == [.init(token: "ơn", previous: nil)])
    }

    @Test("A word typed after one of Funput's own is learned as following it")
    func authoredWordsAreLearnedFrom() {
        let probe = ServiceProbe()
        probe.service.update(.init(prefix: "", completedToken: "cảm", context: "cảm"), canQuery: true)
        probe.service.update(.init(prefix: "", completedToken: "ơn", context: "ơn"), canQuery: true)

        #expect(probe.learned.last == .init(token: "ơn", previous: "cảm"))
    }

    @Test("A caret moved after other words asks anew, with no key typed")
    func movedCaretAsksAnew() async {
        let probe = ServiceProbe()
        probe.type("", after: "cảm")
        await probe.answer("ơn")

        probe.service.update(
            .init(prefix: "", completedToken: nil, context: "hôm", borrowed: true), canQuery: true
        )

        #expect(probe.queries.count == 2)
        #expect(probe.queries.last?.context == "hôm")
        #expect(probe.shown == ["ơn"], "kept until the new answer lands")

        await probe.answer("nay", "qua")
        #expect(probe.shown == ["nay", "qua"])
    }

    @Test("The same words asked again ask nothing")
    func sameWordsAskOnce() {
        let probe = ServiceProbe()
        probe.type("", after: "cảm")
        probe.type("", after: "cảm")

        #expect(probe.queries.count == 1)
    }
}
