package br.com.gaozin.gaodb.model;

import br.com.gaozin.gaodb.storage.FileStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Table {
    private final String name;
    // Índice primário por ID: permite busca média O(1).
    private final Map<Long, Record> records = new LinkedHashMap<>();
    private final FileStorage storage;

    public Table(String name, FileStorage storage) {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Nome de tabela inválido: " + name);
        }
        this.name = name;
        this.storage = storage;
        try {
            for (Record record : storage.load(name)) {
                if (records.putIfAbsent(record.getId(), record) != null) {
                    throw new IOException("ID duplicado encontrado no arquivo: " + record.getId());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar a tabela '" + name + "'.", e);
        }
    }

    public String getName() {
        return name;
    }

    public void insert(Record record) {
        if (records.containsKey(record.getId())) {
            throw new IllegalArgumentException("Já existe um registro com o ID " + record.getId());
        }
        records.put(record.getId(), record);
        persistOrRollback(() -> records.remove(record.getId()));
    }

    public Record findById(long id) {
        return records.get(id);
    }

    public List<Record> getAll() {
        return new ArrayList<>(records.values());
    }

    public boolean update(long id, String field, String value) {
        Record record = records.get(id);
        if (record == null) return false;
        String oldValue = record.getField(field);
        boolean hadField = record.getFields().containsKey(field);
        record.setField(field, value);
        persistOrRollback(() -> {
            if (hadField) record.setField(field, oldValue);
            else record.removeField(field);
        });
        return true;
    }

    public boolean delete(long id) {
        Record removed = records.remove(id);
        if (removed == null) return false;
        persistOrRollback(() -> records.put(id, removed));
        return true;
    }

    public int size() {
        return records.size();
    }

    public List<Record> query(String field, String operator, String expected) {
        List<Record> result = new ArrayList<>();
        for (Record record : records.values()) {
            String actual = field.equalsIgnoreCase("id")
                    ? Long.toString(record.getId())
                    : record.getField(field);
            if (actual != null && compare(actual, operator, expected)) {
                result.add(record);
            }
        }
        return result;
    }

    public List<Record> ordered(List<Record> input, String field, boolean ascending) {
        List<Record> result = new ArrayList<>(input);
        result.sort((a, b) -> {
            String av = field.equalsIgnoreCase("id") ? Long.toString(a.getId()) : a.getField(field);
            String bv = field.equalsIgnoreCase("id") ? Long.toString(b.getId()) : b.getField(field);
            int comparison;
            if (av == null) comparison = bv == null ? 0 : -1;
            else if (bv == null) comparison = 1;
            else comparison = compareValues(av, bv);
            return ascending ? comparison : -comparison;
        });
        return result;
    }

    private static boolean compare(String actual, String operator, String expected) {
        int c = compareValues(actual, expected);
        return switch (operator) {
            case "=" , "==" -> actual.equalsIgnoreCase(expected);
            case "!=", "<>" -> !actual.equalsIgnoreCase(expected);
            case ">" -> c > 0;
            case ">=" -> c >= 0;
            case "<" -> c < 0;
            case "<=" -> c <= 0;
            default -> throw new IllegalArgumentException("Operador não suportado: " + operator);
        };
    }

    private static int compareValues(String a, String b) {
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        } catch (NumberFormatException ignored) {
            return a.toLowerCase(Locale.ROOT).compareTo(b.toLowerCase(Locale.ROOT));
        }
    }

    private void persistOrRollback(Runnable rollback) {
        try {
            storage.save(name, getAll());
        } catch (IOException e) {
            rollback.run();
            throw new IllegalStateException("Falha ao salvar a tabela '" + name + "'.", e);
        }
    }
}
