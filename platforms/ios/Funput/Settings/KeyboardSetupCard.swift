import SwiftUI

struct KeyboardSetupCard: View {
    let hasFullAccess: Bool
    let openSettings: () -> Void

    var body: some View {
        ContentCard {
            Label(title, systemImage: statusIcon)
                .font(.headline)
                .foregroundStyle(hasFullAccess ? Color.green : Color.primary)
            Text(summary)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            if hasFullAccess {
                privacyNotice
            } else {
                setupSteps
                privacyNotice
                Button("Mở Cài đặt", systemImage: "gear", action: openSettings)
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityHint("Mở cài đặt của Funput để bật bàn phím và truy cập đầy đủ")
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    private var title: String {
        hasFullAccess ? "Funput đã sẵn sàng" : "Hoàn tất thiết lập Funput"
    }

    private var statusIcon: String {
        hasFullAccess ? "checkmark.seal.fill" : "keyboard.badge.ellipsis"
    }

    private var summary: String {
        if hasFullAccess {
            return "Cho phép truy cập đầy đủ đã được bàn phím xác nhận."
        }
        return "Thực hiện ba bước để dùng đầy đủ tính năng trong mọi ứng dụng."
    }

    private var setupSteps: some View {
        VStack(alignment: .leading, spacing: 12) {
            step(1, "Thêm bàn phím Funput", "Cài đặt chung → Bàn phím → Các bàn phím.")
            step(2, "Bật Cho phép truy cập đầy đủ", "Chọn Funput trong danh sách bàn phím.")
            step(3, "Mở Funput một lần", "Chuyển sang Funput trong bất kỳ ô nhập nào để xác nhận.")
        }
    }

    private var privacyNotice: some View {
        Label {
            Text("Quyền này cho phép lưu cài đặt và gợi ý cá nhân trên thiết bị. Funput không gửi nội dung bạn gõ ra ngoài.")
        } icon: {
            Image(systemName: "lock.shield.fill")
                .foregroundStyle(.tint)
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .fixedSize(horizontal: false, vertical: true)
    }

    private func step(_ number: Int, _ title: String, _ detail: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text("\(number)")
                .font(.caption.bold())
                .foregroundStyle(.tint)
                .frame(width: 24, height: 24)
                .background(.tint.opacity(0.12), in: .circle)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(detail).font(.caption).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("Cần Full Access") {
    KeyboardSetupCard(hasFullAccess: false, openSettings: {})
        .padding()
}

#Preview("Đã có Full Access") {
    KeyboardSetupCard(hasFullAccess: true, openSettings: {})
        .padding()
}
