package com.securefile.util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InputValidator {

    private static final String ENCRYPTED_FILE_EXTENSION = ".sfe";

    private InputValidator() {}

    public static Path validateInputFile(Path inputFile) {
        if (inputFile == null) {
            throw new IllegalArgumentException("Select an input file.");
        }

        Path normalized = inputFile.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IllegalArgumentException("The selected input file does not exist.");
        }
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("The selected input path is not a file.");
        }
        if (!Files.isReadable(normalized)) {
            throw new IllegalArgumentException("The selected input file cannot be read.");
        }
        return normalized;
    }

    public static Path validateEncryptedInputFile(Path inputFile) {
        Path normalized = validateInputFile(inputFile);
        String fileName = normalized.getFileName() == null ? "" : normalized.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(ENCRYPTED_FILE_EXTENSION)) {
            throw new IllegalArgumentException("Select a .sfe encrypted input file for decryption.");
        }
        return normalized;
    }

    public static Path validateOutputFile(Path inputFile, Path outputFile) {
        if (outputFile == null) {
            throw new IllegalArgumentException("Select an output file.");
        }

        Path normalized = outputFile.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent == null || !Files.exists(parent) || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException("The output folder does not exist.");
        }
        if (!Files.isWritable(parent)) {
            throw new IllegalArgumentException("The output folder is not writable.");
        }
        if (Files.exists(normalized) && !Files.isWritable(normalized)) {
            throw new IllegalArgumentException("The selected output file is not writable.");
        }
        if (inputFile != null && inputFile.toAbsolutePath().normalize().equals(normalized)) {
            throw new IllegalArgumentException("Input and output files must be different.");
        }
        return normalized;
    }

    public static void validatePassword(char[] password, boolean requireMinimumLength) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Enter a password.");
        }
        if (requireMinimumLength && password.length < 12) {
            throw new IllegalArgumentException("Use a password with at least 12 characters.");
        }
    }
}