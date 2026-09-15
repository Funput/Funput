import CoreGraphics
import KeyboardTouchCore
import Testing

/// Which key a press commits when the finger moves between landing and lifting.
@Suite("Contact resolver lift")
struct ContactResolverLiftTests {
    /// A finger that lands on one key and lifts over its neighbour types the neighbour, as
    /// Apple's keyboard does: seeing the wrong key highlighted and sliding onto the right one
    /// is how people correct a press on iOS.
    @Test func slideToCorrectUsesTerminalHit() {
        var resolver = ContactResolver<String>(
            configuration: .init(tapSlop: 100, maximumTapDuration: 1)
        )
        _ = resolver.consume(contactSample(1, .began, at: 0), hit: "A")
        _ = resolver.consume(
            contactSample(1, .moved, at: 0.05, point: CGPoint(x: 20, y: 0)),
            hit: "B"
        )
        #expect(
            resolver.consume(
                contactSample(1, .ended, at: 0.1, point: CGPoint(x: 20, y: 0)),
                hit: "B"
            ) == .resolved(
                .init(rawValue: 1),
                "B",
                .init(exceededTapSlop: false)
            )
        )
    }

    /// No margin is held back for a roll on the way up: the stock keyboard switches keys as
    /// soon as the lift point crosses into the neighbour, so a small drift that does cross
    /// commits the neighbour here too.
    @Test func driftUnderTheTapSlopCommitsTheKeyUnderTheLift() {
        var resolver = ContactResolver<String>()
        _ = resolver.consume(contactSample(1, .began, at: 0), hit: "A")
        #expect(
            resolver.consume(
                contactSample(1, .ended, at: 0.05, point: CGPoint(x: 12, y: 0)),
                hit: "B"
            ) == .resolved(
                .init(rawValue: 1),
                "B",
                .init(exceededTapSlop: false)
            )
        )
    }

    @Test func driftThatStaysOnTheKeyKeepsIt() {
        var resolver = ContactResolver<String>()
        _ = resolver.consume(contactSample(1, .began, at: 0), hit: "A")
        _ = resolver.consume(
            contactSample(1, .moved, at: 0.03, point: CGPoint(x: 12, y: 0)),
            hit: "B"
        )
        #expect(
            resolver.consume(
                contactSample(1, .ended, at: 0.05, point: CGPoint(x: 4, y: 0)),
                hit: "A"
            ) == .resolved(
                .init(rawValue: 1),
                "A",
                .init(exceededTapSlop: false)
            )
        )
    }
}
