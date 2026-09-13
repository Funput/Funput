import Foundation

/// Rust counts Unicode scalars; UIKit deletes extended grapheme clusters.
enum KeyboardReplacement {
    static func deletionCount(scalars: Int, buffer: String, context: String?) -> Int? {
        guard scalars > 0 else { return 0 }
        guard let context, !context.isEmpty,
              context.hasSuffix(buffer) || buffer.hasSuffix(context),
              let start = buffer.unicodeScalars.index(
                buffer.unicodeScalars.endIndex, offsetBy: -scalars,
                limitedBy: buffer.unicodeScalars.startIndex
              ), buffer.indices.contains(start) else { return nil }
        let count = buffer[start...].count
        return count <= context.count ? count : nil
    }
}
