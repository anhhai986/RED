/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.project;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class ImportSearchPaths {

    private final PathsProvider pathsProvider;

    public ImportSearchPaths(final PathsProvider pathsProvider) {
        this.pathsProvider = pathsProvider;
    }

    public URI getAbsoluteUri(final URI importingFileUri, final ResolvedImportPath importPath) {
        return findAbsoluteUri(importingFileUri, importPath).get();
    }

    public Optional<URI> findAbsoluteUri(final URI importingFileUri, final ResolvedImportPath importPath) {
        return findAbsoluteMarkedUri(importingFileUri, importPath).map(MarkedUri::getPath);
    }

    public Optional<MarkedUri> findAbsoluteMarkedUri(final URI importingFileUri, final ResolvedImportPath importPath) {

        final URI absoluteUri = importPath.resolveInRespectTo(importingFileUri);
        if (pathsProvider.targetExists(absoluteUri)) {
            return Optional.of(new MarkedUri(absoluteUri, PathRelativityPoint.FILE));
        }

        final List<File> modulesSearchPaths = pathsProvider.providePythonModulesSearchPaths();
        for (final File moduleSearchPath : modulesSearchPaths) {
            final URI searchPathUri = importPath.resolveInRespectTo(moduleSearchPath.toURI());
            if (pathsProvider.targetExists(searchPathUri)) {
                return Optional.of(new MarkedUri(searchPathUri, PathRelativityPoint.PYTHON_MODULE_SEARCH_PATH));
            }
        }

        final List<File> userSearchPaths = pathsProvider.provideUserSearchPaths();
        for (final File userSearchPath : userSearchPaths) {
            final URI searchPathUri = importPath.resolveInRespectTo(userSearchPath.toURI());
            if (pathsProvider.targetExists(searchPathUri)) {
                return Optional.of(new MarkedUri(searchPathUri, PathRelativityPoint.USER_DEFINED_SEARCH_PATH));
            }
        }
        return Optional.empty();
    }

    /**
     * Provides paths which are used for relative paths resolution.
     * 
     * @author anglart
     */
    public static interface PathsProvider {

        boolean targetExists(URI uri);

        List<File> providePythonModulesSearchPaths();

        List<File> provideUserSearchPaths();
    }

    /**
     * Wraps URI together with flag signaling how its relativeness have been resolved
     * 
     * @author anglart
     */
    public static class MarkedUri {

        private final URI uri;

        private final PathRelativityPoint relativity;

        public MarkedUri(final URI uri, final PathRelativityPoint relativity) {
            this.uri = uri;
            this.relativity = relativity;
        }

        public URI getPath() {
            return uri;
        }

        public PathRelativityPoint getRelativity() {
            return relativity;
        }

        public boolean isAbsolute() {
            return relativity == PathRelativityPoint.NONE;
        }

        public boolean isRelativeLocally() {
            return relativity == PathRelativityPoint.FILE;
        }

        public boolean isRelativeGlobally() {
            return !isAbsolute() && !isRelativeLocally();
        }
    }

    public static enum PathRelativityPoint {
        NONE,                       // for absolute uris
        FILE,                       // uri is relative in respect to file given
        PYTHON_MODULE_SEARCH_PATH,  // uri is relative in respect to some path taken from Python sys.path path
        USER_DEFINED_SEARCH_PATH    // uri is relative in respect to some path defined by user
    }
}
