package com.securefile.crypto;

import com.securefile.util.InputValidator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class FileCryptoService {

    private static final byte[] MAGIC = new byte[] { 'S', 'F', 'E', '1' };
    private static final int VERSION = 1;
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int BUFFER_SIZE = 8192;
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();

    public void encryptFile(Path inputFile, Path outputFile, char[] password) throws IOException, GeneralSecurityException {
        Path validatedInput = InputValidator.validateInputFile(inputFile);
        Path validatedOutput = InputValidator.validateOutputFile(validatedInput, outputFile);
        InputValidator.validatePassword(password, true);

        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);
        byte[] header = buildHeader(ITERATIONS, salt, iv);

        SecretKey secretKey = deriveKey(password, salt, ITERATIONS);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        cipher.updateAAD(header);

        processFile(validatedInput, validatedOutput, header, cipher);
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(iv, (byte) 0);
    }

    public void decryptFile(Path inputFile, Path outputFile, char[] password) throws IOException, GeneralSecurityException {
        Path validatedInput = InputValidator.validateEncryptedInputFile(inputFile);
        Path validatedOutput = InputValidator.validateOutputFile(validatedInput, outputFile);
        InputValidator.validatePassword(password, false);

        try (InputStream fileInput = new BufferedInputStream(Files.newInputStream(validatedInput))) {
            Header header = readHeader(fileInput);
            SecretKey secretKey = deriveKey(password, header.salt, header.iterations);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, header.iv));
            cipher.updateAAD(header.encodedHeader);

            processFile(fileInput, validatedOutput, null, cipher);
            header.clear();
        }
    }

    private void processFile(Path inputFile, Path outputFile, byte[] header, Cipher cipher)
            throws IOException, GeneralSecurityException {
        try (InputStream fileInput = new BufferedInputStream(Files.newInputStream(inputFile))) {
            processFile(fileInput, outputFile, header, cipher);
        }
    }

    private void processFile(InputStream fileInput, Path outputFile, byte[] header, Cipher cipher)
            throws IOException, GeneralSecurityException {
        Path tempFile = createTempFile(outputFile);
        boolean success = false;
        try (OutputStream fileOutput = new BufferedOutputStream(Files.newOutputStream(
                tempFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING))) {

            if (header != null) {
                fileOutput.write(header);
            }
            transfer(fileInput, fileOutput, cipher);
            success = true;
        } finally {
            if (success) {
                moveIntoPlace(tempFile, outputFile);
            } else {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private void transfer(InputStream inputStream, OutputStream outputStream, Cipher cipher)
            throws IOException, GeneralSecurityException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] encryptedChunk = cipher.update(buffer, 0, bytesRead);
            if (encryptedChunk != null && encryptedChunk.length > 0) {
                outputStream.write(encryptedChunk);
            }
        }

        try {
            byte[] finalChunk = cipher.doFinal();
            if (finalChunk != null && finalChunk.length > 0) {
                outputStream.write(finalChunk);
            }
        } catch (AEADBadTagException exception) {
            throw new AEADBadTagException("The password is incorrect or the encrypted file was modified.");
        }
    }

    private SecretKey deriveKey(char[] password, byte[] salt, int iterations) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(derivedKey, "AES");
            } finally {
                Arrays.fill(derivedKey, (byte) 0);
            }
        } finally {
            spec.clearPassword();
        }
    }

    private byte[] buildHeader(int iterations, byte[] salt, byte[] iv) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(byteStream)) {
            output.write(MAGIC);
            output.writeByte(VERSION);
            output.writeInt(iterations);
            output.writeByte(salt.length);
            output.writeByte(iv.length);
            output.write(salt);
            output.write(iv);
        }
        return byteStream.toByteArray();
    }

    private Header readHeader(InputStream inputStream) throws IOException {
        DataInputStream input = new DataInputStream(inputStream);

        byte[] magic = new byte[MAGIC.length];
        input.readFully(magic);
        if (!Arrays.equals(MAGIC, magic)) {
            throw new IOException("Unsupported encrypted file format.");
        }

        int version = input.readUnsignedByte();
        if (version != VERSION) {
            throw new IOException("Unsupported encrypted file version.");
        }

        int iterations = input.readInt();
        int saltLength = input.readUnsignedByte();
        int ivLength = input.readUnsignedByte();
        if (iterations < 100_000 || iterations > 1_000_000) {
            throw new IOException("Encrypted file header is invalid.");
        }
        if (saltLength != SALT_LENGTH || ivLength != IV_LENGTH) {
            throw new IOException("Encrypted file header is invalid.");
        }

        byte[] salt = new byte[saltLength];
        byte[] iv = new byte[ivLength];
        input.readFully(salt);
        input.readFully(iv);

        return new Header(iterations, salt, iv, buildHeader(iterations, salt, iv));
    }

    private Path createTempFile(Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        String fileName = outputFile.getFileName().toString();
        return Files.createTempFile(parent, fileName + ".", ".tmp");
    }

    private void moveIntoPlace(Path tempFile, Path outputFile) throws IOException {
        try {
            Files.move(tempFile, outputFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private static final class Header {
        private final int iterations;
        private final byte[] salt;
        private final byte[] iv;
        private final byte[] encodedHeader;

        private Header(int iterations, byte[] salt, byte[] iv, byte[] encodedHeader) {
            this.iterations = iterations;
            this.salt = salt;
            this.iv = iv;
            this.encodedHeader = encodedHeader;
        }

        private void clear() {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(encodedHeader, (byte) 0);
        }
    }
}