package com.example.emgphone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PhoneCommandTest {

    @Test
    public void fromString_returnsMatchingCommand() {
        assertEquals(PhoneCommand.MOVE_UP, PhoneCommand.fromString("MOVE_UP"));
        assertEquals(PhoneCommand.TAP, PhoneCommand.fromString("TAP"));
        assertEquals(PhoneCommand.RECENTS, PhoneCommand.fromString("RECENTS"));
    }

    @Test
    public void fromString_returnsUnknownForNull() {
        assertEquals(PhoneCommand.UNKNOWN, PhoneCommand.fromString(null));
    }

    @Test
    public void fromString_returnsUnknownForBadValue() {
        assertEquals(PhoneCommand.UNKNOWN, PhoneCommand.fromString("BANANA"));
    }
}