import Foundation
import Testing
import FunputShared

struct ToneStyleInstallCohortTests {
    @Test("A clean App Group is a new install")
    func cleanGroupIsModern() {
        withVolatileDefaults { defaults in
            let store = ToneStyleInstallCohortStore(defaults: defaults, hasPriorState: { false })
            #expect(store.cohort() == .modern)
            #expect(store.cohort().toneStyle == .modern)
        }
    }

    @Test("State left by an earlier run marks the install legacy")
    func priorStateIsLegacy() {
        withVolatileDefaults { defaults in
            let store = ToneStyleInstallCohortStore(defaults: defaults, hasPriorState: { true })
            #expect(store.cohort() == .legacy)
            #expect(store.cohort().toneStyle == .traditional)
        }
    }

    /// The whole point of writing the answer down: evidence of a prior install
    /// only accumulates, so a later run must not be allowed to change its mind
    /// and move the tone placement after it has settled.
    @Test("The first answer holds even once evidence appears")
    func recordedCohortIsStable() {
        withVolatileDefaults { defaults in
            #expect(ToneStyleInstallCohortStore(defaults: defaults, hasPriorState: { false })
                .cohort() == .modern)
            #expect(ToneStyleInstallCohortStore(defaults: defaults, hasPriorState: { true })
                .cohort() == .modern)
        }
    }

    @Test("Any key an earlier run wrote counts as prior state")
    func everyProbedKeyCounts() {
        for key in ToneStyleInstallCohortStore.priorStateKeys {
            withVolatileDefaults { defaults in
                defaults.set("anything", forKey: key)
                #expect(ToneStyleInstallCohortStore(defaults: defaults).cohort() == .legacy)
            }
        }
    }

    /// A garbled marker is not evidence of anything, so the decision is retaken
    /// rather than defaulted to one side.
    @Test("An unreadable marker is decided again")
    func unreadableMarkerIsReresolved() {
        withVolatileDefaults { defaults in
            defaults.set("neither", forKey: FunputAppGroup.toneStyleCohortKey)
            #expect(ToneStyleInstallCohortStore(defaults: defaults, hasPriorState: { true })
                .cohort() == .legacy)
        }
    }

    private func withVolatileDefaults(_ body: (UserDefaults) -> Void) {
        let suiteName = "test.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }
        body(defaults)
    }
}
