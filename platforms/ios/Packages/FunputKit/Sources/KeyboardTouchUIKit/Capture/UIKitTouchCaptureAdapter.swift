#if canImport(UIKit)
import KeyboardTouchCore
import UIKit

@MainActor
public final class UIKitTouchCaptureAdapter {
    private var identities = UIKitTouchIdentityMap()
    public private(set) var unknownCallbackCount = 0
    /// How often UIKit reused a `UITouch` object whose previous contact was still tracked.
    /// Retiring the old contact is a policy decision, not an observation, so the frequency
    /// is reported instead of being swallowed.
    public private(set) var staleIdentityCount = 0

    public init() {}

    public func samples(
        for touches: Set<UITouch>,
        phase: ContactPhase,
        in view: UIView
    ) -> [ContactSample] {
        var result: [ContactSample] = []
        result.reserveCapacity(touches.count + 1)
        for touch in ordered(touches, in: view) {
            switch phase {
            case .began:
                let (contactID, stale) = identities.begin(touch)
                if let stale {
                    staleIdentityCount += 1
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
        staleIdentityCount = 0
    }

    /// `Set` iteration order is unspecified, so a batch delivering several touches at once would
    /// assign contact order at random. Sorting makes a run reproducible. It does not claim to
    /// recover the physical press order — UIKit does not carry that information for identical
    /// timestamps (architecture document, §4.2 and §9.5).
    private func ordered(_ touches: Set<UITouch>, in view: UIView) -> [UITouch] {
        guard touches.count > 1 else { return Array(touches) }
        return touches.sorted { lhs, rhs in
            guard lhs.timestamp == rhs.timestamp else {
                return lhs.timestamp < rhs.timestamp
            }
            let left = lhs.location(in: view)
            let right = rhs.location(in: view)
            guard left.x == right.x else { return left.x < right.x }
            return left.y < right.y
        }
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
