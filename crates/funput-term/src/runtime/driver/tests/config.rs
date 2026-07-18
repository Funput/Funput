use super::*;

#[test]
fn toggle_off_disables_composition() {
    let state = SharedState::new(true);
    let mut out = Vec::new();
    let mut toggles = Vec::new();
    forward_input(
        &[0x1c, b'a', b's'][..],
        &mut out,
        &config_for(InputMethod::Telex),
        &state,
        |status| toggles.push(status.enabled),
    )
    .unwrap();
    assert_eq!(out, b"as");
    assert_eq!(toggles, vec![false]);
}

#[test]
fn cycle_method_key_switches_telex_vni() {
    let config = TermConfig {
        cycle_method: Some(0x1e),
        ..config_for(InputMethod::Telex)
    };
    let state = SharedState::new(true);
    let mut out = Vec::new();
    let mut methods = Vec::new();
    forward_input(b"as\x1eas".as_ref(), &mut out, &config, &state, |status| {
        methods.push(status.method)
    })
    .unwrap();
    assert_eq!(reconstruct(&out), "áas");
    assert_eq!(methods, vec![InputMethod::Vni]);
}

#[test]
fn config_disabled_composes_nothing() {
    let config = TermConfig {
        enabled: false,
        ..config_for(InputMethod::Telex)
    };
    let state = SharedState::new(config.enabled);
    let mut out = Vec::new();
    forward_input(b"as".as_ref(), &mut out, &config, &state, |_| {}).unwrap();
    assert_eq!(out, b"as");
}

#[test]
fn config_shortcut_expands_at_boundary() {
    let config = TermConfig {
        shortcuts: vec![("vn".into(), "Việt Nam".into())],
        ..config_for(InputMethod::Telex)
    };
    let state = SharedState::new(true);
    let mut out = Vec::new();
    forward_input(b"vn ".as_ref(), &mut out, &config, &state, |_| {}).unwrap();
    assert_eq!(reconstruct(&out), "Việt Nam ");
}
