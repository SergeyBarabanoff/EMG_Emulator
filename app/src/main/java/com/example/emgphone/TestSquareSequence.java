package com.example.emgphone;

import java.util.ArrayList;
import java.util.List;

public class TestSquareSequence {

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

    private static void addRepeated(List<PhoneCommand> commands, PhoneCommand command, int count) {
        for (int i = 0; i < count; i++) {
            commands.add(command);
        }
    }
}