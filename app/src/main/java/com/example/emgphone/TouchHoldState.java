package com.example.emgphone;

public class TouchHoldState {
    private boolean held = false;

    public boolean isHeld() {
        return held;
    }

    public void toggle() {
        held = !held;
    }

    public void reset() {
        held = false;
    }
}