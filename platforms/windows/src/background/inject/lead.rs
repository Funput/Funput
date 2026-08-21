//! Which foreground app needs [`super::send_plan`]'s lead character.
//!
//! The lead is what makes a plan's Backspaces unambiguous when the app has put a
//! selection in front of the caret without being asked — see [`super::send_plan`]
//! for the mechanism. It is not free: it costs one extra Backspace on every
//! replacement, and an app whose text engine drops a key arriving in the same
//! `SendInput` burst as the glyph before it loses precisely that Backspace and lands
//! one character short.
//!
//! CorelDRAW is such an app. Its text tool draws on the canvas rather than through
//! an edit control; measured with `duong` + `w`, whose plan is 4 Backspaces and
//! "ương", the lead makes it 5, four land, and "dương" comes out "duương". The count
//! is off by exactly one whether the plan asks for 1 Backspace or 5, which is what
//! identifies the lead's own Backspace as the one being lost. Illustrator, Photoshop
//! and every other canvas text engine are the same shape of program.
//!
//! # Asking the engine, not the app
//!
//! Inline autofill — a selected suffix completed as the user types — is a *browser
//! engine* behaviour, not something each browser reinvents. So the question this
//! module answers is which engine drew the window, which it reads off the top-level
//! window class. Two strings cover every Chromium and every Gecko browser there is
//! or will be: no fork to chase, no exe name to keep current, nothing for a user to
//! configure. A list of browser executables would have been wrong the moment someone
//! opened Thorium, and wrong in a way nobody could see.
//!
//! Autofill is not *only* a browser habit, though — Excel completes a cell from the
//! column above the same way — and those apps share no engine to ask. They come in
//! by exe name through [`AUTOFILL_APPS`], one measurement at a time.
//!
//! # Why being wrong here is cheap
//!
//! An earlier fix listed browsers to give them a `Delete` primer, and being on that
//! list was *destructive*: `Delete` ate the character to the right of the caret in
//! every page field of those browsers. This test is the opposite. Matching wrongly
//! costs nothing — a lead is typed and paid back inside the same batch — which is
//! what makes it safe that `Chrome_WidgetWin_1` also catches every Electron app.
//! Those render text through Blink and pay the extra Backspace without noticing.
//! Missing a match costs one visibly wrong syllable in an address bar, while inline
//! autofill is actually showing. Neither direction can lose text.

use std::sync::atomic::{AtomicBool, Ordering};

/// Top-level window class of every Chromium-based browser, and of Electron and CEF
/// apps along with them. The trailing number is a widget-type counter rather than a
/// version, so this matches as a prefix.
const CHROMIUM_CLASS_PREFIX: &str = "Chrome_WidgetWin_";

/// Top-level window class of every Gecko-based browser — Firefox and its forks,
/// Tor Browser included, all of which ship as `firefox.exe` anyway.
const GECKO_CLASS: &str = "MozillaWindowClass";

/// Apps that inline-autofill without being a browser, by lowercased exe file name —
/// the same app id the per-app VI/EN memory uses.
///
/// Excel is here because it was measured, not assumed: its AutoComplete offers a
/// value from the column above with the unmatched suffix selected, exactly the
/// omnibox shape. Given "mai anh" in the cell above, typing `mai` shows "mai[ anh]"
/// and the plan for `f` (2 Backspaces, "ài") came out "maài" — the first Backspace
/// spent on the selection, "i" surviving, "ài" landing on top.
///
/// Nothing else is listed, and that is a claim rather than an oversight. Outlook's
/// recipient fields and Explorer's address bar behave the same way to the eye but
/// have not been put through the same test, and a Vietnamese tone rarely lands while
/// one of their completions is live: the typed prefix stops matching the moment a
/// diacritic appears, which is what makes this so much rarer than it looks. Each
/// needs its own measurement before it earns a line, since guessing would put the
/// lead's extra Backspace back on apps that never needed it.
const AUTOFILL_APPS: &[&str] = &["excel.exe"];

/// Whether the focused app is one of them. Written by the foreground hook, read by
/// [`super::send_plan`] inside `WH_KEYBOARD_LL` — an atomic rather than a trip
/// through the shell mutex, since that is the hot path.
static FOREGROUND_INLINE_AUTOFILLS: AtomicBool = AtomicBool::new(false);

