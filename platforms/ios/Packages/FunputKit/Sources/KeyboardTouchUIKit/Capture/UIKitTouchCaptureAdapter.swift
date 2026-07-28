#if canImport(UIKit)
import KeyboardTouchCore
import UIKit

@MainActor
public final class UIKitTouchCaptureAdapter {
    private var identities = UIKitTouchIdentityMap()
    public private(set) var unknownCallbackCount = 0

    public init() {}

    public func samples(
        for touches: Set<UITouch>,
        phase: ContactPhase,
        in view: UIView
    ) -> [ContactSample] {
        var result: [ContactSample] = []
        result.reserveCapacity(touches.count + 1)
        for touch in touches {
            switch phase {
            case .began:
                let (contactID, stale) = identities.begin(touch)
                if let stale {
                    result.append(sample(for: touch, id: stale, phase: .cancelled, in: view))
                }
                result.append(sample(for: touch, id: contactID, phase: .began, in: view))
            case .moved:
                guard let contactID = identities.contactID(for: touch) else {
                    unknownCallbackCount += 1
                    continue
                }
                result.append(sample(for: touch, id: contactID, phase: .moved, in: view))
            case .ended, .cancelled:
                guard let contactID = identities.end(touch) else {
                    unknownCallbackCount += 1
                    continue
                }
                result.append(sample(for: touch, id: contactID, phase: phase, in: view))
            }
        }
        return result
    }

    public func reset() {
        identities.reset()
        unknownCallbackCount = 0
    }

    private func sample(
        for touch: UITouch,
        id: ContactID,
        phase: ContactPhase,
        in view: UIView
    ) -> ContactSample {
        ContactSample(
            id: id,
            phase: phase,
            timestamp: touch.timestamp,
            location: touch.location(in: view),
            previousLocation: touch.previousLocation(in: view)
        )
    }
}
#endif
