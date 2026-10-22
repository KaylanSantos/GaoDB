package br.com.gaozin.gaodb.core;

import br.com.gaozin.gaodb.model.Table;
import br.com.gaozin.gaodb.storage.FileStorage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    private final Map<String, Table> tables = new LinkedHashMap<>();
    private final FileStorage storage;

    public Database() {
        this(Path.of("gaodb-data"));
    }

    public Database(Path dataDirectory) {
        this.storage = new FileStorage(dataDirectory);
    }

    public Table createTable(String name) {
        String key = normalize(name);
        if (tables.containsKey(key)) {
            throw new IllegalArgumentException("A tabela '" + name + "' já está aberta.");
        }
        Table table = new Table(name, storage);
        tables.put(key, table);
        return table;
    }

    public Table getTable(String name) {
        String key = normalize(name);
        Table table = tables.get(key);
        if (table == null) {
            // Tabelas já existentes são carregadas do arquivo sob demanda.
            table = new Table(name, storage);
            tables.put(key, table);
        }
        return table;
    }

    public boolean hasTable(String name) {
        return tables.containsKey(normalize(name));
    }

    public Map<String, Table> getTables() {
        return Map.copyOf(tables);
    }

    private static String normalize(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }
}
