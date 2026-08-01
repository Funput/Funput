import CoreGraphics
import Foundation
import KeyboardTouchCore

@MainActor
extension KeyboardTouchPipeline {
    public func consume(_ sample: ContactSample) -> KeyboardTouchDisposition {
        let geometry = sample.phase == .began
            ? currentGeometry : geometries[sample.id]
        let tracked = eligibleHit(at: sample.location, in: geometry)
        let hit = recoveredHit(for: sample, current: tracked)
        let resolution = resolver.consume(sample, hit: hit)
        return handle(resolution, sample: sample, geometry: geometry)
    }

    @discardableResult
    public func exclude(
        _ contactID: ContactID,
        at timestamp: TimeInterval
    ) -> Bool {
        detach(contactID, at: timestamp)
    }

    @discardableResult
    public func claimForGesture(_ contactID: ContactID) -> Bool {
        resolver.discard(contactID)
    }

    @discardableResult
    public func resolveGesture(
        _ contactID: ContactID,
        action: KeyboardTouchAction,
        at timestamp: TimeInterval
    ) -> Bool {
        guard geometries.removeValue(forKey: contactID) != nil else { return false }
        initialHits.removeValue(forKey: contactID)
        arbiter.resolve(contactID, payload: action, at: timestamp)
        return true
    }

    @discardableResult
    public func detach(
        _ contactID: ContactID,
        at timestamp: TimeInterval
    ) -> Bool {
        let existed = resolver.discard(contactID)
            || geometries[contactID] != nil
        guard existed else { return false }
        geometries.removeValue(forKey: contactID)
        initialHits.removeValue(forKey: contactID)
        arbiter.cancel(contactID, at: timestamp)
        return true
    }

    public func reset() {
        arbiter.reset()
        resolver.reset()
        geometries.removeAll(keepingCapacity: true)
        initialHits.removeAll(keepingCapacity: true)
        counters = KeyboardTouchPipelineStatistics()
    }

    private func eligibleHit(
        at point: CGPoint,
        in geometry: KeyboardGeometrySnapshot?
    ) -> KeyboardTouchHit? {
        guard let hit = geometry?.touchHit(at: point),
              policy.isEligible(hit.key.role) else { return nil }
        return hit
    }

    /// A fast two-thumb tap often lifts off the tracked geometry. Recovering it as the key the
    /// finger landed on keeps the press instead of dropping it, mirroring how the resolver's
    /// tap-slop recovery already treats a finger that merely drifted.
    private func recoveredHit(
        for sample: ContactSample,
        current: KeyboardTouchHit?
    ) -> KeyboardTouchHit? {
        let locked = lockedHit(for: sample.id, current: current)
        // `isTracking` keeps a contact already claimed by a gesture out of the counters, since
        // the resolver would ignore the hit anyway.
        guard locked == nil,
              sample.phase == .ended,
              resolver.isTracking(sample.id),
              let initial = initialHits[sample.id] else { return locked }
        let recovered = policy.recoversReleaseOutside(initial.key.role)
        counters.recordReleaseOutside(recovered: recovered)
        return recovered ? initial : nil
    }

    /// Keys with a horizontal swipe own the contact once it starts, so a finger sliding across
    /// the spacebar never commits a neighbouring key.
    private func lockedHit(
        for id: ContactID,
        current: KeyboardTouchHit?
    ) -> KeyboardTouchHit? {
        guard let initial = initialHits[id],
              initial.key.horizontalSwipeAction != nil,
              current != nil else { return current }
        return initial
    }
}
