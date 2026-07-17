//! Input-method mechanics: Telex and VNI keystroke sequences.

mod support;

#[path = "methods/telex.rs"]
mod telex;
#[path = "methods/telex_deferred_w.rs"]
mod telex_deferred_w;
#[path = "methods/telex_multi_intent_spec.rs"]
mod telex_multi_intent_spec;
#[path = "methods/vni.rs"]
mod vni;
