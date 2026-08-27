import SwiftUI

struct ConvertHeader: View {
    let showsRestart: Bool
    let restart: () -> Void

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Theme.Spacing.lg) {
            VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
                Text("Chuyển mã")
                    .font(.largeTitle.bold())
                Text("Chuyển văn bản giữa Unicode và các bảng mã tiếng Việt cũ.")
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if showsRestart {
                Button("Bắt đầu lại", systemImage: "arrow.counterclockwise", action: restart)
                    .buttonStyle(.glass)
                    .help("Xóa nội dung và quay lại từ đầu")
                    .accessibilityIdentifier("convert.restart")
            }
        }
    }
}

struct ConvertCharsetPicker: View {
    let title: String
    let options: [ConvertCharset]
    var allowsUnknown = false
    var usesGlass = true
    @Binding var selection: Int?

    @ViewBuilder
    var body: some View {
        if usesGlass {
            picker
                .padding(.horizontal, Theme.Spacing.sm)
                .glassEffect(.regular.interactive(), in: .rect(cornerRadius: Theme.Radius.control))
        } else {
            picker
        }
    }

    private var picker: some View {
        Picker(title, selection: $selection) {
            if allowsUnknown {
                Text("Chọn bảng mã…").tag(Int?.none)
                    .disabled(true)
            }
            ForEach(options) { option in
                Text(option.name).tag(Optional(option.id))
            }
        }
        .pickerStyle(.menu)
        .labelsHidden()
        .tint(.primary)
        .accessibilityLabel(title)
        .accessibilityValue(selectedName)
    }

    private var selectedName: String {
        options.first(where: { $0.id == selection })?.name ?? "Chưa chọn"
    }
}

struct ConvertEditorPane: View {
    let title: String
    let isReadOnly: Bool
    @Binding var text: String

    var body: some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
            Label(title, systemImage: isReadOnly ? "eye" : "pencil")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
            TextEditor(text: $text)
                .font(.body)
                .textEditorStyle(.plain)
                .scrollContentBackground(.hidden)
                .padding(Theme.Spacing.sm)
                .background(.background.opacity(0.72), in: paneShape)
                .overlay { paneShape.strokeBorder(.separator.opacity(0.3), lineWidth: 0.5) }
                .disabled(isReadOnly)
                .accessibilityLabel(title)
        }
    }

    private var paneShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: Theme.Radius.control, style: .continuous)
    }
}

struct ConvertStatus: View {
    let text: String
    var isBusy = false

    var body: some View {
        HStack(spacing: Theme.Spacing.xs) {
            if !text.isEmpty {
                if isBusy {
                    ProgressView().controlSize(.small)
                } else {
                    Image(systemName: "checkmark.circle").foregroundStyle(.secondary)
                }
            }
            Text(text)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .truncationMode(.middle)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}
