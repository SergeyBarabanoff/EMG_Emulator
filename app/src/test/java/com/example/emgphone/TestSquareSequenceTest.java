package com.example.emgphone;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestSquareSequenceTest {

    @Test
    public void buildSequence_hasExpectedLength() {
        int steps = 8;
        List<PhoneCommand> commands = TestSquareSequence.buildSequence(steps);

        int expected = (4 * steps) + 10; // 2 taps at start + 2 taps after each side
        assertEquals(expected, commands.size());
    }

    @Test
    public void buildSequence_startsWithTouchTogglePair() {
        List<PhoneCommand> commands = TestSquareSequence.buildSequence(8);

        assertEquals(PhoneCommand.TAP, commands.get(0));
        assertEquals(PhoneCommand.TAP, commands.get(1));
    }

    @Test
    public void buildSequence_firstSideMovesRight() {
        int steps = 4;
        List<PhoneCommand> commands = TestSquareSequence.buildSequence(steps);

        assertEquals(PhoneCommand.MOVE_RIGHT, commands.get(2));
        assertEquals(PhoneCommand.MOVE_RIGHT, commands.get(3));
        assertEquals(PhoneCommand.MOVE_RIGHT, commands.get(4));
        assertEquals(PhoneCommand.MOVE_RIGHT, commands.get(5));
    }

    @Test
    public void buildSequence_endsWithTouchTogglePair() {
        List<PhoneCommand> commands = TestSquareSequence.buildSequence(8);

        int size = commands.size();
        assertEquals(PhoneCommand.TAP, commands.get(size - 2));
        assertEquals(PhoneCommand.TAP, commands.get(size - 1));
    }
}