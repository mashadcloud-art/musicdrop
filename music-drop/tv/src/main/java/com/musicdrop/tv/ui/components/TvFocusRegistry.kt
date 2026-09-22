package com.musicdrop.tv.ui.components

/**
 * Global TV Remote Focus Registry.
 * Holds reference to the currently focused button's onClick action,
 * allowing TvMainActivity.dispatchKeyEvent to trigger clicks directly
 * when hardware OK/ENTER/DPAD_CENTER is pressed, bypassing buggy AOSP WindowManager drop.
 */
object TvFocusRegistry {
    @Volatile
    var activeClickAction: (() -> Unit)? = null

    fun triggerActiveClick(): Boolean {
        val action = activeClickAction
        if (action != null) {
            action.invoke()
            return true
        }
        return false
    }
}
