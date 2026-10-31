use funput_engine::Engine;

use super::measure;

/// The collision check runs at every Telex word boundary, so it must cost nothing
/// on a word it does not restore: the gate and the list lookup work in place.
/// `cwon` passes the deferred-`w` gate and is then searched for and not found, the
/// full path short of a restore, so with the feature on it must allocate exactly
/// what it does with the feature off.
pub(super) fn assert_collision_check_is_free(engine: &mut Engine) {
    let words = 64;
    let text = "cwon tow ".repeat(words);
    let mut measured = |restore_collisions: bool| {
        engine.update_config(|c| c.restore_collisions = restore_collisions);
        engine.clear();
        measure(engine, &text)
    };
    let off = measured(false);
    let on = measured(true);
    println!("collision check: {on:?} on vs {off:?} off over {words} word pairs");
    assert_eq!(
        on, off,
        "the collision check allocated at the word boundary of a word it kept"
    );
}
