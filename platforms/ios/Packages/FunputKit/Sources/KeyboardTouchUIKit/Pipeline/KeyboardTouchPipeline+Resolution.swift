import KeyboardTouchCore

@MainActor
extension KeyboardTouchPipeline {
    func handle(
        _ resolution: ContactResolution<KeyboardTouchHit>,
        sample: ContactSample,
        geometry: KeyboardGeometrySnapshot?
    ) -> KeyboardTouchDisposition {
        switch resolution {
        case let .began(id):
            guard let geometry,
                  let hit = geometry.touchHit(at: sample.location) else {
                return .ignored(.beganOutside)
            }
            geometries[id] = geometry
            initialHits[id] = hit
            _ = arbiter.begin(id, at: sample.timestamp)
            return .began(id, hit)
        case let .resolved(id, hit, metadata):
            geometries.removeValue(forKey: id)
            initialHits.removeValue(forKey: id)
            if metadata.exceededTapSlop,
               !policy.recoversTapSlop(hit.key.role) {
                arbiter.cancel(id, at: sample.timestamp)
                return .fallback(id, .exceededTapSlop)
            }
            onResolved(id, sample.timestamp)
            arbiter.resolve(
                id,
                payload: .released(hit),
                at: sample.timestamp
            )
            return .resolved(id, metadata)
        case let .cancelled(id, reason):
            geometries.removeValue(forKey: id)
            initialHits.removeValue(forKey: id)
            arbiter.cancel(id, at: sample.timestamp)
            return disposition(id: id, reason: reason)
        case let .noOp(reason):
            return reason == .updated
                ? .tracking(sample.id) : .ignored(reason)
        }
    }

    private func disposition(
        id: ContactID,
        reason: ContactCancellationReason
    ) -> KeyboardTouchDisposition {
        switch reason {
        case .system: .cancelled(id)
        case .exceededDuration: .fallback(id, .exceededDuration)
        case .endedOutside: .fallback(id, .endedOutside)
        case .exceededTapSlop: .fallback(id, .exceededTapSlop)
        }
    }
}
