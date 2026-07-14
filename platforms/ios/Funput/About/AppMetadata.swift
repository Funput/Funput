enum AppMetadata {
    static func versionLabel(from info: [String: Any]) -> String {
        let version = info["CFBundleShortVersionString"] as? String

        switch version {
        case let .some(version) where !version.isEmpty:
            return "Phiên bản \(version)"
        default:
            return "Phiên bản đang phát triển"
        }
    }
}
