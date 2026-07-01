# Theme runtime

Owns the versioned theme contract, token resolution, validation, capability
fallbacks, and safe access to installed theme assets.

This module contains no store, billing, network, or keyboard input behavior.
Themes can change visuals only; they cannot change key geometry or actions.
