#if canImport(UIKit)
import KeyboardRenderer
import Testing
import UIKit

@MainActor
@Suite("Keyboard surface host")
struct KeyboardSurfaceHostTests {
    private let host = KeyboardSurfaceHost()
    private let factory = SurfaceFactorySpy()

    @Test("The first claim builds the surface, pins it and binds it")
    func firstClaimBuilds() {
        let first = SurfaceClaimantFake()
        #expect(host.claim(for: first, at: .loading, make: factory.make))
        #expect(factory.built == 1)
        #expect(host.surface?.superview === first.surfaceContainer)
        #expect(first.bound.count == 1)
        #expect(host.isHeld(by: first))
    }

    @Test("One surface serves every claimant")
    func oneSurfaceAcrossClaims() {
        let first = SurfaceClaimantFake()
        let second = SurfaceClaimantFake()
        host.claim(for: first, at: .loading, make: factory.make)
        let surface = host.surface

        host.claim(for: second, at: .appearing, make: factory.make)
        host.claim(for: first, at: .appearing, make: factory.make)

        #expect(factory.built == 1)
        #expect(host.surface === surface)
        #expect(surface?.superview === first.surfaceContainer)
    }

    @Test("A loading claimant waits while the surface is on show, then takes it when appearing")
    func loadingWaitsThenTakes() {
        let shown = SurfaceClaimantFake()
        let next = SurfaceClaimantFake()
        host.claim(for: shown, at: .appearing, make: factory.make)

        #expect(!host.claim(for: next, at: .loading, make: factory.make))
        #expect(host.isHeld(by: shown))
        #expect(shown.surrenders == 0)
        #expect(next.bound.isEmpty)

        #expect(host.claim(for: next, at: .appearing, make: factory.make))
        #expect(host.surface?.superview === next.surfaceContainer)
        #expect(shown.surrenders == 1)
        #expect(next.bound.count == 1)
    }

    @Test("A loading claimant takes a surface its holder has put away")
    func loadingTakesADetachedSurface() {
        let previous = SurfaceClaimantFake()
        let next = SurfaceClaimantFake()
        host.claim(for: previous, at: .appearing, make: factory.make)
        host.detach(by: previous)
        #expect(host.surface?.superview == nil)

        #expect(host.claim(for: next, at: .loading, make: factory.make))
        #expect(previous.surrenders == 1)
        #expect(host.surface?.superview === next.surfaceContainer)
    }

    @Test("The holder gets its surface back after putting it away, without being bound again")
    func holderReattaches() {
        let holder = SurfaceClaimantFake()
        host.claim(for: holder, at: .appearing, make: factory.make)
        host.detach(by: holder)

        #expect(host.claim(for: holder, at: .appearing, make: factory.make))
        #expect(host.surface?.superview === holder.surfaceContainer)
        #expect(holder.bound.count == 1)
    }

    @Test("A claimant that lost the surface cannot detach it")
    func staleDetachIsIgnored() {
        let previous = SurfaceClaimantFake()
        let next = SurfaceClaimantFake()
        host.claim(for: previous, at: .appearing, make: factory.make)
        host.claim(for: next, at: .appearing, make: factory.make)

        host.detach(by: previous)
        #expect(host.surface?.superview === next.surfaceContainer)
    }

    @Test("A holder that has gone leaves the surface free, even still attached")
    func deadHolderFreesTheSurface() {
        var gone: SurfaceClaimantFake? = SurfaceClaimantFake()
        let container = gone?.surfaceContainer
        gone.map { _ = host.claim(for: $0, at: .appearing, make: factory.make) }
        gone = nil
        #expect(!host.hasHolder)
        #expect(host.surface?.superview === container)

        let next = SurfaceClaimantFake()
        #expect(host.claim(for: next, at: .loading, make: factory.make))
        #expect(factory.built == 1)
    }

    @Test("Moving the surface back and forth leaves one set of constraints")
    func constraintsDoNotAccumulate() throws {
        let first = SurfaceClaimantFake()
        let second = SurfaceClaimantFake()
        for _ in 0..<5 {
            host.claim(for: first, at: .appearing, make: factory.make)
            host.claim(for: second, at: .appearing, make: factory.make)
        }
        let surface = try #require(host.surface)
        let pinning = second.surfaceContainer.constraints.filter {
            $0.firstItem === surface || $0.secondItem === surface
        }
        #expect(pinning.count == 4)
        #expect(first.surfaceContainer.constraints.isEmpty)
    }
}
#endif
