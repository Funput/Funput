//! Key → funput-core → ImeResult orchestration.
//!
//! Hot path: the common keystroke (a pure append the app echoes itself) takes
//! the zero-allocation pass-through exit; session strings (`vn_form`, `keys`)
//! are refilled in place so their capacity is reused across keystrokes.

mod display;

use funput_core::{TransformKind, apply_with};

use crate::ImeResult;
use crate::compose::diff::common_prefix_bytes;
use crate::compose::doubled;
use crate::model::Session;
use display::{Shown, displayed};

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

    // Keep the keys a revert swallows while `keys` still holds them, and before the
    // display is chosen, so the reverting key itself can be the decisive one.
    if result.kind == TransformKind::Reverted {
        doubled::on_revert(session);
    }
    let shown = displayed(session, composed, result.kind, key);
    let literal_shown = matches!(shown, Shown::Literal);
    let new_buffer = match shown {
        Shown::Composed(text) => text,
        Shown::Keys => session.keys.clone(),
        Shown::Literal => session.literal.as_str().to_owned(),
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
    // keystrokes (e.g. `mixx` revert → `mix`, then Space → wrongly `mixx`). The
    // swallowed key is not lost: `on_revert` above kept it in `literal`.
    if result.kind == TransformKind::Reverted {
        session.keys.clear();
        session.keys.push_str(&session.buffer);
    }
    if literal_shown {
        doubled::settle_keys(session);
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
