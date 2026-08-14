import Foundation

/// Caches only successfully decoded assets so transient failures are retried.
///
/// `variant` identifies what the cached value was decoded *for* — the keyboard's pixel
/// width, in practice. A backdrop decoded for a phone is not the one an iPad or a
/// rotated keyboard wants, so a changed variant misses just like a changed asset.
@MainActor
public final class KeyboardBackgroundAssetCache<Value> {
    public private(set) var assetID: String?
    public private(set) var variant: Int = 0
    public private(set) var value: Value?

    public init() {}

    public func resolve(
        assetID requestedID: String?,
        variant requestedVariant: Int = 0,
        load: (String) -> Data?,
        decode: (Data) -> Value?
    ) -> Value? {
        guard let requestedID else {
            clear()
            return nil
        }
        if requestedID == assetID, requestedVariant == variant, let value {
            return value
        }

        clear()
        guard let data = load(requestedID), let decoded = decode(data) else {
            return nil
        }
        assetID = requestedID
        variant = requestedVariant
        value = decoded
        return decoded
    }

    public func clear() {
        assetID = nil
        variant = 0
        value = nil
    }
}
