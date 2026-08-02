import Foundation

/// Creates a directory inside the shared App Group container with the file
/// protection and backup exclusion that private user data requires.
///
/// Both the personal-suggestion snapshot and the clipboard history go through
/// here: the protection class and the backup flag are security properties, and
/// a second hand-rolled copy of them is a second place to get them wrong.
public enum AppGroupDirectory {
    public static func containerURL(groupIdentifier: String = FunputAppGroup.identifier) -> URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: groupIdentifier)
    }

    /// - Parameter container: overridable so tests can exercise the real attribute
    ///   work against a temporary directory — a test bundle has no App Group.
    public static func prepare(named name: String, in container: URL? = containerURL()) -> URL? {
        guard var directory = container?.appendingPathComponent(name, isDirectory: true) else {
            return nil
        }
        let protection: [FileAttributeKey: Any] = [
            .protectionKey: FileProtectionType.completeUntilFirstUserAuthentication,
        ]
        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true,
                attributes: protection
            )
            try FileManager.default.setAttributes(protection, ofItemAtPath: directory.path)
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            try directory.setResourceValues(values)
            return directory
        } catch {
            return nil
        }
    }
}
