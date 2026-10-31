import SwiftUI

/// The empty state of a list page: a tinted symbol, a title, a line of help and its
/// actions. The in-house stand-in for `ContentUnavailableView`, which needs iOS 17.
///
/// Uses intrinsic height so a List row cannot stretch the empty-state action.
struct EmptyStateView<Actions: View>: View {
    let title: String
    let systemImage: String
    let summary: String
    @ViewBuilder var actions: () -> Actions

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: systemImage)
                .font(.system(size: 28, weight: .medium))
                .foregroundStyle(.tint)
                .frame(width: 64, height: 64)
                .background(Color.accentColor.opacity(0.1), in: RoundedRectangle(cornerRadius: 20))
                .accessibilityHidden(true)
            VStack(spacing: 8) {
                Text(title).font(.title3.bold()).accessibilityAddTraits(.isHeader)
                Text(summary).font(.subheadline).foregroundStyle(.secondary)
            }
            actions()
                .labelStyle(.titleAndIcon)
                .controlSize(.regular)
        }
        .multilineTextAlignment(.center)
        .accessibilityElement(children: .contain)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 28)
        .fixedSize(horizontal: false, vertical: true)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }
}

/// The add button a list page offers in its empty state: glass on iOS 26.
struct GlassAddButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        let button = Button(action: action) {
            Label(title, systemImage: "plus")
        }
        .labelStyle(.titleAndIcon)
        .controlSize(.regular)

        if #available(iOS 26, *) {
            button.buttonStyle(.glass)
        } else {
            button.buttonStyle(.bordered)
        }
    }
}

/// The section spacing every list page shares.
struct ListSectionSpacing: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17, *) {
            content.listSectionSpacing(16).contentMargins(.top, 12, for: .scrollContent)
        } else {
            content
        }
    }
}
