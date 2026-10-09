//! Key → funput-core → ImeResult orchestration.
//!
//! Hot path: the common keystroke (a pure append the app echoes itself) takes
//! the zero-allocation pass-through exit; session strings (`vn_form`, `keys`)
//! are refilled in place so their capacity is reused across keystrokes.

use funput_core::{TransformKind, apply_with};

use crate::ImeResult;
use crate::compose::RestoreOverride;
use crate::compose::diff::common_prefix_bytes;
use crate::model::Session;

/// Apply one keystroke to `session` and return platform instructions.
///
/// `session.keys` already includes `key` (pushed by the caller).
///
/// `typed` is the character the physical key produces, which differs from `key`
/// when auto-capitalize has already uppercased it. Only `typed` decides whether the
/// app can echo the key itself: a host that passes the key through on
/// [`ImeResult::none`] — the Windows hook — would otherwise let the lowercase `v`
/// reach the app while the buffer holds `V`.
pub(crate) fn process(
    session: &mut Session,
    key: char,
    typed: char,
    capitalize_shortcut: bool,
) -> ImeResult {
    let mut result = apply_with(&session.buffer, key, session.config.compose_options());
    if capitalize_shortcut && result.kind == TransformKind::Applied {
        uppercase_direct_vowel(&mut result.text);
    }

    // Buffer after composing this key (the engine appends literally on Ignored).
    let composed = match result.kind {
        TransformKind::Ignored => format!("{}{key}", session.buffer),
        _ => result.text,
    };

    // Remember the Vietnamese composition before an eager restore can collapse the
    // buffer to raw keys, so the flip hotkey can recover it after a restore.
    session.vn_form.clear();
    session.vn_form.push_str(&composed);

    // A manual flip pins which form is displayed for the rest of the word, overriding
    // the automatic restore decision below.
    let new_buffer = match session.restore_override {
        Some(RestoreOverride::ForceVietnamese) => composed,
        Some(RestoreOverride::ForceRaw) => session.keys.clone(),
        // Eager English restore: flip to the raw keystrokes the instant the word can
        // no longer be Vietnamese (`tẽt` → `text` on the closing `t`). Gated by the
        // smart + eager toggles. Skip on Reverted (a deliberate user restore) and when
        // nothing was transformed (`keys == composed`, e.g. a literal digit `ng1`).
        None => {
            if session.config.smart_restore
                && session.config.eager_restore
                && result.kind != TransformKind::Reverted
                && session.keys != composed
                && is_dead_end(session, &composed, key)
            {
                session.keys.clone()
            } else {
                composed
            }
        }
    };

    // A pure append of the typed key passes through — the app echoes it itself,
    // so there is nothing to inject (and nothing to allocate).
    let prefix = common_prefix_bytes(&session.buffer, &new_buffer);
    let pass_through =
        prefix == session.buffer.len() && new_buffer[prefix..].chars().eq(std::iter::once(typed));
    let instruction = if pass_through {
        ImeResult::none()
    } else {
        let backspace = session.buffer[prefix..].chars().count();
        ImeResult::send(backspace, new_buffer[prefix..].to_string())
    };
    session.buffer = new_buffer;

    // A revert is itself a restore to raw, so keep `keys` in sync — otherwise a
    // later word boundary sees `keys != buffer` and re-restores the stale original
    // keystrokes (e.g. `mixx` revert → `mix`, then Space → wrongly `mixx`).
    if result.kind == TransformKind::Reverted {
        session.keys.clear();
        session.keys.push_str(&session.buffer);
    }

    instruction
}

/// Whether `composed` can no longer become Vietnamese, judged for this keystroke.
///
/// VNI may keep the finals only place names use (`Pa8h` → `Păh`, as in Chư Păh),
/// but a digit landing on a word that carried no mark yet is how numbers glue to
/// English (`bar1`, `ver2`), so that keystroke is judged strictly. A letter after
/// a digit (the `h` of `Pa8h`), or a digit on a word already marked (`Pa8h1` →
/// `Pắh`), only extends a word the user shaped on purpose. What a strict reading
/// restores stays in `vn_form`, so Flip still recovers `Pah8` → `Păh`.
fn is_dead_end(session: &Session, composed: &str, key: char) -> bool {
    let bare_before = || {
        session
            .keys
            .strip_suffix(key)
            .is_some_and(|before| before == session.buffer)
    };
    let rules = session.config.syllable_rules;
    if key.is_ascii_digit() && bare_before() {
        rules.is_definitely_invalid(composed)
    } else {
        rules.is_definitely_invalid_in(composed, session.config.method)
    }
}

fn uppercase_direct_vowel(text: &mut String) {
    let Some(first) = text.chars().next() else {
        return;
    };
    let uppercase = match first {
        'ư' => 'Ư',
        'ơ' => 'Ơ',
        _ => return,
    };
    text.replace_range(..first.len_utf8(), uppercase.encode_utf8(&mut [0; 4]));
}

#[cfg(test)]
mod tests;
