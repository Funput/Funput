# Liquid Glass on macOS

Funput supports macOS 26 and adopts refinements from macOS 27 automatically
through standard SwiftUI controls.

## Layering

- Keep brand artwork, logos, settings content, and metrics in the content layer.
- Use system `NavigationSplitView`, toolbars, menus, and controls whenever possible.
- Reserve Liquid Glass for interactive controls floating above content.
- Group nearby custom glass controls with `GlassEffectContainer`.
- Avoid nested glass effects and decorative glass surfaces.
- Use `regularMaterial` for content surfaces that need separation, not as a
  replacement for interactive glass.

## Interaction

- Use `.buttonStyle(.glass)` for secondary actions.
- Use `.buttonStyle(.glassProminent)` only for the primary action in a group.
- Add `.interactive()` only to custom controls that respond to pointer input.
- Do not add `glassEffectID` unless an effect enters or leaves the hierarchy and
  participates in a coordinated transition.

## Accessibility

- Every selection must have a non-color indicator and `.isSelected`.
- Respect Reduce Motion for explicit animations and transitions.
- Prefer semantic system colors and controls so Reduce Transparency and Increase
  Contrast are handled by the framework.
- Decorative animation must be hidden from accessibility and pause when inactive.
- Fixed-size panels must allow content to grow or scroll at larger text sizes.

## Manual QA matrix

Test Settings, onboarding, and the menu bar panel with:

1. macOS 26 and the latest macOS 27 beta.
2. Light and dark appearances.
3. Reduce Motion on and off.
4. Reduce Transparency on and off.
5. Increase Contrast on and off.
6. Full Keyboard Access and VoiceOver.
7. Minimum and enlarged window sizes.

Verify that:

- Glass controls remain legible over every background.
- Selected items are recognizable without color.
- Focus rings and keyboard activation work for every action.
- Onboarding content scrolls without clipping.
- The menu bar panel grows vertically without truncating text.
- Background animation stops when the app is inactive or Reduce Motion is on.
