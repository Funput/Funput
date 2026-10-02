//! Optional initial consonants carried over JNI without widening nativeConfigure.

use funput_core::ExtraOnsets;
use jni::EnvUnowned;
use jni::sys::{jint, jlong};

use super::update;
use crate::abi::JavaObject;

/// Apply the explicit JNI wire bits (F=1, J=2, W=4, Z=8), ignoring unknown bits.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetExtraOnsets(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    letters: jint,
) {
    set_extra_onsets(handle, letters);
}

fn set_extra_onsets(handle: jlong, letters: jint) {
    let extra_onsets = decode(letters);
    update(handle, |engine| {
        engine.update_config(|config| {
            config.syllable_rules = config.syllable_rules.with_extra_onsets(extra_onsets);
        });
    });
}

fn decode(letters: jint) -> ExtraOnsets {
    // This is a bridge contract, not a cast of the core bitset's representation.
    [
        (1, ExtraOnsets::F),
        (2, ExtraOnsets::J),
        (4, ExtraOnsets::W),
        (8, ExtraOnsets::Z),
    ]
    .into_iter()
    .filter(|(flag, _)| letters & flag != 0)
    .fold(ExtraOnsets::NONE, |selection, (_, onset)| {
        selection.union(onset)
    })
}

#[cfg(test)]
mod tests;
