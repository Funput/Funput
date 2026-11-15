import FunputShared
import SwiftUI
import Testing
@testable import Funput

@MainActor
struct KeyboardFullAccessOnboardingTests {
    @Test("Settings refreshes Full Access after returning to the app")
    func refreshesObservedAccess() {
        let access = KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        let model = SettingsModel(
            store: SettingsTestStore(configuration: .default),
            accessStore: access
        )

        #expect(!model.hasFullAccess)
        access.hasObservedFullAccess = true
        model.reload()
        #expect(model.hasFullAccess)
    }

    @Test("Full Access settings wait for confirmation")
    func gatedSettingWaitsForAccess() {
        let access = KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        let model = SettingsModel(
            store: SettingsTestStore(configuration: .default),
            accessStore: access
        )
        var requestCount = 0
        let binding = model.fullAccessBinding(\.isHapticFeedbackEnabled) {
            requestCount += 1
        }

        binding.wrappedValue = true
        #expect(requestCount == 1)
        #expect(!model.configuration.isHapticFeedbackEnabled)

        access.hasObservedFullAccess = true
        model.reload()
        binding.wrappedValue = true
        #expect(model.configuration.isHapticFeedbackEnabled)
    }

    @Test("Refresh reloads immediately and when the keyboard signals")
    func refreshReloadsOnSignal() async {
        var access = false
        var reloads = 0
        let signal = ObservedFullAccessTestSignal()
        let refresh = FullAccessRefresh(
            reload: { reloads += 1 },
            hasFullAccess: { access },
            pollAttempts: 0,
            observe: signal.observe
        )

        refresh.start()
        #expect(reloads == 1)

        signal.fire()
        await waitUntil { reloads == 2 }
        #expect(reloads == 2)

        refresh.stop()
        signal.fire()
        await Task.yield()
        #expect(reloads == 2)
    }

    @Test("Refresh polling stops once Full Access is observed")
    func refreshPollingStopsWhenGranted() async {
        var access = false
        var reloads = 0
        let refresh = FullAccessRefresh(
            reload: { reloads += 1 },
            hasFullAccess: { access },
            pollAttempts: 4,
            pollInterval: .milliseconds(10),
            observe: { _ in ObservedFullAccessTestToken() }
        )

        refresh.start()
        #expect(reloads == 1)
        access = true
        try? await Task.sleep(for: .milliseconds(80))
        let afterGrant = reloads
        #expect(afterGrant >= 2)
        try? await Task.sleep(for: .milliseconds(80))
        #expect(reloads == afterGrant)
        refresh.stop()
    }
}

final class KeyboardAccessStateTestStore: KeyboardAccessStateReading {
    var hasObservedFullAccess: Bool

    init(hasObservedFullAccess: Bool) {
        self.hasObservedFullAccess = hasObservedFullAccess
    }
}

@MainActor
private final class ObservedFullAccessTestSignal {
    private var handler: (() -> Void)?

    func observe(_ handler: @escaping () -> Void) -> any ObservedFullAccessObserving {
        self.handler = handler
        return ObservedFullAccessTestToken()
    }

    func fire() { handler?() }
}

private final class ObservedFullAccessTestToken: ObservedFullAccessObserving {}

@MainActor
private func waitUntil(
    timeout: Duration = .milliseconds(200),
    condition: () -> Bool
) async {
    let deadline = ContinuousClock.now + timeout
    while !condition(), ContinuousClock.now < deadline {
        await Task.yield()
    }
}
