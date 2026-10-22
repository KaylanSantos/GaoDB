package br.com.gaozin.gaodb;

import br.com.gaozin.gaodb.core.Database;
import br.com.gaozin.gaodb.sql.SqlEngine;

import java.util.Scanner;

public class Main {
    private static final String HELP = """
            Comandos disponíveis:
              CREATE TABLE usuarios
              INSERT INTO usuarios (nome, idade) VALUES ('Kaylan', 20)
              SELECT * FROM usuarios
              SELECT * FROM usuarios WHERE idade >= 18
              SELECT * FROM usuarios ORDER BY nome ASC LIMIT 10
              UPDATE usuarios SET idade = 21 WHERE id = 1
              DELETE FROM usuarios WHERE id = 1
              HELP
              TABLES
              EXIT

            Observação: a sintaxe SQL é parcial e educacional.
            """;

    public static void main(String[] args) {
        Database database = new Database();
        SqlEngine engine = new SqlEngine(database);

        System.out.println("=================================");
        System.out.println("       GaoDB - Java Database");
        System.out.println("=================================");
        System.out.println("Digite HELP para ver os comandos.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\ngaodb> ");
                if (!scanner.hasNextLine()) break;
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                if (input.equalsIgnoreCase("EXIT") || input.equalsIgnoreCase("QUIT")) {
                    System.out.println("Encerrando o GaoDB.");
                    break;
                }
                if (input.equalsIgnoreCase("HELP")) {
                    System.out.println(HELP);
                    continue;
                }
                if (input.equalsIgnoreCase("TABLES")) {
                    if (database.getTables().isEmpty()) System.out.println("Nenhuma tabela aberta nesta sessão.");
                    else database.getTables().keySet().forEach(System.out::println);
                    continue;
                }
                try {
                    System.out.println(engine.execute(input));
                } catch (RuntimeException e) {
                    System.out.println("Erro: " + e.getMessage());
                }
            }
        }
    }
}
