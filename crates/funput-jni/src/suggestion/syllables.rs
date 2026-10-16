//! The shipped syllable bands, asked without a handle.
//!
//! Both read a table compiled into the library, so they take no store handle: the
//! IME asks them beside `nativeFrequency` for each candidate and adds the two
//! numbers before `nativeChooseCorrection`.

use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::jint;

use funput_suggestions::{syllable_tier, syllable_weight};

use crate::abi::{JavaObject, neutral, safe};

/// How many pseudo-uses `word`'s band in the shipped syllable list is worth, and 0
/// for a word that is not on it. Case and normalization do not matter.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeSyllableWeight(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    word: JString<'_>,
) -> jint {
    safe(0, || {
        let text = neutral(env.with_env(|env| word.try_to_string(env)).into_outcome());
        jint::try_from(syllable_weight(&text)).unwrap_or(jint::MAX)
    })
}

/// The band `word` sits in on the shipped syllable list: 1 for the most common, 4
/// for the rare but real, and 0 for a word that is not on it.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeSyllableTier(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    word: JString<'_>,
) -> jint {
    safe(0, || {
        let text = neutral(env.with_env(|env| word.try_to_string(env)).into_outcome());
        syllable_tier(&text).map_or(0, jint::from)
    })
}
