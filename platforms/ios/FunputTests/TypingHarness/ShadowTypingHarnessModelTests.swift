#if DEBUG
import Foundation
import FunputShared
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
struct ShadowTypingHarnessModelTests {
    @Test("Guided session configures mode and classifies exact output")
    func guidedPass() throws {
        try withHarness { model, diagnostics, overrides in
            model.selectedMethod = .telexAdvanced
            model.startGuided()
            let now = Date()
            #expect(model.stage == .guided)
            #expect(overrides.load(now: now)?.inputMethod == .telexAdvanced)

            let session = try #require(diagnostics.activeSession(now: now))
            #expect(diagnostics.save(cleanReport(session), now: now))
            for step in model.fixture.steps {
                model.text = step.expected
                model.checkGuidedStep(now: now)
            }
            #expect(model.stage == .guidedSettling)
            #expect(diagnostics.save(
                cleanReport(session, sequence: 2),
                now: now.addingTimeInterval(0.01)
            ))
            model.tick(now: now.addingTimeInterval(0.01))

            #expect(model.stage == .guidedResult)
            #expect(model.result?.classification == .pass)
            #expect(model.result?.exactMatch == true)
        }
    }

    @Test("Every mode writes an isolated configuration and session")
    func configurationAndIsolation() throws {
        try withHarness { model, diagnostics, overrides in
            var previousID: UUID?
            for method in KeyboardInputMethod.allCases {
                model.selectedMethod = method
                model.startGuided()
                let now = Date()
                let session = try #require(diagnostics.activeSession(now: now))
                #expect(session.id != previousID)
                #expect(session.inputMethod == method)
                #expect(overrides.load(now: now)?.inputMethod == method)
                #expect(diagnostics.report(now: now) == nil)
                previousID = session.id
                model.returnToSetup()
            }
        }
    }

    @Test("Classification distinguishes legacy and shadow failures")
    func classification() {
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .vni,
            phase: .guided,
            generation: 1
        )
        var legacyMetrics = KeyboardTouchDiagnosticMetrics()
        legacyMetrics.legacyMissing = 1
        let legacy = report(session, metrics: legacyMetrics)
        let legacyResult = ShadowHarnessResult.make(
            text: "wrong",
            report: legacy,
            exactMatch: false
        )
        #expect(legacyResult.classification == .legacyDivergence)
        #expect(legacyResult.firstMismatchIndex == 0)

        var shadowMetrics = KeyboardTouchDiagnosticMetrics()
        shadowMetrics.shadowMissing = 1
        let shadow = report(session, metrics: shadowMetrics)
        #expect(ShadowHarnessResult.make(
            text: ShadowTypingFixture.expected,
            report: shadow,
            exactMatch: true
        ).classification == .shadowRegression)
    }

    @Test("Free stress expires at sixty seconds and supports early stop")
    func freeTimer() throws {
        try withHarness { model, diagnostics, _ in
            let now = Date()
            model.startFree(now: now)
            #expect(model.stage == .free)
            #expect(model.freeSecondsRemaining == 60)
            model.tick(now: now.addingTimeInterval(60))
            #expect(model.stage == .freeResult)

            model.startFree(now: now.addingTimeInterval(61))
            let replacement = try #require(
                diagnostics.activeSession(now: now.addingTimeInterval(61))
            )
            #expect(replacement.phase == .free)
            model.stopFree()
            #expect(model.stage == .freeResult)
        }
    }

    private func withHarness(
        _ body: (
            ShadowTypingHarnessModel,
            KeyboardTouchDiagnosticStore,
            FunputUITestConfigurationOverrideStore
        ) throws -> Void
    ) throws {
        let name = "test.shadow-harness.\(UUID())"
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        let diagnostics = KeyboardTouchDiagnosticStore(defaults: defaults)
        let overrides = FunputUITestConfigurationOverrideStore(defaults: defaults)
        let model = ShadowTypingHarnessModel(
            diagnosticStore: diagnostics,
            overrideStore: overrides,
            accessCheck: { true }
        )
        try body(model, diagnostics, overrides)
    }
}

private func cleanReport(
    _ session: KeyboardTouchDiagnosticSession,
    sequence: UInt64 = 1
) -> KeyboardTouchDiagnosticReport {
    report(session, metrics: .init(), sequence: sequence)
}

private func report(
    _ session: KeyboardTouchDiagnosticSession,
    metrics: KeyboardTouchDiagnosticMetrics,
    sequence: UInt64 = 1
) -> KeyboardTouchDiagnosticReport {
    .init(
        sessionID: session.id,
        generation: session.generation,
        sequence: sequence,
        observedAt: session.startedAt,
        metrics: metrics,
        activeContactCount: 0,
        pendingComparisonCount: 0,
        isSettled: true,
        device: .init(model: "test", operatingSystem: "test", maximumFramesPerSecond: 60)
    )
}
#endif
