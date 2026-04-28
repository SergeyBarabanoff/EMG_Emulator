package com.example.emgphone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CursorStateTest {

    @Test
    public void move_updatesPositionWithinBounds() {
        CursorState cursor = new CursorState(100, 100, 50);

        cursor.move(20, -10, 500, 800);

        assertEquals(120, cursor.getX());
        assertEquals(90, cursor.getY());
    }

    @Test
    public void move_clampsLeftAndTopAtZero() {
        CursorState cursor = new CursorState(10, 10, 50);

        cursor.move(-100, -100, 500, 800);

        assertEquals(0, cursor.getX());
        assertEquals(0, cursor.getY());
    }

    @Test
    public void move_clampsRightAndBottomAtScreenEdge() {
        CursorState cursor = new CursorState(440, 760, 50);

        cursor.move(100, 100, 500, 800);

        assertEquals(450, cursor.getX()); // 500 - 50
        assertEquals(750, cursor.getY()); // 800 - 50
    }
}