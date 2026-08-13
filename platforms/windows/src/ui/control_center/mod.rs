//! Tray Control Center flyout (short-lived Slint child process).

mod placement;
mod wire;

use std::cell::Cell;
use std::sync::atomic::{AtomicU8, Ordering};

use slint::winit_030::{EventResult, WinitWindowAccessor};
use slint::{ComponentHandle, PhysicalPosition, Weak};

use crate::shared::shell;
use crate::ui::{mica, system_accent};
use crate::{ControlCenterWindow, Theme};
use funput_config::{Method, ToneStyle};

/// Process exit codes parent reaps to open Settings on a given tab.
pub(super) const EXIT_DISMISS: u8 = 0;
pub(super) const EXIT_SETTINGS: u8 = 10;
// 11 was EXIT_SHORTCUTS, dropped when the flyout's shortcut tile became the
// smart-restore toggle. Codes stay stable so an older parent reaping a newer
// child never mistakes one destination for another.
pub(super) const EXIT_KEYBOARD: u8 = 12;

static EXIT_CODE: AtomicU8 = AtomicU8::new(EXIT_DISMISS);

/// Build, show, and wire the flyout. Returns the exit code after the event loop.
pub(super) fn open() -> u8 {
    EXIT_CODE.store(EXIT_DISMISS, Ordering::SeqCst);
    let window = ControlCenterWindow::new().expect("create control center");
    system_accent::apply(&window.global::<Theme>());
    populate(&window);
    wire::attach(&window);
    let _ = window.show();
    position_above_tray(window.window());
    schedule_backdrop(window.as_weak());
    watch_focus_loss(window.as_weak());
    let _ = slint::run_event_loop();
    EXIT_CODE.load(Ordering::SeqCst)
}

pub(super) fn request_exit(code: u8) {
    EXIT_CODE.store(code, Ordering::SeqCst);
    let _ = slint::quit_event_loop();
}

fn populate(window: &ControlCenterWindow) {
    let s = shell::snapshot();
    let on = shell::enabled();
    window.set_enabled(on);
    window.set_status(status_line(on, s.method).into());
    window.set_method_label(method_label(s.method).into());
    window.set_spell_check(s.spell_check);
    window.set_auto_capitalize(s.auto_capitalize);
    window.set_smart_restore(s.smart_restore);
    window.set_hotkey_label(hotkey_label(&s).into());
    window.set_tone_label(tone_label(s.tone_style).into());
}

fn schedule_backdrop(weak: Weak<ControlCenterWindow>) {
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(window) = weak.upgrade() {
            let active = mica::apply_flyout(window.window());
            window.global::<Theme>().set_mica(active);
        }
    });
}

fn watch_focus_loss(weak: Weak<ControlCenterWindow>) {
    // Arm after the first event-loop turn so show() does not dismiss immediately.
    let armed = Cell::new(false);
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(window) = weak.upgrade() {
            armed.set(true);
            window.window().on_winit_window_event(move |_, event| {
                use slint::winit_030::winit::event::WindowEvent;
                if let WindowEvent::Focused(false) = event {
                    if armed.get() {
                        request_exit(EXIT_DISMISS);
                    }
                }
                EventResult::Propagate
            });
        }
    });
}

fn position_above_tray(window: &slint::Window) {
    let tray = placement::Rect::from_size(
        env_f64("FUNPUT_TRAY_X"),
        env_f64("FUNPUT_TRAY_Y"),
        env_f64("FUNPUT_TRAY_W").max(1.0),
        env_f64("FUNPUT_TRAY_H"),
    );
    let size = window.size();
    let (x, y) = placement::anchor(
        tray,
        placement::work_area(tray),
        f64::from(size.width),
        f64::from(size.height),
    );
    window.set_position(PhysicalPosition::new(x, y));
}

fn env_f64(key: &str) -> f64 {
    std::env::var(key)
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(0.0)
}

pub(super) fn status_line(on: bool, method: Method) -> String {
    if on {
        format!("Đang gõ tiếng Việt · {}", method_label(method))
    } else {
        "Đang gõ tiếng Anh".into()
    }
}

pub(super) fn method_label(method: Method) -> &'static str {
    match method {
        Method::Telex => "Telex",
        Method::TelexAdvanced => "Telex nâng cao",
        Method::Vni => "VNI",
    }
}

pub(super) fn tone_label(tone: ToneStyle) -> &'static str {
    match tone {
        ToneStyle::Traditional => "Truyền thống",
        ToneStyle::Modern => "Hiện đại",
    }
}

pub(super) fn cycle_method(current: Method) -> Method {
    match current {
        Method::Telex => Method::TelexAdvanced,
        Method::TelexAdvanced => Method::Vni,
        Method::Vni => Method::Telex,
    }
}

fn hotkey_label(s: &funput_config::Settings) -> String {
    if let Some(combo) = &s.toggle_combo {
        combo.caps().join("+")
    } else {
        s.toggle_hotkey.caps().join("+")
    }
}
