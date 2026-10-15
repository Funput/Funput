import SwiftUI

/// Loading, or the reason the feed could not be refreshed, with a way to try again.
struct NewsStatusCard: View {
    @ObservedObject var model: NewsModel

    var body: some View {
        ContentCard {
            if model.phase == .failed {
                Label(title, systemImage: "wifi.exclamationmark")
                    .font(.headline)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Button {
                    Task { await model.refresh() }
                } label: {
                    Label("Thử lại", systemImage: "arrow.clockwise")
                }
                .settingsActionButtonStyle()
                .accessibilityIdentifier("news.retry")
            } else {
                ProgressView("Đang tải bài viết…")
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var title: String {
        model.articles.isEmpty ? "Chưa tải được bài viết" : "Chưa cập nhật được"
    }

    private var message: String {
        model.articles.isEmpty
            ? "Kiểm tra kết nối mạng rồi thử lại."
            : "Đang hiện các bài đã lưu trên máy. Kiểm tra kết nối mạng rồi thử lại."
    }
}
