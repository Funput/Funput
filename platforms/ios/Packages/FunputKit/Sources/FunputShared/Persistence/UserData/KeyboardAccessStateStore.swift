import Foundation

/// Shares the keyboard extension's last confirmed Full Access state with the app.
///
/// Once latched, the flag stays true so a stale extension process that still reports
/// `hasFullAccess == false` cannot clear the app's confirmed setup state.
public struct KeyboardAccessStateStore {
    private let defaults: UserDefaults
    private let key: String
    private let notify: () -> Void

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.observedFullAccessKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    public init(
        defaults: UserDefaults,
        key: String = FunputAppGroup.observedFullAccessKey,
        notify: @escaping () -> Void = { ObservedFullAccessSignal.post() }
    ) {
        self.defaults = defaults
        self.key = key
        self.notify = notify
    }

    public var hasObservedFullAccess: Bool {
        defaults.bool(forKey: key)
    }

    /// Latches Full Access after the keyboard confirms it. Idempotent.
    public func recordFullAccess() {
        guard !hasObservedFullAccess else { return }
        defaults.set(true, forKey: key)
        notify()
    }
}

/// Cross-process signal when the keyboard first latches Full Access.
public enum ObservedFullAccessSignal {
    public static let name = FunputAppGroup.observedFullAccessChangedNotification

    public static func post() {
        CFNotificationCenterPostNotification(
            CFNotificationCenterGetDarwinNotifyCenter(),
            CFNotificationName(name as CFString),
            nil,
            nil,
            true
        )
    }

    /// Retain the token for as long as observation should last.
    public static func observe(_ handler: @escaping () -> Void) -> any ObservedFullAccessObserving {
        ObservedFullAccessObserver(handler: handler)
    }
}

public protocol ObservedFullAccessObserving: AnyObject {}

final class ObservedFullAccessObserver: ObservedFullAccessObserving {
    private let lock = NSLock()
    private var handler: (() -> Void)?

    init(handler: @escaping () -> Void) {
        self.handler = handler
        let callback: CFNotificationCallback = { _, observer, _, _, _ in
            guard let observer else { return }
            let unmanaged = Unmanaged<ObservedFullAccessObserver>.fromOpaque(observer)
            unmanaged.retain()
            unmanaged.takeUnretainedValue().invoke()
            unmanaged.release()
        }
        CFNotificationCenterAddObserver(
            CFNotificationCenterGetDarwinNotifyCenter(),
            Unmanaged.passUnretained(self).toOpaque(),
            callback,
            FunputAppGroup.observedFullAccessChangedNotification as CFString,
            nil,
            .deliverImmediately
        )
    }

    private func invoke() {
        lock.lock()
        let handler = self.handler
        lock.unlock()
        handler?()
    }

    deinit {
        lock.lock()
        handler = nil
        lock.unlock()
        CFNotificationCenterRemoveObserver(
            CFNotificationCenterGetDarwinNotifyCenter(),
            Unmanaged.passUnretained(self).toOpaque(),
            CFNotificationName(FunputAppGroup.observedFullAccessChangedNotification as CFString),
            nil
        )
    }
}
