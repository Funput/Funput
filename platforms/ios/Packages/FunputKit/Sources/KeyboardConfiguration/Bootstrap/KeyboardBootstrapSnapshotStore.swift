import Foundation
import FunputShared

public struct KeyboardBootstrapSnapshotStore: Sendable {
    public static let directoryName = "KeyboardBootstrap"
    public static let fileName = "snapshot.json"

    private let directoryURL: URL?

    public init(suiteName: String = FunputAppGroup.identifier) {
        let container = AppGroupDirectory.containerURL(groupIdentifier: suiteName)
        directoryURL = AppGroupDirectory.prepare(
            named: Self.directoryName,
            in: container
        )
    }

    public init(containerURL: URL?) {
        directoryURL = AppGroupDirectory.prepare(
            named: Self.directoryName,
            in: containerURL
        )
    }

    public func load() throws -> KeyboardBootstrapSnapshot {
        let data = try Data(contentsOf: fileURL(), options: .mappedIfSafe)
        return try JSONDecoder().decode(KeyboardBootstrapSnapshot.self, from: data)
    }

    public func save(_ snapshot: KeyboardBootstrapSnapshot) throws {
        let data = try JSONEncoder().encode(snapshot)
        let url = try fileURL()
        try coordinateWrite(data, to: url, onlyIfInvalid: false)
    }

    public func repairIfNeeded(_ snapshot: KeyboardBootstrapSnapshot) throws {
        let data = try JSONEncoder().encode(snapshot)
        let url = try fileURL()
        try coordinateWrite(data, to: url, onlyIfInvalid: true)
    }

    private func fileURL() throws -> URL {
        guard let directoryURL else {
            throw KeyboardBootstrapSnapshotError.unavailableContainer
        }
        return directoryURL.appendingPathComponent(Self.fileName, isDirectory: false)
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
