import SwiftUI

struct KeyboardSetupCard: View {
    private let steps = [
        "Mở ứng dụng Cài đặt trên iPhone hoặc iPad.",
        "Chọn Cài đặt chung → Bàn phím → Các bàn phím.",
        "Chọn Thêm bàn phím mới → Funput.",
    ]

    var body: some View {
        AdaptiveGlassCard {
            Label("Bật bàn phím Funput", systemImage: "keyboard.badge.ellipsis")
                .font(.headline)
            Text("Chỉ cần thực hiện một lần để bắt đầu sử dụng Funput trong mọi ứng dụng.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(steps.enumerated()), id: \.offset) { index, step in
                    HStack(alignment: .top, spacing: 12) {
                        Text("\(index + 1)")
                            .font(.caption.bold())
                            .foregroundStyle(.tint)
                            .frame(width: 24, height: 24)
                            .background(.tint.opacity(0.12), in: .circle)
                        Text(step)
                            .font(.subheadline)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Hướng dẫn bật bàn phím Funput")
    }
}
