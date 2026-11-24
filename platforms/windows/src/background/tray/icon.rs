//! The tray glyphs, cached per size, and the hover tooltip text.

use std::cell::RefCell;

use funput_core::InputMethod;
use tray_icon::Icon;

use super::glyph;

thread_local! {
    /// Both glyphs at the size last asked for, drawn on first use. Every VI/EN
    /// switch asks for one, and they are drawn again only when the taskbar's
    /// scale changes. `Icon` owns a refcounted handle, so handing out a clone
    /// costs nothing. Thread-local like the tray itself: both belong to the hook
    /// thread that built them.
    static ICONS: RefCell<Glyphs> = const { RefCell::new(Glyphs::EMPTY) };
}

struct Glyphs {
    px: u32,
    /// Indexed by `on`: mono (EN, high-contrast white), then colour (VI).
    icons: [Option<Icon>; 2],
}

impl Glyphs {
    const EMPTY: Self = Self {
        px: 0,
        icons: [None, None],
    };
}

/// The enabled (colour) or disabled (mono) tray glyph, `px` pixels square.
pub(super) fn make_icon(on: bool, px: u32) -> Option<Icon> {
    ICONS.with(|cache| {
        let mut cache = cache.borrow_mut();
        if cache.px != px {
            *cache = Glyphs {
                px,
                ..Glyphs::EMPTY
            };
        }
        let slot = &mut cache.icons[usize::from(on)];
        if slot.is_none() {
            *slot = glyph::draw(px, !on).and_then(|rgba| Icon::from_rgba(rgba, px, px).ok());
        }
        slot.clone()
    })
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
