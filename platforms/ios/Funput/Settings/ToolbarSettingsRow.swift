import SwiftUI

/// The one switch for the band above the keys.
///
/// Off means the rows and nothing above them, so it takes the suggestions and the
/// clipboard with it — and says so first: both are features someone may be using without
/// connecting them to the strip they appear in.
struct ToolbarSettingsRow: View {
    @ObservedObject var model: SettingsModel
    @State private var confirmsHiding = false

    var body: some View {
        SettingsToggleRow(
            title: "Thanh công cụ",
            summary: "Dải gợi ý, clipboard và emoji phía trên bàn phím.",
            systemImage: "rectangle.topthird.inset.filled",
            isOn: binding
        )
        // Anchored to the row rather than to the screen: iOS 26 points a confirmation
        // dialog at whatever presents it, the same reason `SettingsDataCard` hangs its
        // dialog off its button.
        .confirmationDialog(
            "Tắt thanh công cụ?",
            isPresented: $confirmsHiding,
            titleVisibility: .visible
        ) {
            Button("Tắt", role: .destructive) { model.setToolbarVisible(false) }
        } message: {
            Text("Gợi ý từ và lịch sử clipboard sẽ tắt theo. Phím emoji chuyển xuống hàng dưới.")
        }
    }

    /// Switching on restores both features. Switching off only opens the dialog, so the
    /// switch keeps reading from the configuration and snaps back if the user cancels.
    private var binding: Binding<Bool> {
        Binding(
            get: { model.showsToolbar },
            set: { isOn in
                if isOn {
                    model.setToolbarVisible(true)
                } else {
                    confirmsHiding = true
                }
            }
        )
    }
}
