import SwiftUI
import UIKit

struct FunputIdentityView: View {
    var compact = false
    var hero = false

    var body: some View {
        if hero {
            VStack(spacing: 10) {
                Image("FunputLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 160, height: 160)
                    .accessibilityHidden(true)
                Text("Funput")
                    .font(.largeTitle.bold())
                Text("Bộ gõ tiếng Việt đa nền tảng - mã nguồn mở")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        } else {
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
    }

    @ViewBuilder
    private var icon: some View {
        if let image = AppIconLoader.image {
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .clipShape(.rect(cornerRadius: iconCornerRadius))
                .frame(width: iconSize, height: iconSize)
        } else {
            Image(systemName: "keyboard.fill")
                .font(.system(size: iconSize * 0.44, weight: .semibold))
                .foregroundStyle(.tint)
                .frame(width: iconSize, height: iconSize)
                .background(.tint.opacity(0.12), in: .rect(cornerRadius: iconCornerRadius))
        }
    }

    private var iconSize: CGFloat { hero ? 112 : (compact ? 52 : 68) }
    private var iconCornerRadius: CGFloat { hero ? 26 : (compact ? 12 : 16) }
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
