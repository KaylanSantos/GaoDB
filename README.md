# GaoDB

Mini banco de dados desenvolvido do zero em Java, sem banco externo.

## Requisitos
- JDK 17 ou superior
- Maven 3.9+ (opcional para executar pelo VS Code; necessário para os comandos Maven)

## Funcionalidades
- Criação de tabelas
- Inserção, busca por ID, listagem, atualização e remoção
- Persistência em arquivos binários
- Índice em memória por ID, reconstruído ao carregar os dados
- Interface de terminal
- Comandos inspirados em SQL: `CREATE TABLE`, `INSERT INTO`, `SELECT`, `UPDATE`, `DELETE`
- Filtros simples com `WHERE`, operadores `=`, `!=`, `>`, `>=`, `<`, `<=`
- Ordenação com `ORDER BY` e limite com `LIMIT`

## Executar
Na raiz do projeto:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="br.com.gaozin.gaodb.Main"
```

O `pom.xml` não depende de bibliotecas externas, mas o comando `exec:java` usa um plugin do Maven que pode ser baixado na primeira execução. Alternativa: abra `src/main/java/br/com/gaozin/gaodb/Main.java` no VS Code e clique em **Run**.

## Comandos de exemplo
```sql
CREATE TABLE usuarios
INSERT INTO usuarios (nome, idade) VALUES ('Kaylan', 20)
INSERT INTO usuarios (nome, idade) VALUES ('Maria', 22)
SELECT * FROM usuarios
SELECT * FROM usuarios WHERE idade >= 21 ORDER BY nome ASC
UPDATE usuarios SET idade = 21 WHERE id = 1
DELETE FROM usuarios WHERE id = 2
```

Também há comandos de terminal:
- `HELP`
- `TABLES`
- `DESCRIBE usuarios`
- `EXIT`

### Observações
- Os campos são armazenados como texto; comparações numéricas tentam interpretar os valores como números.
- Cada tabela é salva em `gaodb-data/` em formato binário próprio.
- O índice por ID é reconstruído na inicialização. Índices secundários, transações, concorrência, recuperação após falhas e compatibilidade completa com SQL ainda não fazem parte do projeto.
- Este é um projeto educacional, não destinado a dados de produção.
