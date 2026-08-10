//! Tray icon assets and hover tooltip text.

use std::cell::RefCell;

use funput_core::InputMethod;
use tray_icon::Icon;

const TRAY_PNG: &[u8] = include_bytes!("../../../icons/tray.png"); // VI: brand colour
const TRAY_MONO_PNG: &[u8] = include_bytes!("../../../icons/tray-mono.png"); // EN: high-contrast white

thread_local! {
    /// The two glyphs, decoded on first use and kept: they never change, and
    /// every VI/EN switch asks for one. Indexed by `on`. `Icon` owns a refcounted
    /// handle, so handing out a clone costs nothing. Thread-local like the tray
    /// itself — both belong to the hook thread that built them.
    static ICONS: RefCell<[Option<Icon>; 2]> = const { RefCell::new([None, None]) };
}

/// The enabled (colour) or disabled (mono) 32×32 tray glyph.
pub(super) fn make_icon(on: bool) -> Option<Icon> {
    ICONS.with(|cache| {
        let mut cache = cache.borrow_mut();
        let slot = &mut cache[usize::from(on)];
        if slot.is_none() {
            *slot = decode(on);
        }
        slot.clone()
    })
}

fn decode(on: bool) -> Option<Icon> {
    let bytes = if on { TRAY_PNG } else { TRAY_MONO_PNG };
    let img = image::load_from_memory(bytes).ok()?.into_rgba8();
    let (w, h) = img.dimensions();
    Icon::from_rgba(img.into_raw(), w, h).ok()
}

/// Hover text: brand, language mode, and method when Vietnamese is on.
pub(super) fn tooltip(enabled: bool, method: InputMethod) -> String {
    if enabled {
        format!("Funput · Tiếng Việt · {}", method_label(method))
    } else {
        "Funput · Tiếng Anh".to_string()
    }
}

fn method_label(method: InputMethod) -> &'static str {
    match method {
        InputMethod::Telex => "Telex",
        InputMethod::TelexAdvanced => "Telex nâng cao",
        InputMethod::Vni => "VNI",
        _ => "Telex",
    }
}
