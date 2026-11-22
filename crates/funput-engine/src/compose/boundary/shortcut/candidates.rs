//! Which spellings of the live word a trigger is looked up by, longest first.
//!
//! A trigger may carry symbols around the word: a prefix typed in front of it
//! (`;ct`, `2k`) and a suffix that is the boundary key itself (`ct;`). The word
//! is tried with both, with each, then bare, so a table holding `ct` and `;ct`
//! answers each as typed. A word glued to the text in front of it is only reached
//! through its prefix: bare, it is part of something larger (`abc.vn`).

use crate::compose::doubled;
use crate::model::Session;

/// Longest affixed spelling looked up, in bytes. Joined on the stack, since the
/// suffix lookup runs at every punctuation boundary and must not allocate.
const JOIN_CAP: usize = 64;

/// A trigger the live word reached.
pub(in crate::compose::boundary) struct Found {
    /// The word's own form that matched (`ct`), without the symbols around it.
    pub(in crate::compose::boundary) core: String,
    /// The expansion, re-cased to match how the word was typed.
    pub(in crate::compose::boundary) cased: String,
    /// The symbols in front of the word that belong to the trigger, deleted with it.
    pub(in crate::compose::boundary) prefix: String,
    /// The boundary key belongs to the trigger, so it is not typed after it.
    pub(in crate::compose::boundary) suffixed: bool,
}

/// The trigger the live word reaches when ended by `boundary` (`None` for a key
/// the host sends on itself, such as Enter).
pub(super) fn find(session: &Session, boundary: Option<char>) -> Option<Found> {
    let affixes = session.config.shortcut_affixes;
    let prefix = session.lead.prefix().filter(|_| affixes);
    let suffix = boundary.filter(|key| affixes && key.is_ascii_punctuation());
    let glued = session.lead.is_glued();
    let shapes = [
        (prefix, suffix),
        (prefix, None),
        (None, suffix),
        (None, None),
    ];
    shapes
        .iter()
        .enumerate()
        .filter(|&(i, shape)| !shapes[..i].contains(shape))
        .filter(|(_, (prefix, _))| prefix.is_some() || !glued)
        .find_map(|(_, &(prefix, suffix))| find_shaped(session, prefix, suffix))
}

/// The word with `prefix` and `suffix` around it, by each of its forms: typed keys,
/// then `keys`, then buffer.
///
/// A trigger is written however the user thinks of the word: as keys (`vn`, `ddc`)
/// or as the Vietnamese they compose to (`đc`, `mấy`). Keys as typed come before
/// what a revert left of them, so `pass` reaches a `pass` trigger whether or not the
/// word was decided mid-word, and still reaches `pas`.
fn find_shaped(session: &Session, prefix: Option<&str>, suffix: Option<char>) -> Option<Found> {
    let typed = doubled::typed_keys(session);
    let keys = session.keys.as_str();
    let buffer = session.buffer.as_str();
    let forms = [typed, keys, buffer];
    let mut join = [0; JOIN_CAP];
    forms
        .iter()
        .enumerate()
        .filter(|&(i, form)| !forms[..i].contains(form))
        .find_map(|(_, &form)| {
            let word = joined(&mut join, prefix, form, suffix)?;
            let cased = lookup(session, word)?;
            Some(Found {
                core: form.to_owned(),
                cased,
                prefix: prefix.unwrap_or_default().to_owned(),
                suffixed: suffix.is_some(),
            })
        })
}

/// `form` with `prefix` and `suffix` around it, written into `buf`. A bare form is
/// returned as is; `None` when the joined spelling outgrows any trigger.
fn joined<'a>(
    buf: &'a mut [u8; JOIN_CAP],
    prefix: Option<&'a str>,
    form: &'a str,
    suffix: Option<char>,
) -> Option<&'a str> {
    if prefix.is_none() && suffix.is_none() {
        return Some(form);
    }
    let mut suffix_utf8 = [0; 4];
    let suffix = suffix.map_or("", |c| c.encode_utf8(&mut suffix_utf8));
    let mut len = 0;
    for part in [prefix.unwrap_or_default(), form, suffix] {
        buf.get_mut(len..len + part.len())?
            .copy_from_slice(part.as_bytes());
        len += part.len();
    }
    std::str::from_utf8(&buf[..len]).ok()
}

/// The expansion `word` triggers, re-cased to match it.
fn lookup(session: &Session, word: &str) -> Option<String> {
    let smart = session.config.shortcut_smart_case;
    session.shortcuts.find(word, smart).map(|hit| hit.cased())
}
