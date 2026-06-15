mod support;

use funput_core::InputMethod;
use funput_engine::Action;

#[test]
fn vni_a1_tone() {
    let (buffer, results) = support::type_keys(InputMethod::Vni, "a1");
    assert_eq!(buffer, "á");
    assert_eq!(results[1].action, Action::Send);
    assert_eq!(results[1].backspace, 1);
    assert_eq!(results[1].output, "á");
}

#[test]
fn vni_d9_stroke() {
    let (buffer, results) = support::type_keys(InputMethod::Vni, "d9");
    assert_eq!(buffer, "đ");
    assert_eq!(results[1].action, Action::Send);
    assert_eq!(results[1].backspace, 1);
    assert_eq!(results[1].output, "đ");
}

#[test]
fn vni_a11_revert() {
    assert_eq!(support::type_keys_buffer(InputMethod::Vni, "a11"), "a");
}

#[test]
fn vni_ng1_literal_ignored() {
    let (buffer, results) = support::type_keys(InputMethod::Vni, "ng1");
    assert_eq!(buffer, "ng1");
    assert_eq!(results[2].action, Action::None);
    assert_eq!(results[2].backspace, 0);
    assert!(results[2].output.is_empty());
}

#[test]
fn vni_reposition_multi_char_output() {
    // "to1a": tone lands on `o` (`tó`), then `a` moves it onto `a` (`toá`).
    // The final step deletes `ó` and injects two chars `oá`.
    let (buffer, results) = support::type_keys(InputMethod::Vni, "to1a");
    assert_eq!(buffer, "toá");
    let last = results.last().unwrap();
    assert_eq!(last.action, Action::Send);
    assert_eq!(last.backspace, 1);
    assert_eq!(last.output, "oá");
}
