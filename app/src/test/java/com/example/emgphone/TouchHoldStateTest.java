package com.example.emgphone;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TouchHoldStateTest {

    @Test
    public void toggle_changesHeldState() {
        TouchHoldState state = new TouchHoldState();

        assertFalse(state.isHeld());

        state.toggle();
        assertTrue(state.isHeld());

        state.toggle();
        assertFalse(state.isHeld());
    }

    @Test
    public void reset_clearsHeldState() {
        TouchHoldState state = new TouchHoldState();

        state.toggle();
        assertTrue(state.isHeld());

        state.reset();
        assertFalse(state.isHeld());
    }
}