@testable import KeyboardRenderer
import Foundation
import Testing

struct KeyboardSystemEdgeBlendTests {
    @Test("Edge bridge stays within its visual depth budget")
    func depthBounds() {
        #expect(KeyboardSystemEdgeBlend.depth(for: 200) == 24)
        #expect(KeyboardSystemEdgeBlend.depth(for: 304) == 27.36)
        #expect(KeyboardSystemEdgeBlend.depth(for: 500) == 32)
    }

    @Test("Mask remains opaque until the system-facing bottom transition")
    func maskLocations() {
        let locations = KeyboardSystemEdgeBlend.locations(for: 300).map(\.doubleValue)
        #expect(locations[0] == 0)
        #expect(abs(locations[1] - 0.91) < 0.0001)
        #expect(locations[2] == 1)
    }
}
