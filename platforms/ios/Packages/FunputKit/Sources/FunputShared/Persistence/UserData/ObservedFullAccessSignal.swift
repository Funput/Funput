import Foundation
import notify

/// Cross-process signal from the keyboard to the app that the stored Full Access state
/// changed. It carries no payload: observers re-read ``KeyboardAccessStateStore``.
public enum ObservedFullAccessSignal {
    public static let name = FunputAppGroup.observedFullAccessChangedNotification

    public static func post() {
        notify_post(name)
    }

    /// Observation lasts as long as the returned token is retained.
    public static func observe(
        _ handler: @escaping @MainActor () -> Void
    ) -> any ObservedFullAccessObserving {
        ObservedFullAccessObserver(name: name, handler: handler)
    }
}

public protocol ObservedFullAccessObserving: AnyObject {}

/// Registers on the main queue, so the handler always runs on the main actor, and the
/// registration is cancelled when the token is released.
final class ObservedFullAccessObserver: ObservedFullAccessObserving {
    private let token: Int32?

    init(name: String, handler: @escaping @MainActor () -> Void) {
        var token = NOTIFY_TOKEN_INVALID
        let status = notify_register_dispatch(name, &token, .main) { _ in
            MainActor.assumeIsolated { handler() }
        }
        self.token = status == UInt32(NOTIFY_STATUS_OK) ? token : nil
    }

    deinit {
        if let token { notify_cancel(token) }
    }
}
