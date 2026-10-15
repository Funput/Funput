import SwiftUI

/// One post in the news list: its kind and date, title, and a two-line summary.
struct NewsArticleRow: View {
    let article: NewsArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                if article.isGuide { NewsChip(text: "Hướng dẫn") }
                if let platform = article.platform { NewsChip(text: platform) }
                Spacer(minLength: 8)
                Text(article.published, format: Self.dateStyle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Text(article.title)
                .font(.headline)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.leading)
            if !article.summary.isEmpty {
                Text(article.summary)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 12)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Mở bài viết trong Safari")
    }

    // Posts are dated at midnight UTC; reading them in UTC keeps the day the site shows.
    private static let dateStyle = Date.FormatStyle(
        date: .abbreviated, time: .omitted, locale: Locale(identifier: "vi_VN"), timeZone: .gmt
    )
}

private struct NewsChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(.tint)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(.tint.opacity(0.12), in: Capsule())
    }
}
