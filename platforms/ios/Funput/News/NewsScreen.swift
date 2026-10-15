import SwiftUI

/// The news tab: the setup guide for this device, then the blog's posts.
struct NewsScreen: View {
    @ObservedObject var model: NewsModel
    @State private var reading: SafariDestination?

    static let guideURL = URL(string: "https://docs.funput.app/docs/install/ios/")!

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
            Button { reading = SafariDestination(url: Self.guideURL) } label: {
                SettingsRowLabel(
                    title: "Hướng dẫn cài đặt trên iPhone",
                    summary: "Thêm bàn phím, bật truy cập đầy đủ và gõ thử.",
                    systemImage: "book"
                )
            }
            .buttonStyle(SettingsRowButtonStyle())
            .accessibilityIdentifier("news.guide")
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
