//! Reading the two parallel arrays the IME ranks candidates with.
//!
//! Both are nullable on the Java side, and an absent array has a meaning rather than
//! being an error: no counts means an even prior, no mask means every candidate is
//! eligible.

use jni::objects::{JBooleanArray, JIntArray};

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
