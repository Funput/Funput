//! What `funput case` takes on the command line.
//!
//! `TransformArg` lives here rather than in [`crate::cli`], where `MethodArg` is:
//! that one is up there because more than one command family takes an input method,
//! and this one belongs to a single command. Either way the clap type stays at the
//! CLI layer, so `funput-core` need not depend on clap.

use std::path::PathBuf;

use clap::{Args, ValueEnum};

use funput_core::textcase::{Options, Transform};

#[derive(Debug, Args)]
pub struct CaseArgs {
    /// Transforms to run, comma-separated and in order: `lower,title`.
    // The doc comment above is the help text, so the reason for the explicit action
    // is a plain comment: a `Vec` field derives `Append` with `num_args(1..)`, which
    // is greedy — it would swallow the file name after it, leaving `[FILE]` an
    // argument clap can never reach. `Cli::command().debug_assert()` says so out
    // loud, and `cli.rs` runs it in a test.
    #[arg(
        value_enum,
        required = true,
        value_delimiter = ',',
        num_args = 1,
        action = clap::ArgAction::Set
    )]
    pub transforms: Vec<TransformArg>,

    /// File to transform. Reads standard input when omitted.
    pub file: Option<PathBuf>,

    /// Keep đ and Đ instead of writing d and D (affects `no-diacritics`).
    #[arg(long)]
    pub keep_d: bool,

    /// Also lowercase words that are already all-caps — `TP. HCM` becomes `Tp. Hcm`
    /// (affects `title`).
    #[arg(long)]
    pub flatten_caps: bool,
}

impl CaseArgs {
    /// The switches, as core takes them.
    ///
    /// Neither switch conflicts with a transform that does not read it. Several
    /// transforms can run in one command, so `--keep-d` beside `lower,no-diacritics`
    /// is a sensible thing to type, and refusing it would be a rule the user has to
    /// learn for nothing.
    pub(super) fn options(&self) -> Options {
        Options {
            d_to_ascii: !self.keep_d,
            keep_all_caps: !self.flatten_caps,
        }
    }
}

/// The five transforms, as words on the command line. clap spells the variants in
/// kebab-case, so this is `upper`, `lower`, `no-diacritics`, `sentence`, `title`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, ValueEnum)]
pub enum TransformArg {
    /// UPPERCASE.
    Upper,
    /// lowercase.
    Lower,
    /// Bỏ dấu tiếng Việt.
    NoDiacritics,
    /// Capitalise the first letter of each sentence.
    Sentence,
    /// Capitalise The First Letter Of Each Word.
    Title,
}

impl From<TransformArg> for Transform {
    fn from(arg: TransformArg) -> Self {
        match arg {
            TransformArg::Upper => Transform::Upper,
            TransformArg::Lower => Transform::Lower,
            TransformArg::NoDiacritics => Transform::NoDiacritics,
            TransformArg::Sentence => Transform::Sentence,
            TransformArg::Title => Transform::Title,
        }
    }
}
