import SwiftUI

struct KeyboardSetupCard: View {
    let hasFullAccess: Bool
    let openSettings: () -> Void

    var body: some View {
        ContentCard {
            if hasFullAccess {
                readyStatus
            } else {
                Label(title, systemImage: statusIcon)
                    .font(.headline)
                Text(summary)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                setupSteps
                privacyNotice
                Button(action: openSettings) {
                    Label("Mở Cài đặt", systemImage: "gear")
                }
                    .settingsActionButtonStyle(prominent: true)
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

    /// Once set up, the card only confirms it: the explanation is one tap away instead of
    /// taking the top of the screen on every visit.
    private var readyStatus: some View {
        DisclosureGroup {
            privacyNotice
                .padding(.top, 10)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: statusIcon)
                    .font(.title3)
                    .foregroundStyle(Color.green)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.headline).foregroundStyle(Color.green)
                    Text(summary).font(.caption).foregroundStyle(.secondary)
                }
                // The disclosure label hands its content the width left beside the
                // chevron, and a summary long enough to wrap took the middle of it.
                .frame(maxWidth: .infinity, alignment: .leading)
                .multilineTextAlignment(.leading)
            }
        }
        .tint(.secondary)
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
                .background(.tint.opacity(0.12), in: Circle())
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
