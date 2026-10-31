//! The personal store on its own: learning and promotion in `learning`, how
//! often a word was typed in `frequency`, making room in `eviction`, and a long
//! run under load in `stress`.

mod eviction;
mod frequency;
mod learning;
mod stress;
