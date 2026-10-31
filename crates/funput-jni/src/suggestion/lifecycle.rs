//! JNI ownership for the independent personal suggestion engine.

use std::path::Path;

use funput_suggestions::SeedLanguage;
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jint, jlong};

use super::registry;
use crate::abi::{JavaObject, neutral, safe};
use crate::engine::decode_tone_style;

/// `nativeSetSeedLanguage`: the user is typing Vietnamese. `0` means off (English,
/// or the host has not said), and so does any value this does not name.
const SEED_LANGUAGE_VIETNAMESE: jint = 1;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeCreate(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
) -> jlong {
    safe(0, registry::create)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeOpen(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    path: JString<'_>,
) -> jlong {
    safe(0, || {
        let value = neutral(env.with_env(|env| path.try_to_string(env)).into_outcome());
        (!value.is_empty())
            .then(|| registry::open(Path::new(&value)))
            .flatten()
            .unwrap_or(0)
    })
}

/// Opens the store at `path` read-only, as the app does while the keyboard owns it.
/// 0 for an empty path or a store that cannot be read.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeOpenSnapshot(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    path: JString<'_>,
) -> jlong {
    safe(0, || {
        let value = neutral(env.with_env(|env| path.try_to_string(env)).into_outcome());
        (!value.is_empty())
            .then(|| registry::open_snapshot(Path::new(&value)))
            .flatten()
            .unwrap_or(0)
    })
}

/// Attaches the English `en.lex` at `path` so queries fill the slots personal
/// words leave empty. `false` for an empty path, an unknown handle, or a file
/// that will not attach, and the engine keeps the lexicon it had. The decision is
/// `registry::attach_lexicon`, tested there; this only unwraps the Java string.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeAttachLexicon(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    path: JString<'_>,
) -> jboolean {
    safe(false, || {
        let value = neutral(env.with_env(|env| path.try_to_string(env)).into_outcome());
        !value.is_empty() && registry::attach_lexicon(handle, Path::new(&value))
    })
}

/// Says what the shipped Vietnamese data may say: `language` is 0 (off) or 1
/// (Vietnamese), anything else off; `toneStyle` is the composer's (`0` traditional,
/// `1` modern). Call it on create and on every VI/EN or tone-style switch. `false`
/// only for an unknown handle.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeSetSeedLanguage(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    language: jint,
    tone_style: jint,
) -> jboolean {
    safe(false, || {
        registry::set_seed_language(handle, decode_seed_language(language, tone_style))
    })
}

fn decode_seed_language(language: jint, tone_style: jint) -> SeedLanguage {
    match language {
        SEED_LANGUAGE_VIETNAMESE => SeedLanguage::Vietnamese(decode_tone_style(tone_style)),
        _ => SeedLanguage::Off,
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeDestroy(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) {
    safe((), || registry::destroy(handle));
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle;
    use funput_suggestions::SeedLanguage;

    use super::{SEED_LANGUAGE_VIETNAMESE, decode_seed_language};

    #[test]
    fn the_wire_ints_decode_and_anything_unknown_is_off() {
        assert_eq!(
            decode_seed_language(SEED_LANGUAGE_VIETNAMESE, 1),
            SeedLanguage::Vietnamese(ToneStyle::Modern)
        );
        assert_eq!(
            decode_seed_language(SEED_LANGUAGE_VIETNAMESE, 0),
            SeedLanguage::Vietnamese(ToneStyle::Traditional)
        );
        for language in [0, -1, 2, i32::MAX] {
            assert_eq!(
                decode_seed_language(language, 1),
                SeedLanguage::Off,
                "{language}"
            );
        }
    }
}
