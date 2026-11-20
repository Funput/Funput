import SwiftUI

/// The corner button that brings the docked preview back after it was closed.
struct AppearancePreviewOpenButton: View {
    let action: () -> Void

    var body: some View {
        styled
            .accessibilityLabel("Hiện bàn phím xem trước")
            .accessibilityIdentifier("appearance.showPreview")
    }

    @ViewBuilder private var styled: some View {
        if #available(iOS 26, *) {
            Button(action: action) { icon }
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .controlSize(.large)
        } else {
            Button(action: action) {
                icon
                    .padding(12)
                    .background(.regularMaterial, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    private var icon: some View {
        Image(systemName: "keyboard")
            .font(.title3.weight(.semibold))
            .frame(width: 28, height: 28)
    }
}
