import CoreGraphics
import KeyboardTouchCore
import Testing

@Suite("Contact resolver")
struct ContactResolverTests {
    @Test func resolvesSequentialTap() {
        var resolver = ContactResolver<String>()
        #expect(resolver.consume(contactSample(1, .began, at: 0), hit: "A") == .began(.init(rawValue: 1)))
        #expect(
            resolver.consume(contactSample(1, .ended, at: 0.1), hit: "A")
                == .resolved(.init(rawValue: 1), "A")
        )
        #expect(resolver.activeContactCount == 0)
    }

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
            ) == .resolved(.init(rawValue: 1), "B")
        )
    }

    @Test func cancelsWanderedLongAndOutsideTaps() {
        var resolver = ContactResolver<String>()
        _ = resolver.consume(contactSample(1, .began, at: 0), hit: "A")
        #expect(
            resolver.consume(
                contactSample(1, .ended, at: 0.1, point: CGPoint(x: 17, y: 0)),
                hit: "B"
            ) == .cancelled(.init(rawValue: 1))
        )
        _ = resolver.consume(contactSample(2, .began, at: 1), hit: "A")
        #expect(
            resolver.consume(contactSample(2, .ended, at: 1.301), hit: "A")
                == .cancelled(.init(rawValue: 2))
        )
        _ = resolver.consume(contactSample(3, .began, at: 2), hit: "A")
        #expect(
            resolver.consume(contactSample(3, .ended, at: 2.1), hit: nil)
                == .cancelled(.init(rawValue: 3))
        )
    }

    @Test func duplicateAndUnknownCallbacksAreNoOps() {
        var resolver = ContactResolver<String>()
        #expect(resolver.consume(contactSample(1, .began, at: 0), hit: nil) == .noOp(.beganOutside))
        #expect(resolver.consume(contactSample(1, .moved, at: 0.1), hit: "A") == .noOp(.unknownContact))
        _ = resolver.consume(contactSample(1, .began, at: 0.2), hit: "A")
        #expect(
            resolver.consume(contactSample(1, .began, at: 0.3), hit: "B")
                == .noOp(.duplicateBegin)
        )
        #expect(
            resolver.consume(contactSample(2, .cancelled, at: 0.4), hit: nil)
                == .noOp(.unknownContact)
        )
    }

    @Test func contactsStayIndependentAndResetDropsAll() {
        var resolver = ContactResolver<String>()
        _ = resolver.consume(contactSample(1, .began, at: 0), hit: "A")
        _ = resolver.consume(contactSample(2, .began, at: 0.01), hit: "B")
        #expect(resolver.activeContactCount == 2)
        #expect(
            resolver.consume(contactSample(2, .cancelled, at: 0.02), hit: nil)
                == .cancelled(.init(rawValue: 2))
        )
        resolver.reset()
        #expect(resolver.activeContactCount == 0)
        #expect(
            resolver.consume(contactSample(1, .ended, at: 0.1), hit: "A")
                == .noOp(.unknownContact)
        )
    }
}
