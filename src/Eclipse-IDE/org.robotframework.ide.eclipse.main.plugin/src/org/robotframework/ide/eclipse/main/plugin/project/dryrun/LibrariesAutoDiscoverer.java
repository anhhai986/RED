/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.dryrun;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.rf.ide.core.dryrun.RobotDryRunLibraryEventListener;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport.DryRunLibraryImportStatus;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport.DryRunLibraryType;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImportCollector;
import org.rf.ide.core.executor.EnvironmentSearchPaths;
import org.rf.ide.core.executor.RobotRuntimeEnvironment.RobotEnvironmentException;
import org.rf.ide.core.libraries.LibraryDescriptor;
import org.rf.ide.core.project.RobotProjectConfig.ExcludedFolderPath;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.rf.ide.core.project.RobotProjectConfig.RemoteLocation;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.LibrariesConfigUpdater;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.editor.libraries.ILibraryClass;
import org.robotframework.ide.eclipse.main.plugin.project.editor.libraries.JarStructureBuilder;
import org.robotframework.ide.eclipse.main.plugin.project.editor.libraries.PythonLibStructureBuilder;

import com.google.common.collect.Sets;

/**
 * @author mmarzec
 */
public abstract class LibrariesAutoDiscoverer extends AbstractAutoDiscoverer {

    private final Consumer<Collection<RobotDryRunLibraryImport>> summaryHandler;

    private final RobotDryRunLibraryImportCollector dryRunLibraryImportCollector;

    LibrariesAutoDiscoverer(final RobotProject robotProject,
            final Consumer<Collection<RobotDryRunLibraryImport>> summaryHandler) {
        super(robotProject);
        this.summaryHandler = summaryHandler;
        this.dryRunLibraryImportCollector = new RobotDryRunLibraryImportCollector(
                robotProject.getLibraryDescriptorsStream()
                        .filter(LibraryDescriptor::isStandardLibrary)
                        .map(LibraryDescriptor::getName)
                        .collect(toSet()));
    }

