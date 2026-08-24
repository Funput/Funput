//! `funput convert`: the charset-conversion tool (công cụ chuyển mã).
//!
//! Vietnamese government documents still circulate in TCVN3, drawn by the `.VnTime`
//! fonts, and in VNI-Windows. This turns them into Unicode and back. It converts
//! text that already exists; typing in a legacy charset is not offered and is not
//! planned — see `docs/features/charset.md`.
//!
//! - [`source`] — turning an argument or standard input into text, and working out
//!   what charset spelled it.
//! - [`sink`] — writing the result back out, as bytes when the target is a legacy
//!   charset and as UTF-8 when it is not.
//! - [`report`] — everything printed to standard error: the charset table, the
//!   detected name, and the warning when something could not be represented.
//!
//! Nothing here names a charset. `--to tcvn3` is matched against
//! `funput_core::charset::ALL` by slug, so implementing VISCII in core is one PR
//! there and this command picks it up.

mod report;
mod sink;
mod source;

use std::path::PathBuf;
use std::process::ExitCode;

use clap::Args;

use funput_core::charset::{self, Charset};

use crate::cli::{CliError, CliResult};

#[derive(Debug, Args)]
pub struct ConvertArgs {
    /// File to convert. Reads standard input when omitted.
    pub file: Option<PathBuf>,

    /// Charset to convert to, by slug (`funput convert --list`).
    #[arg(short, long, value_name = "CHARSET", required_unless_present_any = ["detect", "list"])]
    pub to: Option<String>,

    /// Charset the input is in. Guessed when omitted.
    #[arg(short, long, value_name = "CHARSET")]
    pub from: Option<String>,

    /// Print the charset the input looks like and stop.
    #[arg(long, conflicts_with = "to")]
    pub detect: bool,

    /// List the charsets and stop.
    #[arg(long, exclusive = true)]
    pub list: bool,
}

/// Run `funput convert`.
pub fn run(args: ConvertArgs) -> CliResult {
    if args.list {
        report::list();
        return Ok(ExitCode::SUCCESS);
    }

    // Both slugs are resolved before anything is read, so a typo in `--to` is
    // answered straight away rather than after standard input has been drained.
    let to = args.to.as_deref().map(by_slug).transpose()?;
    let declared = args.from.as_deref().map(by_slug).transpose()?;

    let input = source::read(args.file.as_deref())?;
    // A charset the user named beats one that was worked out: they are looking at
    // the document, and the detector is looking at statistics.
    let Some(from) = declared.or(input.charset) else {
        return Err(CliError::Msg(report::UNDETECTED.to_string()));
    };

    // No `--to` means `--detect`: clap makes each one required unless the other is
    // present, so there is no third case to handle.
    let Some(to) = to else {
        report::detected(from);
        return Ok(ExitCode::SUCCESS);
    };

    // Through the pivot, which is what makes this one command rather than one per
    // pair: core reads `from` into Unicode, and writes Unicode back out as `to`.
    let read = charset::convert(&input.text, from, Charset::Unicode);
    let (bytes, written) = charset::encode_bytes(&read.text, to);
    sink::write(&bytes)?;
    report::losses(&read, &written);
    Ok(ExitCode::SUCCESS)
}

/// The charset a slug names. The error lists what would have worked, since a wrong
/// slug is nearly always a typo or a guess at the spelling.
fn by_slug(slug: &str) -> Result<Charset, CliError> {
    charset::ALL
        .iter()
        .copied()
        .find(|c| c.slug() == slug)
        .ok_or_else(|| CliError::Msg(report::unknown_slug(slug)))
}

#[cfg(test)]
mod tests;
