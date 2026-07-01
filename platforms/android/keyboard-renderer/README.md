# Keyboard renderer

Owns the latency-sensitive Android keyboard surface: responsive layout, geometry,
hit testing, touch state, accessibility nodes, and Canvas rendering.

This module must not depend on Compose, billing, networking, storage, or the IME
service. The same renderer will be hosted by the real keyboard and by previews in
the app module.
