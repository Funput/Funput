import SwiftUI

struct SettingsResetCard: View {
    let action: () -> Void

    @State private var confirms = false

    var body: some View {
        ContentCard {
            VStack(alignment: .leading, spacing: 10) {
                Label("Bắt đầu lại", systemImage: "arrow.counterclockwise")
                    .font(.headline)
                Text("Khôi phục toàn bộ thiết lập bộ gõ về giá trị mặc định của Funput.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                resetButton
                    // Anchored to the button rather than to the screen: iOS 26 points a
                    // confirmation dialog at whatever presents it, so one hung off the
                    // root view surfaces at the top instead of beside its control.
                    .confirmationDialog(
                        "Khôi phục cài đặt mặc định?",
                        isPresented: $confirms,
                        titleVisibility: .visible
                    ) {
                        Button("Khôi phục", role: .destructive, action: action)
                    } message: {
                        Text("Các tùy chỉnh bộ gõ hiện tại sẽ bị thay thế.")
                    }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder private var resetButton: some View {
        if #available(iOS 26, *) {
            Button("Khôi phục cài đặt", role: .destructive) { confirms = true }
                .buttonStyle(.glass)
        } else {
            Button("Khôi phục cài đặt", role: .destructive) { confirms = true }
                .buttonStyle(.bordered)
        }
    }
}
