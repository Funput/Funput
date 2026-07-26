package app.funput.funput.keyboard.ui

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

internal object KeyboardComposeLifecycle {
    fun install(root: View) {
        val owner = Owner()
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.addOnAttachStateChangeListener(owner)
    }

    private class Owner : LifecycleOwner, SavedStateRegistryOwner, View.OnAttachStateChangeListener {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val stateController = SavedStateRegistryController.create(this)
        private var lifecycleRoot: View? = null
        private var savedStateRoot: View? = null
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = stateController.savedStateRegistry

        init {
            stateController.performAttach()
            stateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        override fun onViewAttachedToWindow(view: View) {
            val windowRoot = view.rootView
            if (windowRoot.findViewTreeLifecycleOwner() == null) {
                windowRoot.setViewTreeLifecycleOwner(this)
                lifecycleRoot = windowRoot
            }
            if (windowRoot.findViewTreeSavedStateRegistryOwner() == null) {
                windowRoot.setViewTreeSavedStateRegistryOwner(this)
                savedStateRoot = windowRoot
            }
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override fun onViewDetachedFromWindow(view: View) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            lifecycleRoot?.setViewTreeLifecycleOwner(null)
            savedStateRoot?.setViewTreeSavedStateRegistryOwner(null)
            lifecycleRoot = null
            savedStateRoot = null
        }
    }
}
