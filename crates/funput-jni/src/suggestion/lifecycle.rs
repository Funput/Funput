//! JNI ownership for the independent personal suggestion engine.

use std::path::Path;

use funput_suggestions::SeedLanguage;
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jint, jlong};

use super::registry;
use crate::abi::{JavaObject, neutral, safe};
use crate::engine::decode_tone_style;

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

/// Says what the shipped Vietnamese data may say. `language` is one of
/// `SeedLanguage`'s wire codes (`0` off, `1` Vietnamese; anything else, including
/// a value outside a byte, is off); `toneStyle` is the composer's (`0` traditional,
/// `1` modern).
///
/// The handle keeps its own copy of both, apart from the composition engine, so
/// call this on create and whenever the active input language or the tone style
/// changes, whatever changed it, from the same place that configures the composer.
/// `false` for an unknown handle, or a call the boundary had to abandon.
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

/// A Java `int` narrowed to the byte the codes live in; one that does not fit is no
/// code at all, so off.
fn decode_seed_language(language: jint, tone_style: jint) -> SeedLanguage {
    u8::try_from(language).map_or(SeedLanguage::Off, |code| {
        SeedLanguage::from_wire(code, decode_tone_style(tone_style))
    })
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

    use super::decode_seed_language;

    /// The codes themselves are `SeedLanguage::from_wire`'s, tested there; what is
    /// this binding's own is narrowing an `int` that may not fit a byte.
    #[test]
    fn an_int_outside_a_byte_is_off_not_truncated() {
        let vietnamese = i32::from(SeedLanguage::WIRE_VIETNAMESE);
        assert_eq!(
            decode_seed_language(vietnamese, 1),
            SeedLanguage::Vietnamese(ToneStyle::Modern)
        );
        for language in [-1, 256 + vietnamese, i32::MAX] {
            assert_eq!(
                decode_seed_language(language, 1),
                SeedLanguage::Off,
                "{language}"
            );
        }
    }
}
