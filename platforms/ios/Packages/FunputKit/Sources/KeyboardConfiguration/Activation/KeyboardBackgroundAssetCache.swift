import Foundation

/// Caches only successfully decoded assets so transient failures are retried.
@MainActor
public final class KeyboardBackgroundAssetCache<Value> {
    public private(set) var assetID: String?
    public private(set) var value: Value?

    public init() {}

    public func resolve(
        assetID requestedID: String?,
        load: (String) -> Data?,
        decode: (Data) -> Value?
    ) -> Value? {
        guard let requestedID else {
            clear()
            return nil
        }
        if requestedID == assetID, let value {
            return value
        }

        clear()
        guard let data = load(requestedID), let decoded = decode(data) else {
            return nil
        }
        assetID = requestedID
        value = decoded
        return decoded
    }

    private func clear() {
        assetID = nil
        value = nil
    }
}
