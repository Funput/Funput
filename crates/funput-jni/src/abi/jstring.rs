//! Java string / object marshalling across the JNI boundary.
//!
//! [`string_result`] runs a Rust closure under the [`safe`] panic guard and hands
//! its `String` back to Java as a `JString`; [`JavaObject`] is the `this` receiver
//! alias every export takes.

use jni::EnvUnowned;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JObject, JString};

use super::safe;

pub(crate) type JavaObject<'caller> = JObject<'caller>;

pub(crate) fn string_result<'caller>(
    mut env: EnvUnowned<'caller>,
    operation: impl FnOnce() -> String,
) -> JString<'caller> {
    let value = safe(String::new(), operation);
    env.with_env(|env| JString::from_str(env, value))
        .resolve::<ThrowRuntimeExAndDefault>()
}
