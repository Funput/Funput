import SwiftUI

/// The "Tôi" tab: the data Funput keeps on this device, then where Funput lives.
struct MeScreen: View {
    let settings: SettingsModel
    private let versionLabel = AppMetadata.versionLabel(from: Bundle.main.infoDictionary ?? [:])

    var body: some View {
        AppScreen {
            AboutHero(versionLabel: versionLabel)
            ForEach(MeSection.allCases) { section in
                content(for: section)
            }
            AboutCommunityFooter()
        }
        .navigationTitle(AppTab.me.title)
        .settingsDestinations(model: settings)
    }

    @ViewBuilder private func content(for section: MeSection) -> some View {
        switch section {
        case .data:
            SettingsNavigationCard(destinations: [.data], model: settings)
        case .discovery:
            AboutLinkSection(title: "Tìm hiểu", systemImage: "safari.fill", destinations: AboutDestination.discovery)
        case .support:
            AboutLinkSection(
                title: "Cộng đồng & hỗ trợ",
                systemImage: "person.2.fill",
                destinations: AboutDestination.support
            )
        case .legal:
            AboutLinkSection(title: "Quyền riêng tư", systemImage: "hand.raised.fill", destinations: AboutDestination.legal)
        case .licenses:
            ContentCard {
                NavigationLink {
                    ThirdPartyLicensesScreen()
                } label: {
                    Label("Giấy phép bên thứ ba", systemImage: "doc.text")
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 9)
                }
                .accessibilityIdentifier("about.licenses")
            }
        }
    }
}

#Preview("Tôi · Light") {
    NavigationStack { MeScreen(settings: SettingsModel(store: PreviewConfigurationStore())) }
        .preferredColorScheme(.light)
}

#Preview("Tôi · Dark") {
    NavigationStack { MeScreen(settings: SettingsModel(store: PreviewConfigurationStore())) }
        .preferredColorScheme(.dark)
}
