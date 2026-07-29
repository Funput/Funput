import CoreGraphics
import KeyboardLayout

public struct KeyboardTouchHit: Sendable {
    public let identity: ShadowKeyIdentity
    public let key: KeySpec
    public let frame: CGRect

    public init(identity: ShadowKeyIdentity, key: KeySpec, frame: CGRect) {
        self.identity = identity
        self.key = key
        self.frame = frame
    }
}
