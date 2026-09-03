import FunputShared
import SwiftUI

struct KeyboardAppearanceCard: View {
    @Binding var appearance: KeyboardAppearanceOption

    var body: some View {
        ContentCard {
            Label("Giao diện bàn phím", systemImage: "circle.lefthalf.filled")
                .font(.headline)
            Text("Bàn phím mặc định đi theo ứng dụng bạn đang gõ, nên ứng dụng chỉ có nền sáng sẽ giữ bàn phím sáng. Chọn Sáng hoặc Tối để cố định.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Picker("Giao diện bàn phím", selection: $appearance) {
                ForEach(KeyboardAppearanceOption.allCases, id: \.self) { option in
                    Text(option.title).tag(option)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("appearance.keyboardAppearance")
        }
    }
}

extension KeyboardAppearanceOption {
    var title: String {
        switch self {
        case .system: "Theo ứng dụng"
        case .light: "Sáng"
        case .dark: "Tối"
        }
    }
}
