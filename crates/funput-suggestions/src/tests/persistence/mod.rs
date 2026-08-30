//! Store-level tests, split by what they are about: the happy path in
//! `round_trip`, and everything to do with damaged or foreign files in `recovery`.

mod recovery;
mod replay;
mod round_trip;