/// The judgement itself, over what the foreground hook could see: the window's class
/// and the owning process's exe name. Anything unrecognized gets no lead, which is
/// also the state Funput starts in — before the first foreground event there is
/// nothing to be wrong about.
fn inline_autofills(window_class: &str, app_id: &str) -> bool {
    window_class.starts_with(CHROMIUM_CLASS_PREFIX)
        || window_class == GECKO_CLASS
        || AUTOFILL_APPS.contains(&app_id)
}

/// Record the app that just took focus.
pub fn note_foreground(window_class: &str, app_id: &str) {
    let autofills = inline_autofills(window_class, app_id);
    FOREGROUND_INLINE_AUTOFILLS.store(autofills, Ordering::Relaxed);
}

/// Whether a replacement should lead with its first character.
pub(super) fn wanted() -> bool {
    FOREGROUND_INLINE_AUTOFILLS.load(Ordering::Relaxed)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_chromium_and_gecko_browser_leads() {
        // Measured on this machine: Chrome reports `Chrome_WidgetWin_1`. Every
        // Chromium fork inherits it, which is the whole point of matching here
        // instead of on an exe name that changes with each new browser.
        assert!(inline_autofills("Chrome_WidgetWin_1", "chrome.exe"));
        assert!(inline_autofills("Chrome_WidgetWin_1", "thorium.exe"));
        assert!(inline_autofills(
            "Chrome_WidgetWin_0",
            "some-old-chromium.exe"
        ));
        // Measured too: Firefox reports `MozillaWindowClass`, which its forks and
        // Tor Browser inherit unchanged.
        assert!(inline_autofills("MozillaWindowClass", "firefox.exe"));
        assert!(inline_autofills("MozillaWindowClass", "librewolf.exe"));
    }

    #[test]
    fn excel_leads_on_its_exe_name_because_no_engine_class_claims_it() {
        // Measured: Excel is `XLMAIN`, which neither engine prefix matches, so it
        // reaches the lead only through `AUTOFILL_APPS`. Both halves are asserted —
        // if someone widens the class test to cover Excel, the exe entry becomes
        // dead weight and should go with it.
        assert!(inline_autofills("XLMAIN", "excel.exe"));
        assert!(!"XLMAIN".starts_with(CHROMIUM_CLASS_PREFIX));
        assert_ne!("XLMAIN", GECKO_CLASS);
    }

    #[test]
    fn a_canvas_text_engine_does_not() {
        // The regression this module exists for. Class measured on CorelDRAW 2026;
        // the version suffix means the string itself must never be matched against.
        assert!(!inline_autofills("CorelDRAW27", "coreldrw.exe"));
        assert!(!inline_autofills("Notepad", "notepad.exe"));
        assert!(!inline_autofills("SunAwtFrame", "idea64.exe"));
        // A window whose class or process could not be read arrives as empty.
        assert!(!inline_autofills("", ""));
    }

    #[test]
    fn an_electron_app_is_caught_and_that_is_fine() {
        // Claude and Cursor both report `Chrome_WidgetWin_1`, so they take the lead
        // they have no address bar to need. Blink pays the extra Backspace, so this
        // is the harmless direction — asserted so a future reader sees it is known
        // and intended rather than a hole.
        assert!(inline_autofills("Chrome_WidgetWin_1", "cursor.exe"));
    }

    #[test]
    fn the_escape_hatch_is_spelled_the_way_the_hook_spells_apps() {
        // `exe_of_window` lowercases, so an entry that is not lowercase is dead.
        for id in AUTOFILL_APPS {
            assert_eq!(*id, id.to_lowercase(), "{id} would never match");
            assert!(id.ends_with(".exe"), "{id} is not an exe file name");
        }
    }

    /// The only test that touches the process-global flag, so nothing it does can
    /// race another test reading it.
    #[test]
    fn the_flag_follows_the_foreground_app_in_both_directions() {
        note_foreground("Chrome_WidgetWin_1", "chrome.exe");
        assert!(wanted());
        note_foreground("CorelDRAW27", "coreldrw.exe");
        assert!(!wanted());
    }
}
