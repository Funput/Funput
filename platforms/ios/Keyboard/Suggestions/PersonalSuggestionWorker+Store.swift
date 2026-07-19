import Foundation
import FunputShared

extension PersonalSuggestionWorker {
    static func prepareStoreURL() -> URL? {
        guard var directory = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: FunputAppGroup.identifier
        )?.appendingPathComponent(
            FunputAppGroup.personalSuggestionsDirectory,
            isDirectory: true
        ) else { return nil }
        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true,
                attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication]
            )
            try FileManager.default.setAttributes(
                [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication],
                ofItemAtPath: directory.path
            )
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            try directory.setResourceValues(values)
            return directory
        } catch {
            return nil
        }
    }
}
