#if DEBUG
import Foundation
import FunputShared

@MainActor
extension ShadowTypingHarnessModel {
    func startGuided(now: Date = Date()) {
        guidedProgress.reset()
        begin(.guided, now: now)
    }

    func checkGuidedStep(now: Date = Date()) {
        guard stage == .guided else { return }
        switch guidedProgress.check(text, fixture: fixture) {
        case .retry:
            break
        case .advanced:
            text = ""
            wantsFocus = true
        case .completed:
            finishGuided(now: now)
        }
    }

    func retryGuidedStep() {
        guard stage == .guided else { return }
        guidedProgress.prepareRetry()
        text = ""
        wantsFocus = true
    }

    func startFree(now: Date = Date()) {
        begin(.free, now: now)
        freeDeadline = now.addingTimeInterval(60)
        freeSecondsRemaining = 60
    }

    func stopFree() {
        guard stage == .free else { return }
        wantsFocus = false
        freeDeadline = nil
        result = ShadowHarnessResult.make(
            text: text,
            report: report,
            exactMatch: nil
        )
        stage = .freeResult
    }

    func returnToSetup() {
        wantsFocus = false
        stage = .setup
        result = nil
    }

    private func finishGuided(now: Date) {
        wantsFocus = false
        stage = .guidedSettling
        report = diagnosticStore.report(now: now)
        settlementAfterSequence = report?.sequence ?? 0
        settlementDeadline = now.addingTimeInterval(1)
        tick(now: now)
    }

    private func begin(
        _ phase: KeyboardTouchDiagnosticPhase,
        now: Date
    ) {
        hasFullAccess = accessCheck()
        guard hasFullAccess else { return }
        wantsFocus = false
        text = ""
        report = nil
        result = nil
        settlementDeadline = nil
        generation &+= 1
        let expiry = now.addingTimeInterval(15 * 60)
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: selectedMethod,
            phase: phase,
            pipelineMode: selectedPipeline,
            generation: generation,
            startedAt: now,
            expiresAt: expiry
        )
        guard overrideStore.save(
            ShadowTypingFixture.configuration(for: selectedMethod),
            expiresAt: expiry
        ), diagnosticStore.start(session) else { return }
        stage = phase == .guided ? .guided : .free
        DispatchQueue.main.async { [weak self] in self?.wantsFocus = true }
    }
}
#endif
