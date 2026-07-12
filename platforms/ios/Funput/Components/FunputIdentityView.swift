import SwiftUI
import UIKit

struct FunputIdentityView: View {
    var compact = false

    var body: some View {
        HStack(spacing: 16) {
            icon
            VStack(alignment: .leading, spacing: 4) {
                Text("Funput")
                    .font(compact ? .title2.bold() : .largeTitle.bold())
                Text("Bàn phím tiếng Việt hiện đại")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private var icon: some View {
        if let image = AppIconLoader.image {
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .clipShape(.rect(cornerRadius: compact ? 12 : 16))
                .frame(width: compact ? 52 : 68, height: compact ? 52 : 68)
        } else {
            Image(systemName: "keyboard.fill")
                .font(.system(size: compact ? 24 : 30, weight: .semibold))
                .foregroundStyle(.tint)
                .frame(width: compact ? 52 : 68, height: compact ? 52 : 68)
                .background(.tint.opacity(0.12), in: .rect(cornerRadius: compact ? 12 : 16))
        }
    }
}

private enum AppIconLoader {
    static let image: UIImage? = {
        guard let icons = Bundle.main.infoDictionary?["CFBundleIcons"] as? [String: Any],
              let primary = icons["CFBundlePrimaryIcon"] as? [String: Any],
              let files = primary["CFBundleIconFiles"] as? [String],
              let name = files.last
        else { return nil }
        return UIImage(named: name)
    }()
}
