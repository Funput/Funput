import SwiftUI

struct AboutLinkSection: View {
    let title: String
    let systemImage: String
    let destinations: [AboutDestination]

    var body: some View {
        AdaptiveGlassCard {
            Label(title, systemImage: systemImage)
                .font(.headline)
            VStack(spacing: 0) {
                ForEach(destinations) { destination in
                    AboutLinkRow(destination: destination)
                    if destination.id != destinations.last?.id {
                        Divider().padding(.leading, 48)
                    }
                }
            }
        }
    }
}

private struct AboutLinkRow: View {
    let destination: AboutDestination

    var body: some View {
        Link(destination: destination.url) {
            HStack(spacing: 12) {
                Image(systemName: destination.systemImage)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(destination.tint)
                    .frame(width: 36, height: 36)
                    .background(destination.tint.opacity(0.12), in: .rect(cornerRadius: 10))
                VStack(alignment: .leading, spacing: 2) {
                    Text(destination.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                    Text(destination.summary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                Image(systemName: "arrow.up.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .contentShape(.rect)
            .padding(.vertical, 9)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("about.\(destination.id)")
        .accessibilityHint("Mở liên kết ngoài")
    }
}

struct AboutCommunityFooter: View {
    var body: some View {
        VStack(spacing: 6) {
            Label("Được xây dựng cho cộng đồng", systemImage: "heart.fill")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            Text("Funput là dự án mã nguồn mở & miễn phí cho tất cả mọi người.")
                .font(.caption)
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}
