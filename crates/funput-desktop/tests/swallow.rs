//! A key the engine swallows without changing the text, driven the way a hook +
//! inject shell drives it: the plan is empty, and the key must still not reach the
//! app. The case that needs it is the mũ key confirming a circumflex the composer
//! added (`viecj` → `việc`, then `e`), which a hook that read an empty plan as
//! "pass the key through" would turn into `việce`.

mod support;

use funput_core::InputMethod;
use support::Shell;

#[test]
fn the_confirming_mu_key_never_reaches_the_app() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("viecje ");
    assert_eq!(shell.app, "việc ");
}

#[test]
fn the_same_in_vni() {
    let mut shell = Shell::new(InputMethod::Vni);
    shell.type_str("viec56 ");
    assert_eq!(shell.app, "việc ");
}

/// After the confirmation the next mũ key reverts as it always did, and the shell
/// types the reverted text.
#[test]
fn a_second_mu_key_still_reverts() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("viecjee ");
    assert_eq!(shell.app, "viẹce ");
}

/// `None` from `plan_inject` is still the pass-through: a plain letter is typed by
/// the app itself.
#[test]
fn a_plain_key_still_passes_through() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("viec");
    assert_eq!(shell.app, "viec");
    assert_eq!(shell.engine.buffer(), "viec");
}

/// The contract change also reaches empty `Send`s the engine already produced,
/// which the old hook let through as a literal key and so put a character on
/// screen the engine did not know about. Those desyncs are gone.
#[test]
fn an_empty_send_the_engine_already_produced_is_swallowed_too() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("cuwuu ");
    assert_eq!(shell.app, "cưu ", "was `cưuu` on the old hook");

    let mut shell = Shell::new(InputMethod::Vni);
    shell.type_str("uu7u ");
    assert_eq!(shell.app, "ưu ", "was `ưuu` on the old hook");

    let mut shell = Shell::new(InputMethod::TelexAdvanced);
    shell
        .engine
        .update_config(|config| config.eager_restore = false);
    shell.type_str("Newsweek ");
    assert_eq!(shell.app, "Newsweek ", "was `NNewsweek` on the old hook");

    // Telex nâng cao's `ww` is a swallowed key by design on every other platform;
    // Windows now agrees (`slowworm` was the one word the old hook got right).
    let mut shell = Shell::new(InputMethod::TelexAdvanced);
    shell.type_str("slowworm ");
    assert_eq!(shell.app, "sloworm ");
}
