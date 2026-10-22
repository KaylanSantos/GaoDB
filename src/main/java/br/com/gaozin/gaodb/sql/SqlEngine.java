package br.com.gaozin.gaodb.sql;

import br.com.gaozin.gaodb.core.Database;
import br.com.gaozin.gaodb.model.Record;
import br.com.gaozin.gaodb.model.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlEngine {
    private final Database database;

    public SqlEngine(Database database) {
        this.database = database;
    }

    public String execute(String sql) {
        String command = sql.trim();
        if (command.isEmpty()) return "";

        Matcher create = Pattern.compile("(?i)^CREATE\\s+TABLE\\s+([A-Za-z_][A-Za-z0-9_]*)$").matcher(command);
        if (create.matches()) {
            Table table = database.createTable(create.group(1));
            return "Tabela '" + table.getName() + "' criada/carregada.";
        }

        Matcher insert = Pattern.compile(
                "(?is)^INSERT\\s+INTO\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*?)\\)\\s*VALUES\\s*\\((.*?)\\)$"
        ).matcher(command);
        if (insert.matches()) {
            String tableName = insert.group(1);
            List<String> columns = splitCsv(insert.group(2));
            List<String> values = splitCsv(insert.group(3));
            if (columns.size() != values.size()) {
                throw new IllegalArgumentException("A quantidade de colunas e valores precisa ser igual.");
            }
            Table table = database.getTable(tableName);
            long id = nextId(table);
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).trim().equalsIgnoreCase("id")) {
                    id = Long.parseLong(unquote(values.get(i).trim()));
                    break;
                }
            }
            Record record = new Record(id);
            for (int i = 0; i < columns.size(); i++) {
                String column = columns.get(i).trim();
                if (!column.equalsIgnoreCase("id")) {
                    record.setField(column, unquote(values.get(i).trim()));
                }
            }
            table.insert(record);
            return "Registro inserido. ID = " + record.getId();
        }

        Matcher select = Pattern.compile(
                "(?is)^SELECT\\s+\\*\\s+FROM\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s+WHERE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(>=|<=|!=|<>|=|==|>|<)\\s*(.+?))?(?:\\s+ORDER\\s+BY\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s+(ASC|DESC))?)?(?:\\s+LIMIT\\s+(\\d+))?$"
        ).matcher(command);
        if (select.matches()) {
            Table table = database.getTable(select.group(1));
            List<Record> rows;
            if (select.group(2) != null) {
                rows = table.query(select.group(2), select.group(3), unquote(select.group(4).trim()));
            } else {
                rows = table.getAll();
            }
            if (select.group(5) != null) {
                rows = table.ordered(rows, select.group(5),
                        !"DESC".equalsIgnoreCase(select.group(6)));
            }
            if (select.group(7) != null) {
                rows = new ArrayList<>(rows.subList(0, Math.min(rows.size(), Integer.parseInt(select.group(7)))));
            }
            if (rows.isEmpty()) return "(0 registros)";
            StringBuilder out = new StringBuilder();
            for (Record row : rows) out.append(row).append(System.lineSeparator());
            return out.toString().stripTrailing();
        }

        Matcher update = Pattern.compile(
                "(?is)^UPDATE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+SET\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.+?)\\s+WHERE\\s+id\\s*=\\s*(\\d+)$"
        ).matcher(command);
        if (update.matches()) {
            boolean changed = database.getTable(update.group(1)).update(
                    Long.parseLong(update.group(4)), update.group(2), unquote(update.group(3).trim()));
            return changed ? "Registro atualizado." : "Nenhum registro encontrado.";
        }

        Matcher delete = Pattern.compile(
                "(?i)^DELETE\\s+FROM\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+WHERE\\s+id\\s*=\\s*(\\d+)$"
        ).matcher(command);
        if (delete.matches()) {
            boolean deleted = database.getTable(delete.group(1)).delete(Long.parseLong(delete.group(2)));
            return deleted ? "Registro removido." : "Nenhum registro encontrado.";
        }

        throw new IllegalArgumentException("Comando não reconhecido. Digite HELP para ver exemplos.");
    }

    private static long nextId(Table table) {
        long max = 0;
        for (Record record : table.getAll()) max = Math.max(max, record.getId());
        return max + 1;
    }

    private static List<String> splitCsv(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '\'' || c == '"') && (!quoted || c == quote)) {
                if (quoted) quoted = false;
                else { quoted = true; quote = c; }
                current.append(c);
            } else if (c == ',' && !quoted) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted) throw new IllegalArgumentException("Texto entre aspas não foi fechado.");
        if (!current.toString().isBlank()) parts.add(current.toString().trim());
        return parts;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return value.substring(1, value.length() - 1).replace("" + first + first, "" + first);
            }
        }
        return value;
    }
}
