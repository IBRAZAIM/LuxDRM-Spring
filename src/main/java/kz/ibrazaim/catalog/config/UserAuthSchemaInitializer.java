package kz.ibrazaim.catalog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureUsersConstraints() {
        dropPasswordUniqueConstraints();
        dropPasswordUniqueIndexes();
        ensureLoginUniqueIndex();
    }

    private void dropPasswordUniqueConstraints() {
        String sql = """
                SELECT con.conname
                FROM pg_constraint con
                JOIN pg_class tbl ON tbl.oid = con.conrelid
                JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
                JOIN unnest(con.conkey) AS cols(attnum) ON TRUE
                JOIN pg_attribute att ON att.attrelid = tbl.oid AND att.attnum = cols.attnum
                WHERE ns.nspname = 'public'
                  AND tbl.relname = 'users'
                  AND con.contype = 'u'
                GROUP BY con.conname
                HAVING COUNT(*) = 1 AND MAX(att.attname) = 'password'
                """;

        List<String> constraints = jdbcTemplate.queryForList(sql, String.class);
        for (String constraint : constraints) {
            String dropSql = "ALTER TABLE users DROP CONSTRAINT IF EXISTS " + quoteIdentifier(constraint);
            jdbcTemplate.execute(dropSql);
            log.warn("Dropped incorrect UNIQUE constraint on users.password: {}", constraint);
        }
    }

    private void dropPasswordUniqueIndexes() {
        String sql = """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'users'
                  AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
                  AND indexdef ~* '\\\\(\\\\s*password\\\\s*\\\\)'
                """;

        List<String> indexes = jdbcTemplate.queryForList(sql, String.class);
        for (String index : indexes) {
            String dropSql = "DROP INDEX IF EXISTS public." + quoteIdentifier(index);
            jdbcTemplate.execute(dropSql);
            log.warn("Dropped incorrect UNIQUE index on users.password: {}", index);
        }
    }

    private void ensureLoginUniqueIndex() {
        String duplicatesSql = """
                SELECT lower(login)
                FROM users
                WHERE login IS NOT NULL
                GROUP BY lower(login)
                HAVING COUNT(*) > 1
                """;
        List<String> duplicates = jdbcTemplate.queryForList(duplicatesSql, String.class);

        if (!duplicates.isEmpty()) {
            String sample = duplicates.stream().limit(5).collect(Collectors.joining(", "));
            log.error(
                    "Found duplicate logins in users table (case-insensitive). " +
                            "Unique login index was not created. Duplicates sample: {}",
                    sample
            );
            return;
        }

        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_users_login_ci ON users ((lower(login)))");
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
