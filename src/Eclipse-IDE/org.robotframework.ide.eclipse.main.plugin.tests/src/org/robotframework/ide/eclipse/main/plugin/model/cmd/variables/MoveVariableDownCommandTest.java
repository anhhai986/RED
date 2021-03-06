/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model.cmd.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.Test;
import org.robotframework.ide.eclipse.main.plugin.mockmodel.RobotSuiteFileCreator;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.model.RobotVariable;
import org.robotframework.ide.eclipse.main.plugin.model.RobotVariablesSection;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.EditorCommand;

public class MoveVariableDownCommandTest {

    @Test
    public void nothingHappens_whenLastVariableIsTriedToBeMovedDown() {
        final IEventBroker eventBroker = mock(IEventBroker.class);

        final RobotVariablesSection varSection = createVariables();
        final RobotVariable variableToMove = varSection.getChildren().get(varSection.getChildren().size() - 1);

        final MoveVariableDownCommand command = new MoveVariableDownCommand(variableToMove);
        command.setEventBroker(eventBroker);

        command.execute();
        assertThat(varSection.getChildren()).extracting(RobotElement::getName)
                .containsExactly("scalar", "scalar_as_list", "list", "dict", "invalid}");

        for (final EditorCommand undo : command.getUndoCommands()) {
            undo.execute();
        }
        assertThat(varSection.getChildren()).extracting(RobotElement::getName)
                .containsExactly("scalar", "scalar_as_list", "list", "dict", "invalid}");

        verifyZeroInteractions(eventBroker);
    }

    @Test
    public void firstVariableIsMovedDownProperly() {
        final IEventBroker eventBroker = mock(IEventBroker.class);

        final RobotVariablesSection varSection = createVariables();
        final RobotVariable variableToMove = varSection.getChildren().get(0);

        final MoveVariableDownCommand command = new MoveVariableDownCommand(variableToMove);
        command.setEventBroker(eventBroker);

        command.execute();
        assertThat(varSection.getChildren()).extracting(RobotElement::getName)
                .containsExactly("scalar_as_list", "scalar", "list", "dict", "invalid}");

        for (final EditorCommand undo : command.getUndoCommands()) {
            undo.execute();
        }
        assertThat(varSection.getChildren()).extracting(RobotElement::getName)
                .containsExactly("scalar", "scalar_as_list", "list", "dict", "invalid}");

        verify(eventBroker, times(2)).send(RobotModelEvents.ROBOT_VARIABLE_MOVED, varSection);
        verifyNoMoreInteractions(eventBroker);
    }

    @Test
    public void someInnerVariableIsMovedDownProperly() {
        final IEventBroker eventBroker = mock(IEventBroker.class);

        final RobotVariablesSection varSection = createVariables();
        final RobotVariable variableToMove = varSection.getChildren().get(2);

        final MoveVariableDownCommand command = new MoveVariableDownCommand(variableToMove);
        command.setEventBroker(eventBroker);

        command.execute();
        assertThat(varSection.getChildren()).extracting(RobotElement::getName)
                .containsExactly("scalar", "scalar_as_list", "dict", "list", "invalid}");

        for (final EditorCommand undo : command.getUndoCommands()) {
            undo.execute();
        }
        assertThat(varSection.getChildren()).extracting(RobotElement::getName)
                .containsExactly("scalar", "scalar_as_list", "list", "dict", "invalid}");

        verify(eventBroker, times(2)).send(RobotModelEvents.ROBOT_VARIABLE_MOVED, varSection);
        verifyNoMoreInteractions(eventBroker);
    }

    private static RobotVariablesSection createVariables() {
        final RobotSuiteFile model = new RobotSuiteFileCreator().appendLine("*** Variables ***")
                .appendLine("${scalar}  0")
                .appendLine("${scalar_as_list}  0  1  2")
                .appendLine("@{list}  1  2  3")
                .appendLine("&{dict}  a=1  b=2  c=3  d=4")
                .appendLine("invalid}  1  2  3")
                .build();
        return model.findSection(RobotVariablesSection.class).get();
    }
}
