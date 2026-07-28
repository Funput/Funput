import CoreGraphics
import Foundation
import KeyboardTouchCore

func contactSample(
    _ id: UInt64,
    _ phase: ContactPhase,
    at timestamp: TimeInterval,
    point: CGPoint = .zero
) -> ContactSample {
    ContactSample(
        id: ContactID(rawValue: id),
        phase: phase,
        timestamp: timestamp,
        location: point,
        previousLocation: point
    )
}
