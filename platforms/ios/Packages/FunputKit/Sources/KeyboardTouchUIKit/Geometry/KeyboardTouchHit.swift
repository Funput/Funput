import CoreGraphics
import KeyboardLayout

public struct KeyboardTouchHit: Sendable {
    public let key: KeySpec
    public let frame: CGRect

    public init(key: KeySpec, frame: CGRect) {
        self.key = key
        self.frame = frame
    }
}
