package org.robotframework.ide.core.testData.text.context.recognizer.settingTable;

import org.robotframework.ide.core.testData.text.context.recognizer.ATableElementRecognizer;
import org.robotframework.ide.core.testData.text.lexer.RobotWordType;


public class TestTeardownDeclaration extends ATableElementRecognizer {

    public TestTeardownDeclaration() {
        super(SettingTableRobotContextType.TABLE_SETTINGS_TEST_TEARDOWN,
                createExpectedWithOptionalColonAsLast(RobotWordType.TEST_WORD,
                        RobotWordType.TEARDOWN_WORD));
    }
}
