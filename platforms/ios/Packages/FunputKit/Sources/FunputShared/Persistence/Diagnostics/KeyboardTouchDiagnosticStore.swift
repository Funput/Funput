#if DEBUG
import Foundation

public struct KeyboardTouchDiagnosticStore: @unchecked Sendable {
    private let defaults: UserDefaults
    private let sessionKey: String
    private let reportKey: String

    public init(suiteName: String = FunputAppGroup.identifier) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard)
    }

    public init(
        defaults: UserDefaults,
        sessionKey: String = FunputAppGroup.touchDiagnosticSessionKey,
        reportKey: String = FunputAppGroup.touchDiagnosticReportKey
    ) {
        self.defaults = defaults
        self.sessionKey = sessionKey
        self.reportKey = reportKey
    }

    @discardableResult
    public func start(_ session: KeyboardTouchDiagnosticSession) -> Bool {
        guard let data = try? JSONEncoder().encode(session) else { return false }
        defaults.set(data, forKey: sessionKey)
        defaults.removeObject(forKey: reportKey)
        return true
    }

    public func activeSession(now: Date = Date()) -> KeyboardTouchDiagnosticSession? {
        guard let data = defaults.data(forKey: sessionKey),
              let session = try? JSONDecoder().decode(
                KeyboardTouchDiagnosticSession.self,
                from: data
              ),
              session.isActive(at: now)
        else {
            clear()
            return nil
        }
        return session
    }

    @discardableResult
    public func save(_ report: KeyboardTouchDiagnosticReport, now: Date = Date()) -> Bool {
        guard let session = activeSession(now: now),
              session.id == report.sessionID,
              session.generation == report.generation,
              let data = try? JSONEncoder().encode(report)
        else { return false }
        defaults.set(data, forKey: reportKey)
        return true
    }

    public func report(now: Date = Date()) -> KeyboardTouchDiagnosticReport? {
        guard let session = activeSession(now: now),
              let data = defaults.data(forKey: reportKey),
              let report = try? JSONDecoder().decode(
                KeyboardTouchDiagnosticReport.self,
                from: data
              ),
              report.sessionID == session.id,
              report.generation == session.generation
        else {
            defaults.removeObject(forKey: reportKey)
            return nil
        }
        return report
    }

    public func clear() {
        defaults.removeObject(forKey: sessionKey)
        defaults.removeObject(forKey: reportKey)
    }
}
#endif
