#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceView {
    func presentationDidChange(from oldValue: KeyboardPresentation) {
        let layoutChanged = oldValue.layout != presentation.layout
        let sizingChanged = oldValue.sizing != presentation.sizing
        let themeChanged = oldValue.theme != presentation.theme
        let edgeBlendChanged = oldValue.blendsSystemEdge != presentation.blendsSystemEdge
        if layoutChanged {
            touchOverlay.forgetTrackedTouches()
            interactionController.cancelAll()
            rebuildKeys()
        }
        if layoutChanged || sizingChanged { geometryCache = nil }
        if layoutChanged || themeChanged {
            applyPresentation()
        } else {
            if edgeBlendChanged { applyBackdropPresentation() }
            var roles = Set<KeyRole>()
            if oldValue.shiftState != presentation.shiftState {
                roles.formUnion([.character, .shift, .vniModifier])
            }
            if oldValue.language != presentation.language { roles.insert(.space) }
            if oldValue.enterAction != presentation.enterAction { roles.insert(.enter) }
            applyPresentation(to: roles)
            if oldValue.showsKeyPreviews, !presentation.showsKeyPreviews {
                updatePreview(nil, sourceFrame: nil)
            }
        }
        if layoutChanged || sizingChanged {
            invalidateIntrinsicContentSize()
            setNeedsLayout()
        }
    }

    func rebuildKeys() {
        keyControls.values.forEach { $0.removeFromSuperview() }
        let specs = presentation.layout.rows.flatMap(\.keys)
        keyControls = Dictionary(uniqueKeysWithValues: specs.map { spec in
            let control = KeyboardKeyControl(spec: spec)
            control.onEvent = { [weak self, weak control] event in
                self?.route(event, from: control)
            }
            return (spec.id, control)
        })
        keysHost.install(Array(keyControls.values))
    }

    func applyPresentation() {
        applyBackdropPresentation()
        previewView.apply(theme: presentation.theme, traits: traitCollection)
        keysHost.apply(presentation: presentation)
        toolbarView.apply(
            spec: presentation.layout.toolbar,
            theme: presentation.theme,
            traits: traitCollection
        )
        keyControls.values.forEach {
            $0.apply(presentation: presentation, traits: traitCollection)
        }
    }

    func applyBackdropPresentation() {
        backdropView.apply(
            theme: presentation.theme,
            traits: traitCollection,
            image: backgroundImage,
            blendsSystemEdge: presentation.blendsSystemEdge
        )
    }

    func applyPresentation(to roles: Set<KeyRole>) {
        guard !roles.isEmpty else { return }
        keyControls.values.lazy.filter { roles.contains($0.role) }.forEach {
            $0.apply(presentation: presentation, traits: traitCollection)
        }
    }

    func configureTouchOverlay() {
        touchOverlay.onBegin = { [weak self] token, hit, point in
            guard let self else { return }
            interactionController.beginTouch(
                token: token,
                key: hit.key,
                point: point,
                sourceFrame: hit.frame,
                containerBounds: bounds,
                presentation: presentation
            )
        }
        touchOverlay.onMove = { [weak self] token, hit, point in
            guard let self else { return }
            interactionController.moveTouch(
                token: token,
                key: hit?.key,
                point: point,
                sourceFrame: hit?.frame,
                presentation: presentation
            )
        }
        touchOverlay.onEnd = { [weak self] token in
            self?.interactionController.endTouch(token: token)
        }
        touchOverlay.onCancel = { [weak self] token in
            self?.interactionController.cancelTouch(token: token, reason: .system)
        }
        touchOverlay.onReconcile = { [weak self] active in
            self?.interactionController.reconcileActiveTouches(active)
        }
    }

    func resolvedGeometry() -> ResolvedKeyboard {
        if let cache = geometryCache,
           cache.size == bounds.size,
           cache.layout == presentation.layout,
           cache.sizing == presentation.sizing {
            return cache.value
        }
        let value = KeyboardGeometry.resolve(
            layout: presentation.layout,
            size: bounds.size,
            sizing: presentation.sizing
        )
        geometryCache = (bounds.size, presentation.layout, presentation.sizing, value)
        return value
    }
}
#endif
