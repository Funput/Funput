import SwiftUI

/// What the keyboard tab shows while searching: matching settings, each opening its page.
struct SettingsSearchResults: View {
    let query: String

    var body: some View {
        let results = SettingsSearchMatcher.matches(query)
        ContentCard {
            if results.isEmpty {
                Label("Không tìm thấy cài đặt", systemImage: "magnifyingglass")
                    .font(.headline)
                Text("Thử từ khác, ví dụ \"telex\", \"rung\" hoặc \"clipboard\".")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                VStack(spacing: 0) {
                    ForEach(results) { entry in
                        if entry.id != results.first?.id { SettingsRowDivider() }
                        NavigationLink(value: entry.destination) {
                            SettingsRowLabel(
                                title: entry.title,
                                summary: entry.destination.title,
                                systemImage: entry.systemImage
                            )
                        }
                        .buttonStyle(SettingsRowButtonStyle())
                        .accessibilityIdentifier("settings.search.result")
                    }
                }
            }
        }
    }
}
