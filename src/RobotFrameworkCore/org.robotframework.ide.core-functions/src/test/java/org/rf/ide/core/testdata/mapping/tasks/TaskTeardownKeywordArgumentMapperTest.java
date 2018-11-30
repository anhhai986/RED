package org.rf.ide.core.testdata.mapping.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Stack;

import org.junit.Test;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.TaskTable;
import org.rf.ide.core.testdata.model.table.tasks.Task;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class TaskTeardownKeywordArgumentMapperTest {

    @Test
    public void theMapperIsOnlyUsedForRobotNewerThan31() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        assertThat(mapper.isApplicableFor(new RobotVersion(2, 8))).isFalse();
        assertThat(mapper.isApplicableFor(new RobotVersion(2, 9))).isFalse();
        assertThat(mapper.isApplicableFor(new RobotVersion(3, 0, 1))).isFalse();
        assertThat(mapper.isApplicableFor(new RobotVersion(3, 1))).isTrue();
        assertThat(mapper.isApplicableFor(new RobotVersion(3, 2))).isTrue();
    }

    @Test
    public void mapperCannotMap_whenParserNotInTaskTeardownState() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        final RobotFileOutput output = createOutputModel();
        final Stack<ParsingState> states = stack();
        assertThat(mapper.checkIfCanBeMapped(output, null, null, null, states)).isFalse();
    }

    @Test
    public void mapperCannotMap_whenParserInTaskTeardownStateAndLastTaskTeardownHasNoKeywordYet() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        final RobotFileOutput output = createOutputModel();
        addTaskTeardown(output, "kw");
        addTaskTeardown(output);
        final Stack<ParsingState> states = stack(ParsingState.TASK_SETTING_TEARDOWN);
        assertThat(mapper.checkIfCanBeMapped(output, null, null, null, states)).isFalse();
    }

    @Test
    public void mapperCanMap_whenParserInTaskTeardownStateButLastTaskTeardownHasKeywordAlready() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        final RobotFileOutput output = createOutputModel();
        addTaskTeardown(output);
        addTaskTeardown(output, "kw");
        final Stack<ParsingState> states = stack(ParsingState.TASK_SETTING_TEARDOWN);
        assertThat(mapper.checkIfCanBeMapped(output, null, null, null, states)).isTrue();
    }

    @Test
    public void mapperCanMap_whenParserInTaskTeardownKeywordState() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        final RobotFileOutput output = createOutputModel();
        addTaskTeardown(output, "kw");
        final Stack<ParsingState> states = stack(ParsingState.TASK_SETTING_TEARDOWN_KEYWORD);
        assertThat(mapper.checkIfCanBeMapped(output, null, null, null, states)).isTrue();
    }

    @Test
    public void mapperCanMap_whenParserInTaskSetupKeywordArgumentState() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        final RobotFileOutput output = createOutputModel();
        addTaskTeardown(output, "kw", "arg1");
        final Stack<ParsingState> states = stack(ParsingState.TASK_SETTING_TEARDOWN_KEYWORD_ARGUMENT);
        assertThat(mapper.checkIfCanBeMapped(output, null, null, null, states)).isTrue();
    }

    @Test
    public void whenMapped_theTeardownHasNewKeywordArgumentAddedAndParsingStateIsUpdated() {
        final TaskTeardownKeywordArgumentMapper mapper = new TaskTeardownKeywordArgumentMapper();

        final RobotFileOutput output = createOutputModel();
        final LocalSetting<Task> teardown = addTaskTeardown(output, "kw");
        final Stack<ParsingState> states = stack(ParsingState.TASK_SETTING_TEARDOWN);
        final RobotToken token = mapper.map(null, states, output, RobotToken.create(""), null, "arg1");

        assertThat(teardown.getTokensWithoutDeclaration()).last().isSameAs(token);
        assertThat(token.getTypes()).contains(RobotTokenType.TASK_SETTING_TEARDOWN_KEYWORD_ARGUMENT);
        assertThat(token.getText()).isEqualTo("arg1");

        assertThat(states).containsExactly(ParsingState.TASK_SETTING_TEARDOWN,
                ParsingState.TASK_SETTING_TEARDOWN_KEYWORD_ARGUMENT);
    }

    private static Stack<ParsingState> stack(final ParsingState... states) {
        final Stack<ParsingState> statesStack = new Stack<>();
        for (final ParsingState state : states) {
            statesStack.push(state);
        }
        return statesStack;
    }

    private static final RobotFileOutput createOutputModel() {
        final RobotFileOutput output = new RobotFileOutput(new RobotVersion(3, 1));
        final RobotFile fileModel = output.getFileModel();
        fileModel.includeTaskTableSection();
        fileModel.getTasksTable().createTask("task");
        return output;
    }

    private static final LocalSetting<Task> addTaskTeardown(final RobotFileOutput output,
            final String... settingCells) {
        final TaskTable table = output.getFileModel().getTasksTable();
        final Task task = table.getTasks().get(0);
        final LocalSetting<Task> teardown = task.newTeardown(task.getTeardowns().size());
        for (final String cell : settingCells) {
            teardown.addToken(cell);
        }
        return teardown;
    }
}