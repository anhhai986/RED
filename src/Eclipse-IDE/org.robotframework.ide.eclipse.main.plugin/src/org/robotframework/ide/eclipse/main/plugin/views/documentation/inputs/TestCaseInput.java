/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.views.documentation.inputs;

import static com.google.common.collect.Lists.newArrayList;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.rf.ide.core.libraries.Documentation;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCase;
import org.robotframework.ide.eclipse.main.plugin.model.RobotDefinitionSetting;
import org.robotframework.ide.eclipse.main.plugin.views.documentation.WorkspaceFileUri;

public class TestCaseInput extends InternalElementInput<RobotCase> {

    public TestCaseInput(final RobotCase testCase) {
        super(testCase);
    }

    @Override
    protected String createHeader() {
        return createHeader(element);
    }

    private static String createHeader(final RobotCase test) {
        final Optional<String> templateInUse = test.getTemplateInUse();
        final Optional<URI> imgUri = templateInUse.map(t -> RedImages.getTemplatedTestCaseImageUri())
                .orElseGet(() -> RedImages.getTestCaseImageUri());
        
        final IFile file = test.getSuiteFile().getFile();

        final String srcHref = createShowTestSrcUri(file, test.getName());
        final String srcLabel = file.getFullPath().toString();
        final String docHref = createShowSuiteDocUri(file);

        final String source = String.format("%s [%s]", Formatters.formatHyperlink(srcHref, srcLabel),
                Formatters.formatHyperlink(docHref, "Documentation"));

        final List<List<String>> table = new ArrayList<>();
        table.add(newArrayList("Source", source));
        templateInUse.ifPresent(template -> table.add(newArrayList("Template", template)));

        return Formatters.formatSimpleHeader(imgUri, test.getName(), table);
    }

    private static String createShowTestSrcUri(final IFile file, final String label) {
        try {
            return WorkspaceFileUri.createShowCaseSourceUri(file, label).toString();
        } catch (final URISyntaxException e) {
            return "#";
        }
    }

    private static String createShowSuiteDocUri(final IFile file) {
        try {
            return WorkspaceFileUri.createShowSuiteDocUri(file).toString();
        } catch (final URISyntaxException e) {
            return "#";
        }
    }

    @Override
    protected Documentation createDocumentation() {
        return element.createDocumentation();
    }

    public static class TestCaseOnSettingInput extends InternalElementInput<RobotDefinitionSetting> {

        public TestCaseOnSettingInput(final RobotDefinitionSetting element) {
            super(element);
        }

        @Override
        protected String createHeader() {
            return TestCaseInput.createHeader((RobotCase) element.getParent());
        }

        @Override
        protected Documentation createDocumentation() {
            return ((RobotCase) element.getParent()).createDocumentation();
        }
    }
}
