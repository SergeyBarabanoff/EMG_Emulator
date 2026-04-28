package com.example.emgphone;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building a deterministic square-movement test sequence.
 *
 * <p>The generated sequence moves the virtual cursor in a square and inserts tap-toggle pairs
 * before and after each side so touch down/up behavior can be exercised during testing.</p>
 */
public class TestSquareSequence {

    /**
     * Builds a square test sequence using the given number of movement steps per side.
     *
     * <p>The sequence starts with a tap-toggle pair, then moves right, down, left, and up,
     * inserting another tap-toggle pair after each side.</p>
     *
     * @param stepsPerSide number of move commands to generate for each side of the square
     * @return ordered list of commands representing the full test sequence
     */
    public static List<PhoneCommand> buildSequence(int stepsPerSide) {
        List<PhoneCommand> commands = new ArrayList<>();

        commands.add(PhoneCommand.TAP);
        commands.add(PhoneCommand.TAP);

        addRepeated(commands, PhoneCommand.MOVE_RIGHT, stepsPerSide);
        commands.add(PhoneCommand.TAP);
        commands.add(PhoneCommand.TAP);

        addRepeated(commands, PhoneCommand.MOVE_DOWN, stepsPerSide);
        commands.add(PhoneCommand.TAP);
        commands.add(PhoneCommand.TAP);

        addRepeated(commands, PhoneCommand.MOVE_LEFT, stepsPerSide);
        commands.add(PhoneCommand.TAP);
        commands.add(PhoneCommand.TAP);

        addRepeated(commands, PhoneCommand.MOVE_UP, stepsPerSide);
        commands.add(PhoneCommand.TAP);
        commands.add(PhoneCommand.TAP);

        return commands;
    }

    /**
     * Appends the same command to a command list repeatedly.
     *
     * @param commands destination list
     * @param command command to append
     * @param count number of times to append the command
     */
    private static void addRepeated(List<PhoneCommand> commands, PhoneCommand command, int count) {
        for (int i = 0; i < count; i++) {
            commands.add(command);
        }
    }
}