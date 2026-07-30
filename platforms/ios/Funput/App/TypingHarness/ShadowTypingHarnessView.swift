#if DEBUG
import FunputShared
import SwiftUI
import UIKit

struct ShadowTypingHarnessView: View {
    @StateObject private var model = ShadowTypingHarnessModel()
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase
    var body: some View {
        NavigationStack {
            Form {
                setupSection
                stageContent
                if let report = model.report {
                    MetricsSection(report: report)
                    Button("Copy numeric JSON report") {
                        model.copyNumericReport()
                    }
                }
            }
            .navigationTitle("Touch Shadow")
            .onAppear { model.appear() }
            .onDisappear { model.disappear() }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { model.appear() }
            }
        }
    }

    private var setupSection: some View {
        Section("Session") {
            Picker("Input method", selection: $model.selectedMethod) {
                ForEach(ShadowTypingFixture.all) { fixture in
                    Text(fixture.title).tag(fixture.inputMethod)
                }
            }
            .disabled(
                model.stage == .guided
                    || model.stage == .guidedSettling
                    || model.stage == .free
            )
            if !model.hasFullAccess {
                Label("Cần bật Full Access để nhận live report.", systemImage: "exclamationmark.triangle")
                    .foregroundStyle(.orange)
                Button("Mở Cài đặt") {
                    openURL(URL(string: UIApplication.openSettingsURLString)!)
                }
            }
        }
    }

    @ViewBuilder
    private var stageContent: some View {
        switch model.stage {
        case .setup:
            Section("Guided từng cụm") {
                Text("Mỗi lượt 2–5 từ. Nếu sai, bấm Retry thay vì dùng backspace.")
                Button("Start Guided") { model.startGuided() }
                    .disabled(!model.hasFullAccess)
            }
        case .guided:
            guidedSection
        case .guidedSettling:
            Section("Đang tổng hợp") {
                ProgressView()
                Text("Chờ shadow report settle, tối đa 1 giây.")
            }
        case .guidedResult:
            resultSection
            Section {
                Button("Start 60-second Free Stress") { model.startFree() }
                Button("Đổi input method") { model.returnToSetup() }
            }
        case .free:
            Section("Free stress · \(model.freeSecondsRemaining)s") {
                textField
                Button("Stop Early", role: .destructive) { model.stopFree() }
            }
        case .freeResult:
            resultSection
            Section {
                Button("Chạy mode tiếp theo") { selectNextMethod() }
                Button("Chạy lại Free Stress") { model.startFree() }
            }
        }
    }

    private var guidedSection: some View {
        let step = model.guidedStep
        return Section(
            "Guided \(model.guidedProgress.currentIndex + 1)/\(model.fixture.steps.count)"
        ) {
            ProgressView(
                value: Double(model.guidedProgress.currentIndex + 1),
                total: Double(model.fixture.steps.count)
            )
            LabeledContent("Kết quả cần có", value: step.expected)
            Text(step.rawSequence)
                .font(.system(.body, design: .monospaced))
                .textSelection(.enabled)
            textField
            if let mismatch = model.guidedProgress.mismatchIndex {
                Label(
                    "Chưa khớp từ ký tự \(mismatch + 1). Hãy thử lại cụm này.",
                    systemImage: "xmark.circle"
                )
                .foregroundStyle(.orange)
                Button("Retry current phrase") { model.retryGuidedStep() }
            } else {
                Button("Check & Continue") { model.checkGuidedStep() }
            }
        }
    }

    private var textField: some View {
        ShadowTypingTextView(text: $model.text, wantsFocus: $model.wantsFocus)
            .frame(minHeight: 180)
    }

    private var resultSection: some View {
        Section("Result") {
            if let result = model.result {
                Text(result.classification.rawValue)
                    .font(.headline)
                if let exact = result.exactMatch {
                    LabeledContent("Exact output", value: exact ? "Yes" : "No")
                }
                LabeledContent("Characters", value: "\(result.characterCount)")
                if let index = result.firstMismatchIndex {
                    LabeledContent("First mismatch", value: "\(index)")
                }
            }
        }
    }

    private func selectNextMethod() {
        let methods = ShadowTypingFixture.all.map(\.inputMethod)
        let index = methods.firstIndex(of: model.selectedMethod) ?? 0
        model.selectedMethod = methods[(index + 1) % methods.count]
        model.returnToSetup()
    }
}
#endif
