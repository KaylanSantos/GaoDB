package br.com.gaozin.gaodb.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Record {
    private final long id;
    private final Map<String, String> fields = new LinkedHashMap<>();

    public Record(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setField(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do campo não pode ser vazio.");
        }
        fields.put(name, value);
    }

    public String getField(String name) {
        return fields.get(name);
    }

    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public void removeField(String name) {
        fields.remove(name);
    }

    @Override
    public String toString() {
        return "Record{id=" + id + ", fields=" + fields + "}";
    }
}
