import Foundation
import FunputShared

public struct KeyboardBootstrapSnapshotStore: Sendable {
    public static let directoryName = "KeyboardBootstrap"
    public static let fileName = "snapshot.json"

    private let containerURL: URL?

    public init(suiteName: String = FunputAppGroup.identifier) {
        containerURL = AppGroupDirectory.containerURL(groupIdentifier: suiteName)
    }

    public init(containerURL: URL?) {
        self.containerURL = containerURL
    }

    /// Reads without preparing anything: the keyboard calls this on its way to the
    /// first frame, and a read needs no directory to exist. A missing file throws,
    /// which is the caller's cue to fall back.
    public func load() throws -> KeyboardBootstrapSnapshot {
        guard let containerURL else {
            throw KeyboardBootstrapSnapshotError.unavailableContainer
        }
        let url = containerURL
            .appendingPathComponent(Self.directoryName, isDirectory: true)
            .appendingPathComponent(Self.fileName, isDirectory: false)
        let data = try Data(contentsOf: url, options: .mappedIfSafe)
        return try JSONDecoder().decode(KeyboardBootstrapSnapshot.self, from: data)
    }

    public func save(_ snapshot: KeyboardBootstrapSnapshot) throws {
        let data = try JSONEncoder().encode(snapshot)
        try coordinateWrite(data, to: preparedFileURL(), onlyIfInvalid: false)
    }

    public func repairIfNeeded(_ snapshot: KeyboardBootstrapSnapshot) throws {
        let data = try JSONEncoder().encode(snapshot)
        try coordinateWrite(data, to: preparedFileURL(), onlyIfInvalid: true)
    }

    /// Only writers create the directory and set its protection.
    private func preparedFileURL() throws -> URL {
        guard let directory = AppGroupDirectory.prepare(
            named: Self.directoryName,
            in: containerURL
        ) else {
            throw KeyboardBootstrapSnapshotError.unavailableContainer
        }
        return directory.appendingPathComponent(Self.fileName, isDirectory: false)
    }

    private func coordinateWrite(
        _ data: Data,
        to url: URL,
        onlyIfInvalid: Bool
    ) throws {
        let coordinator = NSFileCoordinator()
        var coordinationError: NSError?
        var writeError: (any Error)?
        coordinator.coordinate(
            writingItemAt: url,
            options: .forReplacing,
            error: &coordinationError
        ) { coordinatedURL in
            do {
                if onlyIfInvalid,
                   let current = try? Data(contentsOf: coordinatedURL),
                   (try? JSONDecoder().decode(
                       KeyboardBootstrapSnapshot.self,
                       from: current
                   )) != nil {
                    return
                }
                try data.write(
                    to: coordinatedURL,
                    options: [
                        .atomic,
                        .completeFileProtectionUntilFirstUserAuthentication,
                    ]
                )
            } catch {
                writeError = error
            }
        }
        if let coordinationError { throw coordinationError }
        if let writeError { throw writeError }
    }
}
