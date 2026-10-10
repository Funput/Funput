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

    /// The same conversion for an edit that replaces text already committed, where
    /// the document itself is the only record of what is being deleted.
    ///
    /// A word boundary clears the composer, so a correction answered after one has no
    /// buffer to measure against — only the context behind the caret.
    static func deletionCount(scalars: Int, context: String?) -> Int? {
        guard scalars > 0 else { return 0 }
        guard let context, !context.isEmpty,
              let start = context.unicodeScalars.index(
                context.unicodeScalars.endIndex, offsetBy: -scalars,
                limitedBy: context.unicodeScalars.startIndex
              ), context.indices.contains(start) else { return nil }
        return context[start...].count
    }
}
