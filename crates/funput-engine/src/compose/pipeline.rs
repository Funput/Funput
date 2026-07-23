//! Key → funput-core → ImeResult orchestration.
//!
//! Hot path: the common keystroke (a pure append the app echoes itself) takes
//! the zero-allocation pass-through exit; session strings (`vn_form`, `keys`)
//! are refilled in place so their capacity is reused across keystrokes.

use funput_core::{TransformKind, apply_checked, is_definitely_invalid};

use crate::ImeResult;
use crate::compose::RestoreOverride;
use crate::compose::diff::common_prefix_bytes;
use crate::model::Session;

/// Apply one keystroke to `session` and return platform instructions.
///
/// `session.keys` already includes `key` (pushed by the caller).
pub(crate) fn process(session: &mut Session, key: char, capitalize_shortcut: bool) -> ImeResult {
    let mut result = apply_checked(
        &session.buffer,
        key,
        session.method,
        session.tone_style,
        session.spell_check,
    );
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
            if session.smart_restore
                && session.eager_restore
                && result.kind != TransformKind::Reverted
                && session.keys != composed
                && is_definitely_invalid(&composed)
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
        prefix == session.buffer.len() && new_buffer[prefix..].chars().eq(std::iter::once(key));
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
