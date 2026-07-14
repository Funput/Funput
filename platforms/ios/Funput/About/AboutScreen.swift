import SwiftUI

struct AboutScreen: View {
    private let versionLabel = AppMetadata.versionLabel(from: Bundle.main.infoDictionary ?? [:])

    var body: some View {
        AppScreen {
            AboutHero(versionLabel: versionLabel)

            AboutLinkSection(
                title: "Tìm hiểu",
                systemImage: "safari.fill",
                destinations: AboutDestination.discovery
            )
            AboutLinkSection(
                title: "Cộng đồng & hỗ trợ",
                systemImage: "person.2.fill",
                destinations: AboutDestination.support
            )
            AboutLinkSection(
                title: "Quyền riêng tư",
                systemImage: "hand.raised.fill",
                destinations: AboutDestination.legal
            )
            AboutCommunityFooter()
        }
        .navigationTitle("Giới thiệu")
    }
}

#Preview("Giới thiệu · Light") {
    NavigationStack { AboutScreen() }
        .preferredColorScheme(.light)
}

#Preview("Giới thiệu · Dark") {
    NavigationStack { AboutScreen() }
        .preferredColorScheme(.dark)
}
