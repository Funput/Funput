//! Everything this command says, as opposed to everything it converts.
//!
//! All of it goes to standard error, because standard output carries the converted
//! document and `funput convert … > out.txt` has to produce exactly that file.

use funput_core::charset::{self, Charset, Cost};

/// What to say when nothing could be guessed and nothing was declared.
pub(super) const UNDETECTED: &str =
    "cannot tell which charset this is — say so with --from (see --list)";

/// The charset table, for `--list`. Slug first: that is the word the user types.
pub(super) fn list() {
    let width = charset::ALL
        .iter()
        .map(|c| c.slug().len())
        .max()
        .unwrap_or(0);
    for charset in charset::ALL {
        println!("{:width$}  {}", charset.slug(), charset.name());
    }
}

/// The answer to `--detect`, on standard output because it *is* the answer here —
/// there is no document being written for it to get mixed up with.
pub(super) fn detected(charset: Charset) {
    println!("{}  {}", charset.slug(), charset.name());
}

pub(super) fn unknown_slug(slug: &str) -> String {
    let known: Vec<&str> = charset::ALL.iter().map(|c| c.slug()).collect();
    format!(
        "unknown charset {slug:?} — try one of: {}",
        known.join(", ")
    )
}

/// Warn about what did not survive, if anything did not.
///
/// Two different counts, and only one of them usually matters. `read` is characters
/// the *source* charset does not define, which means the file is damaged or `--from`
/// is wrong. `written` is characters the target cannot represent — an uppercase toned
/// vowel going to TCVN3, say, which has no code for one. Neither is fatal: a
/// character is never dropped, only spelled badly, so the document is still written
/// and the user is told where to look.
///
/// `normalized` is deliberately silent. Converting to Unicode tổ hợp respells every
/// toned vowel and none of it is a loss, so counting it would train the reader to
/// ignore this line.
pub(super) fn losses(cost: &Cost) {
    if cost.undefined > 0 {
        eprintln!(
            "warning: {} character(s) the source charset does not define — \
             the file may be damaged, or --from may be wrong",
            cost.undefined
        );
    }
    if cost.unrepresentable > 0 {
        eprintln!(
            "warning: {} character(s) the target charset cannot represent",
            cost.unrepresentable
        );
    }
}
