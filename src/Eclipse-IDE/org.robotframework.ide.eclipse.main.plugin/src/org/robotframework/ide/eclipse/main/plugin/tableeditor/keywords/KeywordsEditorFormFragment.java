package org.robotframework.ide.eclipse.main.plugin.tableeditor.keywords;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.robotframework.ide.eclipse.main.plugin.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.RobotKeywordDefinition;
import org.robotframework.ide.eclipse.main.plugin.RobotKeywordsSection;
import org.robotframework.ide.eclipse.main.plugin.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.RobotSuiteFileSection;
import org.robotframework.ide.eclipse.main.plugin.cmd.CreateFreshKeywordCallCommand;
import org.robotframework.ide.eclipse.main.plugin.cmd.CreateFreshKeywordDefinitionCommand;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotElementEditingSupport.NewElementsCreator;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotSuiteEditorEvents;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.code.CodeEditorFormFragment;

public class KeywordsEditorFormFragment extends CodeEditorFormFragment {

    @Override
    protected ITreeContentProvider createContentProvider() {
        return new UserKeywordsContentProvider();
    }

    @Override
    protected String getContextMenuId() {
        return "org.robotframework.ide.eclipse.editor.page.keywords.contextMenu";
    }

    @Override
    protected boolean sectionIsDefined() {
        return fileModel.findSection(RobotKeywordsSection.class).isPresent();
    }

    @Override
    protected RobotSuiteFileSection getSection() {
        return (RobotSuiteFileSection) fileModel.findSection(RobotKeywordsSection.class).orNull();
    }

    @Override
    protected NewElementsCreator provideNewElementsCreator() {
        return new NewElementsCreator() {

            @Override
            public RobotElement createNew(final Object parent) {
                if (parent instanceof RobotKeywordsSection) {
                    final RobotKeywordsSection section = (RobotKeywordsSection) parent;
                    commandsStack.execute(new CreateFreshKeywordDefinitionCommand(section, true));
                    return section.getChildren().get(section.getChildren().size() - 1);
                } else if (parent instanceof RobotKeywordDefinition) {
                    final RobotKeywordDefinition definition = (RobotKeywordDefinition) parent;
                    commandsStack.execute(new CreateFreshKeywordCallCommand(definition, true));
                    return definition.getChildren().get(definition.getChildren().size() - 1);
                }
                return null;
            }
        };
    }

    @Inject
    @Optional
    private void whenUserRequestedFiltering(@UIEventTopic(RobotSuiteEditorEvents.SECTION_FILTERING_TOPIC + "/"
            + RobotKeywordsSection.SECTION_NAME) final String filter) {
        // nothing to do yet
    }

    @Inject
    @Optional
    private void whenKeywordIsAddedOrRemoved(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_DEFINITION_STRUCTURAL_ALL) final RobotSuiteFileSection section) {
        if (section.getSuiteFile() == fileModel) {
            viewer.refresh();
            setDirty();
        }
    }

    @Inject
    @Optional
    private void whenKeywordCallIsAddedOrRemoved(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_CALL_STRUCTURAL_ALL) final RobotKeywordDefinition definition) {
        if (definition.getSuiteFile() == fileModel) {
            viewer.refresh(definition);
            setDirty();
        }
    }

    @Inject
    @Optional
    private void whenKeywordDetailIsChanged(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_DEFINITION_CHANGE_ALL) final RobotKeywordDefinition definition) {
        if (definition.getSuiteFile() == fileModel) {
            viewer.update(definition, null);
            setDirty();
        }
    }

    @Inject
    @Optional
    private void whenKeywordCallDetailIsChanged(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_CALL_DETAIL_CHANGE_ALL) final RobotKeywordCall keywordCall) {
        if (keywordCall.getParent() instanceof RobotKeywordDefinition && keywordCall.getSuiteFile() == fileModel) {
            viewer.update(keywordCall, null);
            setDirty();
        }
    }
}
