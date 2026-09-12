//! `funput case`: changing the case of a document (chuyển đổi kiểu chữ).
//!
//! Five transforms over text that already exists — UPPERCASE, lowercase, bỏ dấu,
//! sentence case, title case — as a filter: a file or standard input goes in, the
//! transformed text comes out, and everything else goes to standard error. The
//! rules are `funput_core::textcase`'s; see `docs/features/text-case.md`.
//!
//! **Transforms stack, in the order they are typed.** `lower,title` is how a user
//! reaches `Gửi Về Tp. Hcm` from `GỬI VỀ TP. HCM`, because title case on its own
//! protects words that are already all-caps — that is what it is for. The same
//! stacking is what the converter window offers with two button presses.
//!
//! **Legacy charsets are refused rather than mangled.** A `.VnTime` document is not
//! Unicode text: it is code points `U+0020..=U+00FF` standing in for TCVN3 bytes,
//! and uppercasing those would produce a document nothing can read. Doing it
//! properly means decoding, transforming and re-encoding — and re-encoding can lose
//! letters, because TCVN3 has no room for uppercase toned vowels. That warning
//! belongs to a window with somewhere to show it, not to a command whose standard
//! output is the document. So this says what the file looks like and points at
//! `funput convert`.

mod args;

use std::process::ExitCode;

use funput_core::charset::{self, Charset, document::Document};
use funput_core::textcase::apply;

use crate::cli::{CliError, CliResult};
use crate::io::{read, write};

pub use args::CaseArgs;

/// Run `funput case`.
pub fn run(args: CaseArgs) -> CliResult {
    let text = readable(read(args.file.as_deref())?)?;
    let options = args.options();
    let transformed = args
        .transforms
        .iter()
        .fold(text, |text, &arg| apply(&text, arg.into(), options));
    write(transformed.as_bytes())?;
    Ok(ExitCode::SUCCESS)
}

/// The text of a document this command is willing to touch.
///
/// The charset is asked about rather than assumed: `read` already worked it out, so
/// refusing costs one question and saves a user from a file full of mojibake.
fn readable(document: Document) -> Result<String, CliError> {
    match document.charset {
        Some(charset) if charset::is_byte_oriented(charset) => Err(CliError::Msg(legacy(charset))),
        Some(_) => Ok(document.text),
        None => Err(CliError::Msg(UNREADABLE.to_string())),
    }
}

/// What to say when the bytes are not text at all.
const UNREADABLE: &str =
    "cannot read this as text: not UTF-8, and no charset explains the bytes either";

/// What to say about a document that is text, but not Unicode.
///
/// Names the charset it looks like, because the next command needs it — and the
/// user may disagree with the guess, which `funput convert --from` lets them say.
fn legacy(charset: Charset) -> String {
    format!(
        "this looks like {} ({}), not Unicode — convert it first:\n  \
         funput convert <file> --to unicode | funput case …",
        charset.name(),
        charset.slug()
    )
}

#[cfg(test)]
mod tests;
