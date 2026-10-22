package br.com.gaozin.gaodb.storage;

import br.com.gaozin.gaodb.model.Record;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Armazenamento binário simples. Formato educacional, sem garantias de transação.
 */
public class FileStorage {
    private static final int MAGIC = 0x47414F44; // "GAOD"
    private static final int VERSION = 1;
    private static final int MAX_RECORDS = 1_000_000;
    private static final int MAX_FIELDS = 10_000;
    private static final int MAX_STRING_LENGTH = 1_000_000;

    private final Path directory;

    public FileStorage(Path directory) {
        this.directory = directory;
    }

    public void save(String tableName, List<Record> records) throws IOException {
        Files.createDirectories(directory);
        Path destination = fileFor(tableName);
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            writeString(out, tableName);
            out.writeInt(records.size());

            for (Record record : records) {
                out.writeLong(record.getId());
                out.writeInt(record.getFields().size());
                for (Map.Entry<String, String> field : record.getFields().entrySet()) {
                    writeString(out, field.getKey());
                    writeString(out, field.getValue() == null ? "" : field.getValue());
                }
            }
        }

        try {
            Files.move(temporary, destination,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveNotSupported) {
            Files.move(temporary, destination,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public List<Record> load(String expectedTableName) throws IOException {
        Path file = fileFor(expectedTableName);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC) {
                throw new IOException("Arquivo de banco inválido: " + file);
            }
            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Versão de arquivo não suportada: " + version);
            }

            String storedName = readString(in);
            if (!expectedTableName.equals(storedName)) {
                throw new IOException("O nome da tabela no arquivo não corresponde.");
            }

            int count = in.readInt();
            if (count < 0 || count > MAX_RECORDS) {
                throw new IOException("Quantidade de registros inválida: " + count);
            }

            List<Record> records = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                long id = in.readLong();
                Record record = new Record(id);
                int fieldsCount = in.readInt();
                if (fieldsCount < 0 || fieldsCount > MAX_FIELDS) {
                    throw new IOException("Quantidade de campos inválida.");
                }
                for (int j = 0; j < fieldsCount; j++) {
                    record.setField(readString(in), readString(in));
                }
                records.add(record);
            }

            if (in.read() != -1) {
                throw new IOException("O arquivo contém dados extras ou está corrompido.");
            }
            return records;
        } catch (EOFException e) {
            throw new IOException("Arquivo de banco incompleto ou corrompido: " + file, e);
        }
    }

    private Path fileFor(String tableName) {
        // Codificação hexadecimal dos bytes evita nomes de arquivo perigosos.
        byte[] bytes = tableName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder safe = new StringBuilder();
        for (byte b : bytes) {
            safe.append(String.format("%02x", b & 0xff));
        }
        return directory.resolve(safe + ".gaodb");
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_STRING_LENGTH) {
            throw new IOException("Tamanho de texto inválido: " + length);
        }
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new EOFException();
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
