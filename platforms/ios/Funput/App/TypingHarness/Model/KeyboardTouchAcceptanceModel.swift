#if DEBUG
import Combine
import Foundation
import FunputShared
import KeyboardLayout
import UIKit

@MainActor
final class KeyboardTouchAcceptanceModel: ObservableObject {
    enum Settlement {
        case guided, gestures, free
    }

    enum Stage: Equatable {
        case setup, guided, settling, guidedResult
        case gestures, gestureResult, free, freeResult
    }

    @Published var selectedMethod: KeyboardInputMethod = .vni
    @Published var stage: Stage = .setup
    @Published var text = ""
    @Published var wantsFocus = false
    @Published var report: KeyboardTouchDiagnosticReport?
    @Published var result: KeyboardTouchAcceptanceResult?
    @Published var guidedProgress = GuidedTypingProgress()
    @Published var freeSecondsRemaining = 60
    @Published var hasFullAccess = false

    let diagnosticStore: KeyboardTouchDiagnosticStore
    let overrideStore: FunputUITestConfigurationOverrideStore
    let accessCheck: () -> Bool
    var timer: Timer?
    var freeDeadline: Date?
    var settlementDeadline: Date?
    var settlementAfterSequence: UInt64 = 0
    var settlement: Settlement?
    var guidedClassification: KeyboardTouchAcceptanceClassification?
    var gestureClassification: KeyboardTouchAcceptanceClassification?
    var generation = UInt64(Date().timeIntervalSince1970 * 1_000)

    init(
        diagnosticStore: KeyboardTouchDiagnosticStore = .init(),
        overrideStore: FunputUITestConfigurationOverrideStore = .init(),
        accessCheck: @escaping () -> Bool = {
            KeyboardAccessStateStore().hasObservedFullAccess
        }
    ) {
        self.diagnosticStore = diagnosticStore
        self.overrideStore = overrideStore
        self.accessCheck = accessCheck
    }

    var fixture: KeyboardTouchAcceptanceFixture {
        KeyboardTouchAcceptanceFixture.all.first { $0.inputMethod == selectedMethod }!
    }

    var guidedStep: AcceptanceTypingStep {
        guidedProgress.step(in: fixture)
    }

    func appear() {
        hasFullAccess = accessCheck()
        guard timer == nil else { return }
        timer = Timer.scheduledTimer(withTimeInterval: 0.250, repeats: true) {
            [weak self] _ in
            MainActor.assumeIsolated { self?.tick() }
        }
    }

    func disappear() {
        timer?.invalidate()
        timer = nil
        wantsFocus = false
    }

    func copyNumericReport() {
        UIPasteboard.general.string = report?.numericJSON
    }

    func tick(now: Date = Date()) {
        report = diagnosticStore.report(now: now)
        let receivedFinal = report.map {
            $0.sequence > settlementAfterSequence && $0.isSettled
        } ?? false
        if stage == .settling, let deadline = settlementDeadline,
           receivedFinal || now >= deadline {
            settlementDeadline = nil
            completeSettlement()
        }
        if stage == .free, let deadline = freeDeadline {
            freeSecondsRemaining = max(
                0,
                Int(ceil(deadline.timeIntervalSince(now)))
            )
            if now >= deadline { stopFree(now: now) }
        }
    }

    private func completeSettlement() {
        switch settlement {
        case .guided:
            let value = guidedProgress.verifiedText
            result = .make(
                text: value,
                report: report,
                exactMatch: value == KeyboardTouchAcceptanceFixture.expected
            )
            guidedClassification = result?.classification
            stage = .guidedResult
        case .gestures:
            result = .make(
                text: text, report: report, exactMatch: nil,
                requiresGestureCoverage: true
            )
            gestureClassification = result?.classification
            stage = .gestureResult
        case .free:
            let freeResult = KeyboardTouchAcceptanceResult.make(
                text: text, report: report, exactMatch: nil
            )
            result = freeResult.reclassified(
                overallClassification(free: freeResult.classification)
            )
            stage = .freeResult
        case nil:
            stage = .setup
        }
        settlement = nil
    }

    private func overallClassification(
        free: KeyboardTouchAcceptanceClassification
    ) -> KeyboardTouchAcceptanceClassification {
        let values = [guidedClassification, gestureClassification, free]
        if values.contains(.pipelineRegression) { return .pipelineRegression }
        if guidedClassification != .pass { return .typingMismatch }
        if gestureClassification != .pass { return .incompleteCoverage }
        return free
    }
}
#endif
