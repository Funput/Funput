#if DEBUG
import Combine
import Foundation
import FunputShared
import KeyboardLayout
import UIKit

@MainActor
final class ShadowTypingHarnessModel: ObservableObject {
    enum Stage: Equatable {
        case setup, guided, guidedSettling, guidedResult, free, freeResult
    }

    @Published var selectedMethod: KeyboardInputMethod = .vni
    @Published var selectedPipeline = KeyboardTouchDiagnosticPipelineMode.v2
    @Published var stage: Stage = .setup
    @Published var text = ""
    @Published var wantsFocus = false
    @Published var report: KeyboardTouchDiagnosticReport?
    @Published var result: ShadowHarnessResult?
    @Published var guidedProgress = ShadowGuidedProgress()
    @Published var freeSecondsRemaining = 60
    @Published var hasFullAccess = false

    let diagnosticStore: KeyboardTouchDiagnosticStore
    let overrideStore: FunputUITestConfigurationOverrideStore
    let accessCheck: () -> Bool
    var timer: Timer?
    var freeDeadline: Date?
    var settlementDeadline: Date?
    var settlementAfterSequence: UInt64 = 0
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

    var fixture: ShadowTypingFixture {
        ShadowTypingFixture.all.first { $0.inputMethod == selectedMethod }!
    }

    var guidedStep: ShadowTypingStep {
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
        if stage == .guidedSettling, let deadline = settlementDeadline,
           receivedFinal || now >= deadline {
            settlementDeadline = nil
            result = ShadowHarnessResult.make(
                text: guidedProgress.verifiedText,
                report: report,
                exactMatch: guidedProgress.verifiedText
                    == ShadowTypingFixture.expected
            )
            stage = .guidedResult
        }
        if stage == .free, let deadline = freeDeadline {
            freeSecondsRemaining = max(
                0,
                Int(ceil(deadline.timeIntervalSince(now)))
            )
            if now >= deadline { stopFree() }
        }
    }
}
#endif
