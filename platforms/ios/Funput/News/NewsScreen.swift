import SwiftUI

/// The news tab: the pinned guides, then the blog's posts.
struct NewsScreen: View {
    @ObservedObject var model: NewsModel
    @State private var reading: SafariDestination?

    var body: some View {
        AppScreen {
            guideCard
            NewsArticleList(model: model) { reading = SafariDestination(url: $0.url) }
        }
        .navigationTitle(AppTab.news.title)
        .refreshable { await model.refresh() }
        .task { await model.loadIfNeeded() }
        .sheet(item: $reading) { SafariView(url: $0.url).ignoresSafeArea() }
    }

    private var guideCard: some View {
        ContentCard {
            VStack(spacing: 0) {
                ForEach(NewsGuide.pinned) { guide in
                    if guide.id != NewsGuide.pinned.first?.id { SettingsRowDivider() }
                    Button { reading = SafariDestination(url: guide.url) } label: {
                        SettingsRowLabel(title: guide.title, summary: guide.summary, systemImage: guide.systemImage)
                    }
                    .buttonStyle(SettingsRowButtonStyle())
                    .accessibilityIdentifier("news.guide.\(guide.id)")
                }
            }
        }
    }
}

/// The posts, or why there are none yet.
private struct NewsArticleList: View {
    @ObservedObject var model: NewsModel
    let open: (NewsArticle) -> Void

    var body: some View {
        if model.articles.isEmpty {
            NewsStatusCard(model: model)
        } else {
            ContentCard {
                VStack(spacing: 0) {
                    ForEach(model.articles) { article in
                        if article.id != model.articles.first?.id { Divider() }
                        Button { open(article) } label: { NewsArticleRow(article: article) }
                            .buttonStyle(SettingsRowButtonStyle())
                            .accessibilityIdentifier("news.article.\(article.id)")
                    }
                }
            }
            if model.phase == .failed { NewsStatusCard(model: model) }
        }
    }
}
