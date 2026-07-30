#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

struct KeyboardTouchContactRegistryTests {
    @Test("V2 release emits exactly once")
    func exactlyOnce() {
        var registry = KeyboardTouchContactRegistry()
        let id = ContactID(rawValue: 1)
        let key = key("a")
        registry.begin(id, key: key)

        let first = registry.commit(id, action: action(key))
        let duplicate = registry.commit(id, action: action(key))
        #expect(first)
        #expect(!duplicate)
        #expect(registry.metrics.v2Committed == 1)
        #expect(registry.metrics.ownershipViolation == 1)
        registry.finish(id)
        #expect(registry.pendingCount == 0)
    }

    @Test("System cancellation is terminal without commit")
    func cancellation() {
        var registry = KeyboardTouchContactRegistry()
        let id = ContactID(rawValue: 3)
        let key = key("a")
        registry.begin(id, key: key)

        let cancelled = registry.cancel(id, system: true)
        #expect(cancelled?.id == key.id)
        #expect(registry.metrics.systemCancelled == 1)
        let committedAfterCancellation = registry.commit(id, action: action(key))
        #expect(!committedAfterCancellation)
        registry.finish(id)
        #expect(registry.pendingCount == 0)
    }

    @Test("One hundred thousand V2 transitions remain exactly once")
    func deterministicStress() {
        var registry = KeyboardTouchContactRegistry()
        let key = key("a")
        for rawID in 1...100_000 {
            let id = ContactID(rawValue: UInt64(rawID))
            registry.begin(id, key: key)
            _ = registry.commit(id, action: action(key))
            registry.finish(id)
        }
        #expect(registry.pendingCount == 0)
        #expect(registry.metrics.v2Committed == 100_000)
        #expect(registry.metrics.ownershipViolation == 0)
    }

    private func key(
        _ id: String,
        role: KeyRole = .character
    ) -> KeySpec {
        KeySpec(id: id, label: id, role: role)
    }

    private func action(_ key: KeySpec) -> KeyboardTouchAction {
        .released(
            KeyboardTouchHit(
                identity: .init(geometryRevision: 1, ordinal: 0, role: key.role),
                key: key,
                frame: .zero
            )
        )
    }
}
#endif
