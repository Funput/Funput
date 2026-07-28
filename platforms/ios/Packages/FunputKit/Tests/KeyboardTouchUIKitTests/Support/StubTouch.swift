#if canImport(UIKit)
import UIKit

final class ShadowStubTouch: UITouch {
    var stubTimestamp: TimeInterval = 0
    var stubLocation: CGPoint = .zero
    var stubPreviousLocation: CGPoint = .zero

    override var timestamp: TimeInterval { stubTimestamp }
    override func location(in view: UIView?) -> CGPoint { stubLocation }
    override func previousLocation(in view: UIView?) -> CGPoint { stubPreviousLocation }
}
#endif