    @Override
    public Job start() {
        if (lockDryRun()) {
            final WorkspaceJob wsJob = new WorkspaceJob("Discovering libraries") {

                @Override
                public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                    try {
                        prepareDiscovering(monitor);
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        startDiscovering(monitor);
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        final List<RobotDryRunLibraryImport> libraryImports = getImportedLibraries();
                        startAddingLibrariesToProjectConfiguration(monitor, libraryImports);
                        summaryHandler.accept(libraryImports);
                    } catch (final CoreException e) {
                        throw new AutoDiscovererException("Problems occurred during discovering libraries.", e);
                    } catch (final InterruptedException e) {
                        // fine, will simply stop dry run
                    } finally {
                        monitor.done();
                        unlockDryRun();
                    }

                    return Status.OK_STATUS;
                }

                @Override
                protected void canceling() {
                    stopDiscovering();
                }
            };
            wsJob.setUser(true);
            wsJob.schedule();
            return wsJob;
        }
        return null;
    }

    abstract void prepareDiscovering(IProgressMonitor monitor) throws CoreException;

    List<RobotDryRunLibraryImport> getImportedLibraries() {
        return dryRunLibraryImportCollector.getImportedLibraries();
    }

    @Override
    RobotDryRunLibraryEventListener createDryRunCollectorEventListener(final Consumer<String> libNameHandler) {
        return new RobotDryRunLibraryEventListener(dryRunLibraryImportCollector, libNameHandler);
    }

    @Override
    void startDryRunClient(final int port, final File dataSource) throws CoreException {
        final File projectLocation = robotProject.getProject().getLocation().toFile();
        final boolean recursiveInVirtualenv = RedPlugin.getDefault()
                .getPreferences()
                .isProjectModulesRecursiveAdditionOnVirtualenvEnabled();
        final List<String> excludedPaths = robotProject.getRobotProjectConfig()
                .getExcludedPath()
                .stream()
                .map(ExcludedFolderPath::getPath)
                .collect(toList());
        final EnvironmentSearchPaths additionalPaths = new EnvironmentSearchPaths(robotProject.getClasspath(),
                robotProject.getPythonpath());

        robotProject.getRuntimeEnvironment().startLibraryAutoDiscovering(port, dataSource, projectLocation,
                recursiveInVirtualenv, excludedPaths, additionalPaths);
    }

    void setImportersPaths(final RobotDryRunLibraryImport libraryImport, final Collection<RobotSuiteFile> collection) {
        final Collection<URI> importersPaths = collection.stream()
                .map(suite -> suite.getFile().getLocation().toFile().toURI())
                .collect(toList());
        libraryImport.setImportersPaths(importersPaths);
    }

    private void startAddingLibrariesToProjectConfiguration(final IProgressMonitor monitor,
            final List<RobotDryRunLibraryImport> libraryImports) {
        if (!libraryImports.isEmpty()) {
            final ImportedLibrariesConfigUpdater updater = new ImportedLibrariesConfigUpdater(robotProject);
            final List<RobotDryRunLibraryImport> libraryImportsToAdd = updater.getLibraryImportsToAdd(libraryImports);

            final SubMonitor subMonitor = SubMonitor.convert(monitor);
            subMonitor.subTask("Adding libraries to project configuration...");
            subMonitor.setWorkRemaining(libraryImportsToAdd.size() + 1);
            for (final RobotDryRunLibraryImport libraryImport : libraryImportsToAdd) {
                updater.addLibrary(libraryImport);
                subMonitor.worked(1);
            }

            subMonitor.subTask("Updating project configuration...");
            final IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
            updater.finalizeLibrariesAdding(eventBroker);
            subMonitor.worked(1);
        }
    }

    private static class ImportedLibrariesConfigUpdater extends LibrariesConfigUpdater {

        ImportedLibrariesConfigUpdater(final RobotProject robotProject) {
            super(robotProject);
        }

        List<RobotDryRunLibraryImport> getLibraryImportsToAdd(final List<RobotDryRunLibraryImport> libraryImports) {
            final Set<String> existingReferencedLibraryNames = config.getLibraries()
                    .stream()
                    .map(ReferencedLibrary::getName)
                    .collect(toSet());
            final Set<String> existingRemoteLibraryNames = config.getRemoteLocations()
                    .stream()
                    .map(RemoteLocation::getRemoteName)
                    .collect(toSet());
            final Set<String> existingLibraryNames = Sets.union(existingReferencedLibraryNames,
                    existingRemoteLibraryNames);

            final List<RobotDryRunLibraryImport> result = new ArrayList<>();
            for (final RobotDryRunLibraryImport libraryImport : libraryImports) {
                if (libraryImport.getType() != DryRunLibraryType.UNKNOWN) {
                    if (existingLibraryNames.contains(libraryImport.getName())) {
                        libraryImport.setStatus(DryRunLibraryImportStatus.ALREADY_EXISTING);
                    } else {
                        result.add(libraryImport);
                    }
                }
            }
            result.sort((import1, import2) -> import1.getName().compareToIgnoreCase(import2.getName()));
            return result;
        }

        void addLibrary(final RobotDryRunLibraryImport libraryImport) {
            if (libraryImport.getType() == DryRunLibraryType.JAVA) {
                addJavaLibrary(libraryImport);
            } else if (libraryImport.getType() == DryRunLibraryType.PYTHON) {
                addPythonLibrary(libraryImport);
            } else if (libraryImport.getType() == DryRunLibraryType.REMOTE) {
                addRemoteLibrary(libraryImport);
            }
        }

        private void addPythonLibrary(final RobotDryRunLibraryImport libraryImport) {
            final PythonLibStructureBuilder pythonLibStructureBuilder = new PythonLibStructureBuilder(
                    robotProject.getRuntimeEnvironment(), robotProject.getRobotProjectConfig(),
                    robotProject.getProject());
            try {
                final Collection<ILibraryClass> libraryClasses = pythonLibStructureBuilder
                        .provideAllEntriesFromFile(libraryImport.getSourcePath());
                addReferencedLibrariesFromClasses(libraryImport, libraryClasses);
            } catch (final RobotEnvironmentException e) {
                final Optional<File> modulePath = findPythonLibraryModulePath(libraryImport);
                if (modulePath.isPresent()) {
                    final Path path = new Path(modulePath.get().getPath());
                    final ReferencedLibrary newLibrary = ReferencedLibrary.create(LibraryType.PYTHON,
                            libraryImport.getName(), path.toPortableString());
                    addLibraries(Collections.singletonList(newLibrary));
                } else {
                    libraryImport.setStatus(DryRunLibraryImportStatus.NOT_ADDED);
                    libraryImport.setAdditionalInfo(e.getMessage());
                }
            }
        }

        private Optional<File> findPythonLibraryModulePath(final RobotDryRunLibraryImport libraryImport) {
            try {
                final EnvironmentSearchPaths envSearchPaths = new RedEclipseProjectConfig(config)
                        .createEnvironmentSearchPaths(robotProject.getProject());
                return robotProject.getRuntimeEnvironment().getModulePath(libraryImport.getName(), envSearchPaths);
            } catch (final RobotEnvironmentException e) {
                return Optional.empty();
            }
        }

        private void addJavaLibrary(final RobotDryRunLibraryImport libraryImport) {
            final JarStructureBuilder jarStructureBuilder = new JarStructureBuilder(
                    robotProject.getRuntimeEnvironment(), robotProject.getRobotProjectConfig(),
                    robotProject.getProject());
            try {
                final Collection<ILibraryClass> libraryClasses = jarStructureBuilder
                        .provideEntriesFromFile(libraryImport.getSourcePath());
                addReferencedLibrariesFromClasses(libraryImport, libraryClasses);
            } catch (final RobotEnvironmentException e) {
                libraryImport.setStatus(DryRunLibraryImportStatus.NOT_ADDED);
                libraryImport.setAdditionalInfo(e.getMessage());
            }
        }

        private void addRemoteLibrary(final RobotDryRunLibraryImport libraryImport) {
            final RemoteLocation remoteLibrary = RemoteLocation.create(libraryImport.getSourcePath());
            addRemoteLocation(remoteLibrary);
        }

        private void addReferencedLibrariesFromClasses(final RobotDryRunLibraryImport libraryImport,
                final Collection<ILibraryClass> libraryClasses) {
            final Collection<ReferencedLibrary> librariesToAdd = new ArrayList<>();
            for (final ILibraryClass libraryClass : libraryClasses) {
                if (libraryClass.getQualifiedName().equalsIgnoreCase(libraryImport.getName())) {
                    librariesToAdd.add(libraryClass.toReferencedLibrary(libraryImport.getSourcePath().getPath()));
                }
            }
            if (!librariesToAdd.isEmpty()) {
                addLibraries(librariesToAdd);
            } else {
                libraryImport.setStatus(DryRunLibraryImportStatus.NOT_ADDED);
                libraryImport.setAdditionalInfo("RED was unable to find class '" + libraryImport.getName()
                        + "' inside '" + libraryImport.getSourcePath() + "' module.");
            }
        }

    }

    @FunctionalInterface
    public interface DiscovererFactory {

        LibrariesAutoDiscoverer create(RobotProject project, Collection<RobotSuiteFile> suites);
    }

}
