import SwiftUI

struct ConvertBatchView: View {
    let state: ConvertScreenState
    let dispatch: ConvertDispatch

    var body: some View {
        VStack(spacing: Theme.Spacing.md) {
            header
            Table(state.files) {
                TableColumn("Tệp") { row in
                    Label(row.name, systemImage: "doc.text")
                        .lineLimit(1)
                }
                .width(min: 220, ideal: 320)

                TableColumn("Bảng mã nguồn") { row in
                    ConvertCharsetPicker(
                        title: "Bảng mã của \(row.name)",
                        options: state.charsets,
                        allowsUnknown: true,
                        usesGlass: false,
                        selection: sourceBinding(for: row)
                    )
                }
                .width(min: 170, ideal: 210)

                TableColumn("Cảnh báo") { row in
                    Text(row.note.isEmpty ? "—" : row.note)
                        .foregroundStyle(row.note.isEmpty ? Color.secondary.opacity(0.45) : .orange)
                        .lineLimit(1)
                }
                .width(min: 150, ideal: 200)
            }
            .accessibilityIdentifier("convert.files")
            footer
        }
    }

    private var header: some View {
        HStack(spacing: Theme.Spacing.sm) {
            Label("\(state.rowsTotal) tệp", systemImage: "doc.on.doc")
                .foregroundStyle(.secondary)
            Spacer()
            Text("Sang").foregroundStyle(.secondary)
            ConvertCharsetPicker(
                title: "Bảng mã đích", options: state.charsets,
                selection: targetBinding
            )
        }
    }

    private var footer: some View {
        HStack(spacing: Theme.Spacing.sm) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                ConvertStatus(text: statusText, isBusy: state.isBusy)
                if state.progress.isEmpty {
                    Label(state.outputDirectory, systemImage: "folder")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            GlassEffectContainer(spacing: Theme.Spacing.sm) {
                HStack(spacing: Theme.Spacing.sm) {
                    if state.canLoadMore {
                        Button("Hiển thị thêm", action: loadMore)
                            .buttonStyle(.glass)
                    }
                    Button(state.batchAction, action: convert)
                        .buttonStyle(.glassProminent)
                        .disabled(state.ready == 0 || state.isBusy)
                        .accessibilityIdentifier("convert.batchAction")
                }
            }
        }
    }

    private var targetBinding: Binding<Int?> {
        Binding(get: { state.target }, set: { if let value = $0 { dispatch(.setTarget(value)) } })
    }

    private func sourceBinding(for row: ConvertFileRow) -> Binding<Int?> {
        Binding(
            get: { row.source },
            set: { dispatch(.setRowSource(id: row.id, source: $0)) }
        )
    }

    private func convert() {
        dispatch(.convertFiles)
    }

    private func loadMore() {
        dispatch(.loadMore)
    }

    private var statusText: String {
        if !state.progress.isEmpty { return state.progress }
        if !state.unreadable.isEmpty { return state.unreadable }
        return state.files.count < state.rowsTotal ? "Đang hiện \(state.files.count)/\(state.rowsTotal) tệp" : ""
    }
}

#Preview("Nhiều tệp") {
    ConvertBatchView(state: ConvertFixtures.batch, dispatch: { _ in })
        .padding(Theme.Spacing.xl)
        .frame(width: 920, height: 520)
        .background(.windowBackground)
}

#Preview("Đang chuyển") {
    ConvertBatchView(state: ConvertFixtures.busyBatch, dispatch: { _ in })
        .padding(Theme.Spacing.xl)
        .frame(width: 920, height: 520)
        .background(.windowBackground)
}
