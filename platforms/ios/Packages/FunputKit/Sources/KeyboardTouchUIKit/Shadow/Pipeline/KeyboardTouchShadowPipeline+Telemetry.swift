import KeyboardTouchCore

@MainActor
extension KeyboardTouchShadowPipeline {
    func recordTimestamp(_ sample: ContactSample) {
        for (otherID, timestamp) in beganAt where timestamp == sample.timestamp {
            tiedContacts.insert(otherID)
            tiedContacts.insert(sample.id)
        }
        beganAt[sample.id] = sample.timestamp
    }

    func finish(_ id: ContactID) {
        geometries.removeValue(forKey: id)
        beganAt.removeValue(forKey: id)
    }

    func recordShadow(_ emission: PressEmission<ShadowKeyIdentity>) {
        if let terminalTime = resolvedAt.removeValue(forKey: emission.contactID) {
            trace.recordEmissionDelay(max(0, clock() - terminalTime))
        }
        comparator.recordShadow(
            emission.payload,
            timestampTie: tiedContacts.remove(emission.contactID) != nil
        )
        observeArbiterState()
    }

    func observeArbiterState() {
        trace.observePipeline(
            activeContacts: resolver.activeContactCount,
            arbiterDepth: arbiter.orderedContactCount + arbiter.heldContactCount,
            bypassCount: arbiter.bypassedContactCount
        )
    }
}
