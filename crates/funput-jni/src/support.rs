//! Shared panic and Java-string handling for JNI exports.

use std::panic::{AssertUnwindSafe, catch_unwind};

use jni::EnvUnowned;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JObject, JString};

pub(crate) type JavaObject<'caller> = JObject<'caller>;

pub(crate) fn safe<T>(default: T, operation: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(operation)).unwrap_or(default)
}

pub(crate) fn string_result<'caller>(
    mut env: EnvUnowned<'caller>,
    operation: impl FnOnce() -> String,
) -> JString<'caller> {
    let value = safe(String::new(), operation);
    env.with_env(|env| JString::from_str(env, value))
        .resolve::<ThrowRuntimeExAndDefault>()
}
