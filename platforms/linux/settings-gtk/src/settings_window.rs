//! Settings chrome: a split view whose destinations live in `settings_window/`.
//! Each control still reads `settings.json` and writes through `Settings::update`.
//! Adding a page later is one `Destination` variant, one `add_named`, one stack child.

mod keyboard;
mod overview;
mod sidebar;
mod shortcuts;
mod typing;

use adw::prelude::*;
use adw::{
    Application, HeaderBar, NavigationPage, NavigationSplitView, ToastOverlay, ToolbarView,
    ViewStack, Window, WindowTitle,
};

use sidebar::Destination;

pub fn build(app: &Application) -> Window {
    let window = Window::builder()
        .title("Funput — Cài đặt")
        .default_width(780)
        .default_height(520)
        .build();
    window.set_application(Some(app));

    let stack = ViewStack::new();
    let title = WindowTitle::new(Destination::Overview.title(), "");
    let split = NavigationSplitView::new();
    split.set_min_sidebar_width(200.0);
    split.set_max_sidebar_width(240.0);

    for (dest, page) in [
        (
            Destination::Overview,
            overview::page(&stack, &split, &title),
        ),
        (Destination::Typing, typing::page().upcast()),
        (Destination::Keyboard, keyboard::page().upcast()),
        (Destination::Shortcuts, shortcuts::page()),
        (Destination::About, sidebar::about::page()),
    ] {
        stack.add_named(&page, Some(dest.id()));
    }

    let header = HeaderBar::new();
    header.set_title_widget(Some(&title));

    let content = ToolbarView::new();
    content.add_top_bar(&header);
    content.set_content(Some(&stack));

    let sidebar = sidebar::widget(&stack, &split, &window, &title);
    split.set_sidebar(Some(&NavigationPage::new(&sidebar, "Cài đặt")));
    split.set_content(Some(&NavigationPage::new(
        &content,
        Destination::Overview.title(),
    )));

    let overlay = ToastOverlay::new();
    overlay.set_child(Some(&split));
    window.set_content(Some(&overlay));
    window
}
