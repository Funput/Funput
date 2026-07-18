use funput_ffi::{
    ACTION_NONE, FunputResult, METHOD_TELEX, METHOD_TELEX_ADVANCED, METHOD_VNI, funput_engine_free,
    funput_engine_new, funput_flip_composing, funput_process_char, funput_set_method,
};

unsafe fn type_on(engine: *mut funput_ffi::FunputEngine, keys: &str) -> String {
    let mut output = String::new();
    for key in keys.chars() {
        let result = unsafe { funput_process_char(engine, key as u32) };
        if result.action == ACTION_NONE {
            output.push(key);
            continue;
        }
        for _ in 0..result.backspace {
            output.pop();
        }
        output.extend(
            result.chars[..result.count as usize]
                .iter()
                .filter_map(|value| char::from_u32(*value)),
        );
    }
    output
}

fn typed(method: u8, keys: &str) -> String {
    unsafe {
        let engine = funput_engine_new();
        funput_set_method(engine, method);
        let output = type_on(engine, keys);
        funput_engine_free(engine);
        output
    }
}

fn result_text(result: FunputResult) -> String {
    result.chars[..result.count as usize]
        .iter()
        .filter_map(|value| char::from_u32(*value))
        .collect()
}

fn apply_result(displayed: &mut String, result: FunputResult) {
    for _ in 0..result.backspace {
        displayed.pop();
    }
    displayed.push_str(&result_text(result));
}

#[test]
fn stable_method_ids_select_the_expected_grammar() {
    assert_eq!(typed(METHOD_TELEX, "w "), "w ");
    assert_eq!(typed(METHOD_VNI, "a1 "), "á ");
    assert_eq!(typed(METHOD_TELEX_ADVANCED, "tr[]ngf "), "trường ");
}

#[test]
fn unknown_method_falls_back_to_standard_telex() {
    assert_eq!(typed(u8::MAX, "w t[ "), "w t[ ");
}

#[test]
fn ffi_layout_and_method_ids_are_stable() {
    assert_eq!((METHOD_TELEX, METHOD_VNI, METHOD_TELEX_ADVANCED), (0, 1, 2));
    assert_eq!(std::mem::size_of::<FunputResult>(), 268);
    assert_eq!(std::mem::align_of::<FunputResult>(), 4);
}

#[test]
fn persisted_advanced_id_survives_native_relaunch() {
    let persisted_method = METHOD_TELEX_ADVANCED;
    unsafe {
        let first = funput_engine_new();
        funput_set_method(first, persisted_method);
        assert_eq!(type_on(first, "t["), "tư");
        funput_engine_free(first);

        let relaunched = funput_engine_new();
        funput_set_method(relaunched, persisted_method);
        assert_eq!(type_on(relaunched, "tr[]ngf "), "trường ");
        funput_engine_free(relaunched);
    }
}

#[test]
fn advanced_raw_keys_flip_both_ways_through_c_abi() {
    unsafe {
        let engine = funput_engine_new();
        funput_set_method(engine, METHOD_TELEX_ADVANCED);
        let mut displayed = type_on(engine, "tr[]ngf");
        assert_eq!(displayed, "trường");
        apply_result(&mut displayed, funput_flip_composing(engine));
        assert_eq!(displayed, "tr[]ngf");
        apply_result(&mut displayed, funput_flip_composing(engine));
        assert_eq!(displayed, "trường");
        funput_engine_free(engine);
    }
}
