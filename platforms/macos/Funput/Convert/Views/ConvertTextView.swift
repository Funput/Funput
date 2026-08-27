import SwiftUI

struct ConvertTextView: View {
    let state: ConvertScreenState
    let dispatch: ConvertDispatch

    var body: some View {
        VStack(spacing: Theme.Spacing.md) {
            selectors
            HStack(spacing: Theme.Spacing.md) {
                ConvertEditorPane(
                    title: state.fromFile ? state.fileName ?? "Tệp nguồn" : "Đang có",
                    isReadOnly: state.fromFile,
                    text: inputBinding
                )
                ConvertEditorPane(title: "Sẽ thành", isReadOnly: true, text: outputBinding)
            }
            if !state.warning.isEmpty {
                warning
            }
            footer
        }
    }

    private var selectors: some View {
        GlassEffectContainer(spacing: Theme.Spacing.md) {
            HStack(spacing: Theme.Spacing.md) {
                Text(state.source == nil ? "Chưa đoán được — chọn bảng mã:" : "Đây là")
                    .foregroundStyle(state.source == nil ? .orange : .secondary)
                ConvertCharsetPicker(
                    title: "Bảng mã nguồn", options: state.charsets,
                    allowsUnknown: true,
                    selection: sourceBinding
                )
                Spacer()
                Text("Sang").foregroundStyle(.secondary)
                ConvertCharsetPicker(
                    title: "Bảng mã đích", options: state.charsets,
                    selection: targetBinding
                )
            }
        }
    }

    private var warning: some View {
        Label(state.warning, systemImage: "exclamationmark.triangle.fill")
            .font(.callout)
            .foregroundStyle(.orange)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityIdentifier("convert.warning")
    }

    private var footer: some View {
        HStack(spacing: Theme.Spacing.sm) {
            ConvertStatus(text: state.progress, isBusy: state.isBusy)
            GlassEffectContainer(spacing: Theme.Spacing.sm) {
                HStack(spacing: Theme.Spacing.sm) {
                    Button("Chép kết quả", systemImage: "doc.on.doc", action: copy)
                        .buttonStyle(.glass)
                        .disabled(!state.canUseTextResult)
                    Button(state.textPrimaryAction, action: primaryAction)
                        .buttonStyle(.glassProminent)
                        .disabled(!state.canUseTextResult)
                }
            }
        }
    }

    private var inputBinding: Binding<String> {
        Binding(get: { state.inputText }, set: { dispatch(.setInput($0)) })
    }

    private var outputBinding: Binding<String> {
        .constant(state.outputText)
    }

    private var sourceBinding: Binding<Int?> {
        Binding(get: { state.source }, set: { dispatch(.setSource($0)) })
    }

    private var targetBinding: Binding<Int?> {
        Binding(get: { state.target }, set: { if let value = $0 { dispatch(.setTarget(value)) } })
    }

    private func copy() {
        dispatch(.copyResult)
    }

    private func primaryAction() {
        dispatch(state.fromFile ? .convertFiles : .saveResult)
    }
}

#Preview("Văn bản") {
    ConvertTextView(state: ConvertFixtures.pasted, dispatch: { _ in })
        .padding(Theme.Spacing.xl)
        .frame(width: 920, height: 520)
        .background(.windowBackground)
}

#Preview("Một tệp có cảnh báo") {
    ConvertTextView(state: ConvertFixtures.singleFile, dispatch: { _ in })
        .padding(Theme.Spacing.xl)
        .frame(width: 920, height: 520)
        .background(.windowBackground)
}
