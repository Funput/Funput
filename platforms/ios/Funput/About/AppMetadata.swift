enum AppMetadata {
    static func versionLabel(from info: [String: Any]) -> String {
        let version = info["CFBundleShortVersionString"] as? String
        let build = info["CFBundleVersion"] as? String

        switch (version, build) {
        case let (.some(version), .some(build)) where !version.isEmpty && !build.isEmpty:
            return "Phiên bản \(version) (\(build))"
        case let (.some(version), _) where !version.isEmpty:
            return "Phiên bản \(version)"
        default:
            return "Phiên bản đang phát triển"
        }
    }
}
