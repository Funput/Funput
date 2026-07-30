#if DEBUG
import Foundation
import FunputShared

@MainActor
extension KeyboardTouchAcceptanceModel {
    func startGuided(now: Date = Date()) {
        guidedProgress.reset()
        guidedClassification = nil
        gestureClassification = nil
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

    func startGestures(now: Date = Date()) {
        begin(.gestures, now: now)
    }

    func finishGestures(now: Date = Date()) {
        guard stage == .gestures else { return }
        beginSettlement(.gestures, now: now)
    }

    func stopFree(now: Date = Date()) {
        guard stage == .free else { return }
        freeDeadline = nil
        beginSettlement(.free, now: now)
    }

    func returnToSetup() {
        wantsFocus = false
        stage = .setup
        result = nil
    }

    private func finishGuided(now: Date) {
        beginSettlement(.guided, now: now)
    }

    private func beginSettlement(_ value: Settlement, now: Date) {
        wantsFocus = false
        stage = .settling
        settlement = value
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
            generation: generation,
            startedAt: now,
            expiresAt: expiry
        )
        guard overrideStore.save(
            KeyboardTouchAcceptanceFixture.configuration(for: selectedMethod),
            expiresAt: expiry
        ), diagnosticStore.start(session) else { return }
        switch phase {
        case .guided: stage = .guided
        case .gestures: stage = .gestures
        case .free: stage = .free
        }
        DispatchQueue.main.async { [weak self] in self?.wantsFocus = true }
    }
}
#endif
