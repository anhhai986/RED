/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.table;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.junit.Test;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

/**
 * @author lwlodarc
 *
 */
public class NestedVariablesTest {

    private final ElementsUtility elementsUtility = new ElementsUtility();

    @Test
    public void robotTokenHasProperTypes_forNestedDictionaryVariable() {
        testNestedVariableOfTypeAndSymbol(RobotTokenType.VARIABLES_DICTIONARY_DECLARATION, "&");
    }

    @Test
    public void robotTokenHasProperTypes_forNestedListVariable() {
        testNestedVariableOfTypeAndSymbol(RobotTokenType.VARIABLES_LIST_DECLARATION, "@");
    }

    @Test
    public void robotTokenHasProperTypes_forNestedScalarVariable() {
        testNestedVariableOfTypeAndSymbol(RobotTokenType.VARIABLES_SCALAR_DECLARATION, "$");
    }

    private void testNestedVariableOfTypeAndSymbol(final RobotTokenType type, final String symbol) {
        // prepare
        final Stack<ParsingState> processingState = new Stack<>();
        processingState.push(ParsingState.KEYWORD_TABLE_INSIDE);
        processingState.push(ParsingState.KEYWORD_DECLARATION);
        processingState.push(ParsingState.KEYWORD_INSIDE_ACTION);
        final FilePosition fp = new FilePosition(10, 10, -1);
        final FilePosition fpInside = new FilePosition(10, 12, -1);

        final String text = symbol + "{${var_name}}";

        final List<RobotToken> robotTokens = new ArrayList<>();
        robotTokens.add(RobotToken.create("${var_name}}", fpInside, RobotTokenType.VARIABLES_SCALAR_DECLARATION));
        robotTokens.add(RobotToken.create(text, fp, type));
        final List<IRobotTokenType> wantedResults = new ArrayList<>();
        wantedResults.add(type);
        wantedResults.add(RobotTokenType.VARIABLE_USAGE);

        // execute
        final RobotToken result = elementsUtility.computeCorrectRobotToken(processingState, fp, text, robotTokens);

        // verify
        assertThat(result.getText()).isEqualTo(text);
        assertThat(result.getFilePosition()).isEqualTo(fp);
        assertThat(result.getTypes()).hasSameElementsAs(wantedResults);
    }
}
