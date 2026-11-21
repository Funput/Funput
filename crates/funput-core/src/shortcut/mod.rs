//! Gõ tắt rules that every surface has to answer the same way.
//!
//! The engine looks triggers up with these answers, and the settings and import
//! code store triggers with them, so a table written by one and read by the other
//! never disagrees about what a trigger is. Not feature-gated, for the reason
//! [`crate::sentence`] is not: the keyboards ask too, and keeping one copy of the
//! answer is the point.

mod case;

pub use case::{TriggerCase, stored_case, typed_case};
