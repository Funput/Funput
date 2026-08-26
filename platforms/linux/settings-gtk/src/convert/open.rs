//! Building the Chuyển mã window, and reusing the one already up.

use std::cell::{Cell, RefCell};
use std::rc::Rc;

use adw::prelude::*;
use adw::Application;
use funput_convert::Session;
use gtk::glib;

use super::{io, ui, Convert};

thread_local! {
    /// The open window, or nothing.
    ///
    /// It has to be held somewhere: every closure the widgets own takes a `Weak`, so
    /// without this the whole thing would be freed the moment `present` returned and
    /// the window would go dead on its first click. It doubles as the lookup that
    /// makes a second launch re-present rather than open a duplicate.
    static OPEN: RefCell<Option<Rc<Convert>>> = const { RefCell::new(None) };
}

/// Show the Chuyển mã window, reusing the one already open.
///
/// Deliberately does **not** send a first-run user through onboarding first: someone
/// who opened the converter from the app grid asked for the converter, and this
/// window needs no settings to work.
pub fn present(app: &Application) {
    if let Some(convert) = OPEN.with(|open| open.borrow().clone()) {
        convert.window.present();
        return;
    }
    let convert = build(app);
    convert.window.connect_close_request(|_| {
        OPEN.with(|open| *open.borrow_mut() = None);
        glib::Propagation::Proceed
    });
    OPEN.with(|open| *open.borrow_mut() = Some(convert.clone()));
    convert.window.present();
}

/// A session told how many rows this toolkit wants at a time.
fn session() -> Session {
    let mut session = Session::new();
    session.set_row_window(0, ui::ROWS);
    session
}

fn build(app: &Application) -> Rc<Convert> {
    let panes = ui::Panes::new();
    let restart = gtk::Button::with_label("Bắt đầu lại");
    let header = adw::HeaderBar::new();
    header.pack_end(&restart);

    let content = adw::ToolbarView::builder().content(&panes.stack).build();
    content.add_top_bar(&header);

    let window = adw::ApplicationWindow::builder()
        .application(app)
        .title("Funput — Chuyển mã")
        .default_width(880)
        .default_height(600)
        .width_request(640)
        .height_request(460)
        .content(&content)
        .build();

    let convert = Rc::new(Convert {
        window,
        restart,
        session: RefCell::new(session()),
        panes,
        refreshing: Cell::new(false),
        busy: Cell::new(false),
        progress: RefCell::new(String::new()),
    });

    convert.panes.wire(&convert);
    io::accept_drops(&convert);
    let weak = Rc::downgrade(&convert);
    convert.restart.connect_clicked(move |_| {
        if let Some(convert) = weak.upgrade() {
            convert.session.borrow_mut().reset();
            convert.set_progress(String::new());
        }
    });
    convert.refresh();
    convert
}
