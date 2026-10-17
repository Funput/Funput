import Foundation

extension UserDefaults {
    /// Store `value` as JSON, or remove the key when it is nil or cannot be encoded, so a
    /// later read falls back to its default instead of decoding stale data.
    func setJSON<Value: Encodable>(_ value: Value?, forKey key: String) {
        if let data = value.flatMap({ try? JSONEncoder().encode($0) }) {
            set(data, forKey: key)
        } else {
            removeObject(forKey: key)
        }
    }
}
