import SwiftUI

/// Keeps the same empty-state actions on the minimum supported iOS version.
struct ShortcutsEmptyState<Actions: View>: View {
    let title: String
    let systemImage: String
    let summary: String
    @ViewBuilder var actions: () -> Actions

    var body: some View {
        if #available(iOS 17, *) {
            ContentUnavailableView {
                Label(title, systemImage: systemImage)
            } description: {
                Text(summary)
            } actions: {
                actions()
            }
        } else {
            VStack(spacing: 16) {
                Image(systemName: systemImage)
                    .font(.largeTitle).foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                Text(title).font(.title2.bold()).accessibilityAddTraits(.isHeader)
                Text(summary).font(.subheadline).foregroundStyle(.secondary)
                actions()
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
        }
    }
}

struct ShortcutsListSpacing: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17, *) {
            content.listSectionSpacing(16).contentMargins(.top, 12, for: .scrollContent)
        } else {
            content
        }
    }
}
