import FunputShared
import SwiftUI

/// What stands between the user and a dictionary that learns: Full Access, the
/// suggestion switch, or a wipe the keyboard has not carried out yet. Each notice
/// says why and offers the one thing that fixes it.
struct DictionaryNotices: View {
    @Environment(\.openURL) private var openURL
    @ObservedObject var settings: SettingsModel

    var body: some View {
        if !settings.hasFullAccess {
            notice(
                "Funput chỉ học và lưu từ khi bàn phím được Cho phép truy cập đầy đủ.",
                systemImage: "lock.open",
                tint: .orange,
                action: ("Mở Cài đặt", { openURL(.funputSystemSettings) })
            )
            .accessibilityIdentifier("dictionary.notice.fullAccess")
        }
        if !settings.configuration.personalSuggestionsEnabled {
            notice(
                "Gợi ý từ đang tắt. Từ điển vẫn được giữ và dùng lại khi bạn bật.",
                systemImage: "text.badge.star",
                tint: .accentColor,
                action: ("Bật gợi ý", { settings.boolBinding(\.personalSuggestionsEnabled).wrappedValue = true })
            )
            .accessibilityIdentifier("dictionary.notice.disabled")
        }
        if settings.personalSuggestionResetIsPending {
            notice(
                "Các từ đã học sẽ được xoá khi bạn mở bàn phím lần tới. Danh sách từ bị chặn vẫn được giữ.",
                systemImage: "clock.arrow.circlepath",
                tint: .secondary,
                action: nil
            )
            .accessibilityIdentifier("dictionary.notice.resetPending")
        }
    }

    private func notice(
        _ message: String,
        systemImage: String,
        tint: Color,
        action: (title: String, run: () -> Void)?
    ) -> some View {
        ContentCard(cornerRadius: 18) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: systemImage)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(tint)
                    .frame(width: 28, height: 28)
                    .background(tint.opacity(0.14), in: RoundedRectangle(cornerRadius: 8))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 10) {
                    Text(message).font(.subheadline).fixedSize(horizontal: false, vertical: true)
                    if let action {
                        Button(action.title, action: action.run)
                            .settingsActionButtonStyle()
                            .controlSize(.small)
                    }
                }
            }
        }
        .listRowInsets(EdgeInsets())
        .listRowBackground(Color.clear)
    }
}
