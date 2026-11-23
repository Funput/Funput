//! What more than one Funput window needs: open in the middle of the screen, and
//! keep a VI/EN switch in step with the hook process while it is open.

mod follow;
mod place;

pub(crate) use follow::follow_vi_en;
pub(crate) use place::center_once_shown;
