//! Re-tone a word an eager English restore has just abandoned.
//!
//! A stop coda (`c`, `ch`, `p`, `t`) only ever carries sắc or nặng, so `hõc` is a
//! dead end and eager restore shows the raw keys (`hoxc`) the instant the `c`
//! lands. But the user who slipped on the tone and now presses the nặng key means
//! `học` — which UniKey gives — while the pipeline composes that key against the
//! raw keys and gets nowhere (`hoxcj`). The composition the restore abandoned is
//! still in `vn_form`, and the core re-tones it correctly when handed that form,
//! so a nặng key landing right after a restore composes from `vn_form` instead.
//!
//! Nặng only, measured: letting every tone key rescue turned thirty English words
//! into Vietnamese (`parts` → `pát`, `texts` → `tét`, `gifts` → `gít`, `sorts` →
//! `sót`), while nặng broke none. The window is one key wide by construction: any
//! other key composes on the raw buffer, which restores again and refills
//! `vn_form` from the raw keys, so there is nothing left to re-tone. The `h` of a
//! `ch` coda is the one letter allowed in between ([`shadow_coda`]).
//!
//! The rescue only fires on a word the boundary would not throw straight back —
//! a syllable, or one still awaiting its shape keys (`đuọc`, which `w` finishes
//! as `được`). `gịc` is neither, so `gixcj` stays raw rather than flipping twice.
//!
//! In VNI the nặng key is a digit, so English glued to digits that happen to read
//! as a slip and its repair composes too: `mac35` → `mạc`, `tet45` → `tẹt`. That is
//! the UniKey reading and the one chosen here; a numpad digit never reaches this
//! path. A name final only VNI's mid-word reading tolerates (`Păh`) is not
//! rescued: `Pạh` is no finished syllable, so `Pah85` stays raw and Flip still
//! recovers the name.

use funput_core::{InputMethod, Tone, TransformKind, TransformResult, apply_with, key_tone};

use crate::compose::doubled;
use crate::model::Session;

/// The word re-toned with nặng when `key` lands right after an eager restore and
/// the result is a word the boundary would keep or a later key finish; `None` when
/// the key composes against `session.buffer` as usual.
pub(super) fn retone_after_restore(session: &Session, key: char) -> Option<TransformResult> {
    if !(is_nang_key(session.config.method, key) && after_eager_restore(session, key)) {
        return None;
    }
    let result = apply_with(&session.vn_form, key, session.config.compose_options());
    let rescued = result.kind == TransformKind::Applied
        && session
            .config
            .syllable_rules
            .is_reopenable_syllable(&result.text);
    rescued.then_some(result)
}

/// The Vietnamese form to keep in `vn_form` when the `h` of a `ch` coda follows a
/// restored `c`: `sãc` + `h` → `sãch`, so the nặng key after it can still rescue
/// `sạch`. The screen keeps showing the raw keys; only the shadow moves on. `None`
/// for every other key.
pub(super) fn shadow_coda(session: &Session, key: char) -> Option<String> {
    let after_restored_c = key.eq_ignore_ascii_case(&'h')
        && session.vn_form.ends_with(['c', 'C'])
        && after_eager_restore(session, key);
    if !after_restored_c {
        return None;
    }
    Some(apply_with(&session.vn_form, key, session.config.compose_options()).text)
}

/// Whether the previous key eager-restored the word and nothing has claimed the raw
/// form since. Only eager restore is undone (the gate [`super::display::displayed`]
/// applies); a manual flip to raw and a doubled-key restore (`passw`) both look like
/// a restore but say the word is English.
fn after_eager_restore(session: &Session, key: char) -> bool {
    let config = &session.config;
    config.smart_restore
        && config.eager_restore
        && session.restore_override.is_none()
        && !doubled::has_literal(session)
        && just_restored(session, key)
}

/// Whether the word on screen is the raw keys before `key` while `vn_form` still
/// holds a composition that differs — the signal `correction::preview` reads too.
/// A revert resyncs `keys` to the buffer, and Backspace and `adopt` reseed `vn_form`
/// from it, so none of them pass as a restore.
fn just_restored(session: &Session, key: char) -> bool {
    session.keys.strip_suffix(key) == Some(session.buffer.as_str())
        && session.vn_form != session.buffer
}

fn is_nang_key(method: InputMethod, key: char) -> bool {
    key_tone(method, key) == Some(Tone::Nang)
}
