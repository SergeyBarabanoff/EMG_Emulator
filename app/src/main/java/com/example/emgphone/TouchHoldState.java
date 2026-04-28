package com.example.emgphone;

/**
 * Tracks whether a virtual touch is currently being held down.
 *
 * <p>This class is a small state holder used in tests or controller logic where touch-down and
 * touch-up are modeled as a toggle.</p>
 */
public class TouchHoldState {
    /** Whether touch is currently held down. */
    private boolean held = false;

    /**
     * Returns whether touch is currently held.
     *
     * @return {@code true} if touch is held down, otherwise {@code false}
     */
    public boolean isHeld() {
        return held;
    }

    /**
     * Toggles the current touch-held state.
     *
     * <p>If touch was not held, it becomes held. If it was already held, it becomes released.</p>
     */
    public void toggle() {
        held = !held;
    }

    /**
     * Resets the touch state to released.
     */
    public void reset() {
        held = false;
    }
}