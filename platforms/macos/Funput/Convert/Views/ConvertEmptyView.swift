import SwiftUI

struct ConvertEmptyView: View {
    let dispatch: ConvertDispatch

    var body: some View {
        VStack(spacing: Theme.Spacing.lg) {
            Image(systemName: "arrow.left.arrow.right.circle.fill")
                .font(.system(size: 52, weight: .medium))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(Theme.accent)

            VStack(spacing: Theme.Spacing.sm) {
                Text("Thả tệp vào đây, hoặc dán văn bản")
                    .font(.title2.bold())
                Text("Funput tự nhận ra bảng mã — không cần chọn nguồn.")
                    .foregroundStyle(.secondary)
                Text("Có thể chuyển nhiều tệp cùng lúc; bản chuyển đổi sẽ nằm trong thư mục Đã chuyển mã cạnh tệp nguồn.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 560)
            }

            GlassEffectContainer(spacing: Theme.Spacing.sm) {
                HStack(spacing: Theme.Spacing.sm) {
                    Button("Dán văn bản", systemImage: "doc.on.clipboard", action: paste)
                        .buttonStyle(.glassProminent)
                        .keyboardShortcut("v", modifiers: .command)
                        .accessibilityIdentifier("convert.paste")
                    Button("Chọn tệp…", systemImage: "doc.badge.plus", action: pickFiles)
                        .buttonStyle(.glass)
                        .accessibilityIdentifier("convert.pickFiles")
                }
            }
        }
        .padding(Theme.Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(.quaternary.opacity(0.34), in: dropShape)
        .overlay { dropShape.strokeBorder(.separator.opacity(0.35), style: dashedStroke) }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Khu vực thêm nội dung cần chuyển mã")
    }

    private var dropShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: Theme.Radius.card, style: .continuous)
    }

    private var dashedStroke: StrokeStyle {
        StrokeStyle(lineWidth: 1, dash: [7, 5])
    }

    private func paste() {
        dispatch(.paste)
    }

    private func pickFiles() {
        dispatch(.pickFiles)
    }
}

#Preview("Trống") {
    ConvertEmptyView(dispatch: { _ in })
        .padding(Theme.Spacing.xl)
        .frame(width: 920, height: 520)
        .background(.windowBackground)
}
