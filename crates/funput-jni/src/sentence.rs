//! JNI exports for the shared sentence-boundary rules.
//!
//! Stateless, so unlike the composition exports these take no engine handle. The
//! keyboard calls them on every selection change to decide whether its Shift key
//! should be up, passing the text behind the caret rather than keeping its own
//! running state — a caret moves for reasons no engine sees (a paste, a tap
//! elsewhere, a field that opened with text already in it).

use funput_core::sentence::{Rules, starts_sentence, starts_word};
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::jboolean;

use crate::abi::{JavaObject, neutral, safe};

/// Whether a caret sitting after `before` starts a sentence.
///
/// Always applies [`Rules::TYPING`]: a keyboard commits its capital before the user
/// can see it, so it takes the abbreviation guard that the bulk transform declines.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeStartsSentence(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    before: JString<'_>,
) -> jboolean {
    safe(false, || {
        let text = neutral(env.with_env(|env| before.try_to_string(env)).into_outcome());
        starts_sentence(&text, Rules::TYPING)
    })
}

/// Whether a caret sitting after `before` starts a word.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeStartsWord(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    before: JString<'_>,
) -> jboolean {
    safe(false, || {
        let text = neutral(env.with_env(|env| before.try_to_string(env)).into_outcome());
        starts_word(&text)
    })
}
