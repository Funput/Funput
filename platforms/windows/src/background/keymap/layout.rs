//! Which keyboard layout the user is typing on right now — asked of the thread
//! that receives their keys, which is not always the one that owns the window.

use windows::Win32::UI::Input::KeyboardAndMouse::GetKeyboardLayout;
use windows::Win32::UI::WindowsAndMessaging::{
    GUITHREADINFO, GetForegroundWindow, GetGUIThreadInfo, GetWindowThreadProcessId,
};

/// The keyboard layout handle of whatever has focus, as a plain `u32` for
/// [`funput_desktop::is_foreign_layout`]. Zero when there is no foreground window.
///
/// The input language is a property of a *thread*, which is what makes this the
/// right question to ask however the user has Windows set up: one language for
/// everything, or one per app window. `GetKeyboardLayout(0)` in the parent module
/// answers for Funput's own hook thread instead, which is a different question and
/// never changes.
///
/// Which thread matters: the one that owns keyboard focus, not the one that owns
/// the top-level window. Windows 11 Notepad — like any WinUI or XAML Islands app —
/// runs its text box on a second thread, and switching to Pinyin changes only that
/// one: the window's own thread kept answering US English, so Funput went on
/// typing Vietnamese into a Chinese IME (#599), and the other way round left it
/// suspended on an English keyboard.
///
/// A handful of cheap user32 calls that read session state — no cross-process
/// work, no blocking — which is what makes this safe to do from inside the hook.
/// Windows has no global notification for an input-language change
/// (`WM_INPUTLANGCHANGE` goes only to the app that owns the change, and the TSF
/// sinks are per-thread), so asking on each keystroke is the only way to know; and
/// asking *there* is also what guarantees the first key typed after Win+Space is
/// already judged right.
pub fn foreground_layout() -> u32 {
    // SAFETY: Takes no arguments; a null handle is a valid answer.
    let hwnd = unsafe { GetForegroundWindow() };
    if hwnd.0.is_null() {
        return 0;
    }
    // SAFETY: A window that has just gone away yields thread 0, which reads as none.
    let top = unsafe { GetWindowThreadProcessId(hwnd, None) };
    let tid = pick_thread(top, focus_thread(top));
    // SAFETY: Any thread id is accepted; an unknown one answers with a null handle.
    unsafe { GetKeyboardLayout(tid) }.0 as usize as u32
}

/// The thread whose window has keyboard focus in `top`'s GUI, if it reports one.
/// Console windows and windows with nothing focused do not.
fn focus_thread(top: u32) -> Option<u32> {
    if top == 0 {
        return None;
    }
    let mut info = GUITHREADINFO {
        cbSize: size_of::<GUITHREADINFO>() as u32,
        ..Default::default()
    };
    // SAFETY: `info` is writable and its size is filled in, as the API requires.
    unsafe { GetGUIThreadInfo(top, &mut info) }.ok()?;
    if info.hwndFocus.0.is_null() {
        return None;
    }
    // SAFETY: As above — a stale focus handle yields thread 0.
    Some(unsafe { GetWindowThreadProcessId(info.hwndFocus, None) })
}

/// The thread to ask: the focus thread when there is one, the window's otherwise.
/// Thread id 0 is no thread, so it never wins.
fn pick_thread(top: u32, focus: Option<u32>) -> u32 {
    focus.filter(|&tid| tid != 0).unwrap_or(top)
}

#[cfg(test)]
mod tests {
    use super::pick_thread;

    #[test]
    fn the_thread_with_keyboard_focus_wins() {
        // #599: Notepad's window thread said US English while its text box,
        // on another thread, had already switched to Pinyin.
        assert_eq!(pick_thread(1200, Some(14796)), 14796);
    }

    #[test]
    fn a_single_threaded_app_answers_for_itself() {
        assert_eq!(pick_thread(1200, Some(1200)), 1200);
    }

    #[test]
    fn no_focus_falls_back_to_the_window_thread() {
        assert_eq!(pick_thread(1200, None), 1200);
        assert_eq!(pick_thread(1200, Some(0)), 1200); // the focus window just went away
    }
}
