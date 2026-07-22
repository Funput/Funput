import SwiftUI

struct SettingsSidebar: View {
    @Binding var selection: SettingsDestination?

    var body: some View {
        VStack(spacing: 0) {
            SidebarBrandHeader()
                .padding(.horizontal, Theme.Spacing.md)
                .padding(.top, Theme.Spacing.sm)
                .padding(.bottom, Theme.Spacing.md)

            Divider()
                .padding(.horizontal, Theme.Spacing.md)

            List(selection: $selection) {
                ForEach(SettingsDestination.allCases) { destination in
                    Label(destination.title, systemImage: destination.systemImage)
                        .tag(destination)
                }
            }
            .listStyle(.sidebar)
        }
        .navigationSplitViewColumnWidth(Theme.sidebarWidth)
    }
}

private struct SidebarBrandHeader: View {
    var body: some View {
        HStack(spacing: Theme.Spacing.md) {
            AppLogo(size: 44, contentPadding: 0)
            Text("Funput")
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)
        }
        .padding(.horizontal, Theme.Spacing.md)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .center)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Funput")
    }
}
