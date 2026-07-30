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

    func finishTimestamp(_ id: ContactID) {
        beganAt.removeValue(forKey: id)
    }

    func recordShadow(_ emission: PressEmission<KeyboardTouchAction>) {
        if let terminalTime = resolvedAt.removeValue(forKey: emission.contactID) {
            trace.recordEmissionDelay(max(0, clock() - terminalTime))
        }
        comparator.recordShadow(
            emission.payload.hit.identity,
            timestampTie: tiedContacts.remove(emission.contactID) != nil
        )
        observeArbiterState()
    }

    func observeArbiterState() {
        trace.observePipeline(
            activeContacts: fastTap.activeContactCount,
            arbiterDepth: fastTap.orderedContactCount + fastTap.heldContactCount,
            bypassCount: fastTap.bypassedContactCount
        )
    }
}
