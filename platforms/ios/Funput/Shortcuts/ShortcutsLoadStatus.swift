#if DEBUG
import SwiftUI

struct ShortcutsLoadStatus: View {
    let model: ShortcutsModel

    var body: some View {
        if model.isLoading {
            ProgressView("Đang đọc Gõ tắt…")
                .frame(maxWidth: .infinity, minHeight: 44)
        } else if let error = model.loadError {
            VStack(alignment: .leading, spacing: 12) {
                Label("Không thể mở Gõ tắt", systemImage: "exclamationmark.triangle")
                    .font(.headline)
                Text(error).font(.subheadline).foregroundStyle(.secondary)
                Button("Thử lại") { Task { await model.reload() } }
                    .frame(minHeight: 44)
                    .accessibilityIdentifier("shortcuts.retry")
            }
            .fixedSize(horizontal: false, vertical: true)
        }
    }
}
#endif
