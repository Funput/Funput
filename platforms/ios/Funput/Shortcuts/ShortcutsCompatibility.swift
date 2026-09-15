import SwiftUI

/// Uses intrinsic height so a List row cannot stretch the empty-state action.
struct ShortcutsEmptyState<Actions: View>: View {
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
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 28)
        .fixedSize(horizontal: false, vertical: true)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }
}

struct ShortcutsAddButton: View {
    let action: () -> Void

    var body: some View {
        let button = Button(action: action) {
            Label("Thêm gõ tắt", systemImage: "plus")
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

struct ShortcutsListSpacing: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17, *) {
            content.listSectionSpacing(16).contentMargins(.top, 12, for: .scrollContent)
        } else {
            content
        }
    }
}
