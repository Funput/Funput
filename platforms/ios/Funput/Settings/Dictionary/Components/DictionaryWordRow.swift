import PersonalDictionary
import SwiftUI
import UIKit

/// One word: the word itself, what the dictionary knows about it, and a thin bar for
/// how often it is used next to the most used word in the list.
///
/// Swipe and the context menu offer the same edits. Blocking applies at once, since
/// the blocked list undoes it; forgetting loses the count, so it asks first.
struct DictionaryWordRow: View {
    let word: DictionaryWord
    let highestUses: Int
    let block: () -> Void
    let unblock: () -> Void
    let forget: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(word.text).font(.body.weight(.semibold))
                Text(word.detail).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            trailing
        }
        .frame(minHeight: 44)
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(word.accessibilityLabel)
        .accessibilityIdentifier("dictionary.word.\(word.text)")
        .swipeActions(edge: .trailing, allowsFullSwipe: false) { swipeButtons }
        .contextMenu { menuButtons }
    }

    @ViewBuilder private var trailing: some View {
        switch word.origin {
        case .learned:
            UsageBar(fraction: highestUses > 0 ? Double(word.uses) / Double(highestUses) : 0)
        case .added:
            Text("Tự thêm")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tint)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color.accentColor.opacity(0.12), in: Capsule())
        case .blocked:
            Image(systemName: "nosign").foregroundStyle(.secondary).accessibilityHidden(true)
        }
    }

    @ViewBuilder private var swipeButtons: some View {
        if word.origin == .blocked {
            Button(action: unblock) { Label("Bỏ chặn", systemImage: "arrow.uturn.backward") }
                .tint(.accentColor)
        } else {
            Button(role: .destructive, action: forget) { Label("Xoá", systemImage: "trash") }
                .tint(.red)
            Button(action: block) { Label("Chặn", systemImage: "nosign") }.tint(.orange)
        }
    }

    @ViewBuilder private var menuButtons: some View {
        Button { UIPasteboard.general.string = word.text } label: {
            Label("Sao chép", systemImage: "doc.on.doc")
        }
        if word.origin == .blocked {
            Button(action: unblock) { Label("Bỏ chặn", systemImage: "arrow.uturn.backward") }
        } else {
            Button(action: block) { Label("Không gợi ý từ này", systemImage: "nosign") }
            Button(role: .destructive, action: forget) {
                Label("Xoá khỏi từ điển", systemImage: "trash")
            }
        }
    }
}

/// How used a word is, as a short capsule filled in the accent colour. Never quite
/// empty, so the rarest word in the list still shows it has been used.
private struct UsageBar: View {
    let fraction: Double
    private let width: CGFloat = 44

    var body: some View {
        ZStack(alignment: .leading) {
            Capsule().fill(Color.accentColor.opacity(0.15))
            Capsule().fill(Color.accentColor).frame(width: width * max(0.1, min(fraction, 1)))
        }
        .frame(width: width, height: 6)
        .accessibilityHidden(true)
    }
}
