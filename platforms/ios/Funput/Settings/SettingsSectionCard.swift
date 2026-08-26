import SwiftUI

struct SettingsSectionCard<Content: View>: View {
    let title: String
    let systemImage: String
    let content: Content

    init(title: String, systemImage: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.systemImage = systemImage
        self.content = content()
    }

    var body: some View {
        ContentCard {
            Label(title, systemImage: systemImage)
                .font(.headline)
            VStack(spacing: 0) {
                content
            }
        }
    }
}

struct SettingsRowDivider: View {
    var body: some View {
        Divider().padding(.leading, 34)
    }
}
