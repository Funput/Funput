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

    @Test("Refreshing Full Access alone publishes only a change")
    func refreshFullAccessReportsChange() {
        let access = KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        let model = SettingsModel(
            store: SettingsTestStore(configuration: .default),
            accessStore: access
        )

        #expect(!model.refreshFullAccess())
        access.hasObservedFullAccess = true
        #expect(model.refreshFullAccess())
        #expect(model.hasFullAccess)
        #expect(!model.refreshFullAccess())
        access.hasObservedFullAccess = false
        #expect(model.refreshFullAccess())
        #expect(!model.hasFullAccess)
    }
}

final class KeyboardAccessStateTestStore: KeyboardAccessStateReading {
    var hasObservedFullAccess: Bool

    init(hasObservedFullAccess: Bool) {
        self.hasObservedFullAccess = hasObservedFullAccess
    }
}
