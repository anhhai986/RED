/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public final class RedTemporaryDirectory {

    private static final String DIR_NAME_PREFIX = "RobotTempDir";

    private static Path temporaryDirectory;

    public static File copyScriptFile(final String filename) throws IOException {
        try (InputStream source = getScriptFileAsStream(filename)) {
            return replaceTemporaryFile(filename, source);
        }
    }

    public static InputStream getScriptFileAsStream(final String filename) throws IOException {
        return RedTemporaryDirectory.class.getResourceAsStream("/scripts/" + filename);
    }

    public static File getTemporaryFile(final String filename) throws IOException {
        final Path tempDir = createTemporaryDirectoryIfNotExists();
        return new File(tempDir.toString() + File.separator + filename);
    }

    public static File createTemporaryFile(final String filename) throws IOException {
        final File tempFile = getTemporaryFile(filename);
        tempFile.delete();
        tempFile.createNewFile();
        return tempFile;
    }

    public static File replaceTemporaryFile(final String filename, final InputStream source) throws IOException {
        final File tempFile = getTemporaryFile(filename);
        Files.copy(source, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    public static synchronized Path createTemporaryDirectoryIfNotExists() throws IOException {
        if (temporaryDirectory != null && temporaryDirectory.toFile().exists()) {
            return temporaryDirectory;
        }
        temporaryDirectory = Files.createTempDirectory(DIR_NAME_PREFIX);
        addRemoveTemporaryDirectoryHook();
        return temporaryDirectory;
    }

    private static void addRemoveTemporaryDirectoryHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (temporaryDirectory == null) {
                return;
            }
            try {
                Files.walkFileTree(temporaryDirectory, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                            throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                            throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (final IOException e) {
                // temporary files and directory will not be removed
            }
        }));
    }

}
