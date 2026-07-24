//! Telex modifier-key classifiers: each maps a letter to the diacritic it triggers
//! — circumflex (`aa` → `â`), stroke (`dd` → `đ`), tone (`as` → `á`), and Telex `w`.

pub(crate) mod circumflex;
pub(crate) mod stroke;
pub(crate) mod tone;
pub(crate) mod w;
