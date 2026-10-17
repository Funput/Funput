//! Java arrays in and out of the correction calls.
//!
//! Reading: the two parallel arrays the IME ranks candidates with. Both are nullable
//! on the Java side, and an absent array has a meaning rather than being an error: no
//! counts means an even prior, no mask means every candidate is eligible.
//!
//! Writing: every result that is more than one number comes back as an `int[]` or a
//! `String[]`, built here so each export stays a one-line marshalling call.

use jni::EnvUnowned;
use jni::objects::{JBooleanArray, JIntArray, JObjectArray, JString};
use jni::sys::{jint, jintArray, jobjectArray};

use crate::abi::{neutral, safe};

/// A Java `int[]` as counts the engine can read, empty when the array is absent.
pub(super) fn read_ints(
    env: &mut jni::Env<'_>,
    array: &JIntArray<'_>,
) -> jni::errors::Result<Vec<u32>> {
    let len = array.len(env).unwrap_or(0);
    if len == 0 {
        return Ok(Vec::new());
    }
    let mut values = vec![0; len];
    array.get_region(env, 0, &mut values)?;
    Ok(values
        .into_iter()
        .map(|count| u32::try_from(count).unwrap_or(0))
        .collect())
}

/// A Java `boolean[]` as a permission mask, empty when the array is absent — which
/// the engine reads as "every candidate is eligible".
pub(super) fn read_bools(
    env: &mut jni::Env<'_>,
    array: &JBooleanArray<'_>,
) -> jni::errors::Result<Vec<bool>> {
    let len = array.len(env).unwrap_or(0);
    if len == 0 {
        return Ok(Vec::new());
    }
    let mut values = vec![false; len];
    array.get_region(env, 0, &mut values)?;
    Ok(values)
}

/// `values` as a new Java `int[]`, or null if the JVM could not allocate one.
pub(super) fn int_array(env: &mut EnvUnowned<'_>, values: &[jint]) -> jintArray {
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

/// `words` as a new Java `String[]`, or null if the JVM could not allocate one.
pub(super) fn string_array(env: &mut EnvUnowned<'_>, words: &[String]) -> jobjectArray {
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
