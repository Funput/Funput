//! Java strings and arrays across the boundary, for every export that needs one.
//!
//! Each writer runs under the [`safe`] guard and answers null when the JVM cannot
//! allocate, which the Kotlin side declares as a nullable array.

use jni::EnvUnowned;
use jni::objects::{JIntArray, JLongArray, JObjectArray, JString};
use jni::sys::{jint, jintArray, jlongArray, jobjectArray};

use super::{neutral, safe};

/// A Java string as Rust text; empty when it is null or unreadable.
pub(crate) fn read_string(env: &mut EnvUnowned<'_>, value: &JString<'_>) -> String {
    neutral(env.with_env(|env| value.try_to_string(env)).into_outcome())
}

/// `words` as a new Java `String[]`.
pub(crate) fn string_array(env: &mut EnvUnowned<'_>, words: &[String]) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let initial = JString::from_str(env, words.first().map_or("", String::as_str))?;
                let array = JObjectArray::<JString>::new(env, words.len(), &initial)?;
                for (index, word) in words.iter().enumerate().skip(1) {
                    let value = JString::from_str(env, word)?;
                    array.set_element(env, index, &value)?;
                }
                Ok(array.into_raw())
            })
            .into_outcome();
        neutral(result)
    })
}

/// `values` as a new Java `int[]`.
pub(crate) fn int_array(env: &mut EnvUnowned<'_>, values: &[jint]) -> jintArray {
    safe(std::ptr::null_mut(), || {
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let array = JIntArray::new(env, values.len())?;
                array.set_region(env, 0, values)?;
                Ok(array.into_raw())
            })
            .into_outcome();
        neutral(result)
    })
}

/// `values` as a new Java `long[]`.
pub(crate) fn long_array(env: &mut EnvUnowned<'_>, values: &[i64]) -> jlongArray {
    safe(std::ptr::null_mut(), || {
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let array = JLongArray::new(env, values.len())?;
                array.set_region(env, 0, values)?;
                Ok(array.into_raw())
            })
            .into_outcome();
        neutral(result)
    })
}
