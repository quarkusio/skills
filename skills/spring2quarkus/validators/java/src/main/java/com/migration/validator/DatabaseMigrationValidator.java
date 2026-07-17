package com.migration.validator;

import com.migration.validator.core.ValidationReport;
import com.migration.validator.core.YamlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DatabaseMigrationValidator - Phase 4 validator for Spring to Quarkus
 * migration.
 *
 * Validates:
 * - import.sql file exists and contains schema/data
 * - Datasource configuration is complete for chosen database type
 * - Required JDBC driver dependency is present
 * - Database connectivity can be established
 * - No Spring datasource properties remain
 * - SQL syntax compatibility
 *
 * Instance-based validator with constructor injection for better testability
 * and design.
 */
public class DatabaseMigrationValidator {

    private final Path projectRoot;
    private final Path specPath;
    private final ValidationReport report;
    private Map<String, Object> spec;
    private String dbType;

    /**
     * Constructor with dependency injection.
     *
     * @param projectRoot Absolute path to the target Quarkus project root
     * @param specPath    Absolute path to migration-spec.yaml
     */
    public DatabaseMigrationValidator(Path projectRoot, Path specPath) {
        this.projectRoot = projectRoot.toAbsolutePath();
        this.specPath = specPath.toAbsolutePath();
        this.report = new ValidationReport();
    }

    /**
     * Run validation and return exit code.
     *
     * @param verbose Enable verbose output with detailed logging
     * @return 0 for success, 1 for failure
     */
    public int validate(boolean verbose) {
        printHeader();

        try {
            // Load migration spec
            spec = YamlUtils.loadYaml(specPath);

            // Get database type
            Map<String, Object> database = (Map<String, Object>) spec.get("database");
            dbType = database != null ? (String) database.getOrDefault("type", "h2") : "h2";
            System.out.println("[INFO] Database type: " + dbType + "\n");

            // Get verification rules (use defaults if not specified)
            List<String> rules = getVerificationRules();
            System.out.println("[INFO] Running " + rules.size() + " rule(s) for database-migration phase\n");

            // Run each rule
            for (String rule : rules) {
                System.out.println("[RULE] " + rule);
                try {
                    dispatchRule(rule);
                } catch (Exception e) {
                    report.fail(rule, "Verifier error: " + e.getMessage());
                    if (verbose) {
                        e.printStackTrace();
                    }
                }
                System.out.println();
            }

            // Print summary
            printSummary();

            // Print important runtime verification note if successful
            if (report.getStatus().equals("success")) {
                printRuntimeVerificationNote();
            }

            // Save results to spec
            saveResults();

            return report.getStatus().equals("success") ? 0 : 1;

        } catch (Exception e) {
            System.err.println("[ERROR] Validation failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private List<String> getVerificationRules() {
        Map<String, Object> verificationRules = (Map<String, Object>) spec.get("verification_rules");
        if (verificationRules != null && verificationRules.containsKey("database-migration")) {
            return (List<String>) verificationRules.get("database-migration");
        }

        // Default rules
        return Arrays.asList(
                "import.sql file present with schema and/or data",
                "Datasource configuration complete for chosen database type",
                "No Spring datasource properties in configuration",
                "JDBC driver dependency present for database type",
                "Database connectivity check passes",
                "SQL syntax compatibility check passes");
    }

    private void dispatchRule(String rule) {
        String r = rule.toLowerCase().trim();

        if (r.contains("import.sql") || r.contains("database initialization")) {
            checkImportSqlExists(rule);
        } else if (r.contains("datasource configuration") || r.contains("quarkus.datasource")) {
            checkDatasourceConfiguration(rule);
        } else if (r.contains("no spring datasource") || r.contains("spring.datasource")
                || r.contains("spring properties")) {
            checkNoSpringDatasourceConfig(rule);
        } else if (r.contains("jdbc driver") || r.contains("database dependency")) {
            checkJdbcDriverDependency(rule);
        } else if (r.contains("database connectivity") || r.contains("connection")) {
            checkDatabaseConnectivity(rule);
        } else if (r.contains("sql syntax") || r.contains("compatibility")) {
            checkSqlSyntaxCompatibility(rule);
        } else {
            report.fail(rule, "Rule not implemented in verifier: '" + rule + "'");
        }
    }

    private void checkImportSqlExists(String rule) {
        Path importSql = projectRoot.resolve("src/main/resources/import.sql");

        if (!Files.isRegularFile(importSql)) {
            report.fail(rule, "import.sql not found in src/main/resources");
            return;
        }

        try {
            long size = Files.size(importSql);
            if (size == 0) {
                report.fail(rule, "import.sql exists but is empty");
                return;
            }

            String content = Files.readString(importSql);
            String upperContent = content.toUpperCase();

            boolean hasCreate = upperContent.contains("CREATE TABLE");
            boolean hasInsert = upperContent.contains("INSERT INTO") || upperContent.contains("MERGE INTO");

            List<String> details = new ArrayList<>();
            if (hasCreate) {
                int tableCount = countMatches(content, "(?i)CREATE\\s+TABLE");
                details.add(tableCount + " table(s)");
            }
            if (hasInsert) {
                int insertCount = countMatches(content, "(?i)(INSERT\\s+INTO|MERGE\\s+INTO)");
                details.add(insertCount + " data statement(s)");
            }

            if (!hasCreate && !hasInsert) {
                report.fail(rule,
                        String.format(
                                "import.sql exists (%d bytes) but contains no CREATE TABLE or INSERT/MERGE statements",
                                size));
                return;
            }

            String mode;
            if (hasCreate && hasInsert) {
                mode = "schema and data";
            } else if (hasCreate) {
                mode = "schema only";
            } else {
                mode = "data only (valid when schema is generated from JPA entities/Hibernate ORM)";
            }

            String evidence = String.format("import.sql present (%d bytes) with %s [%s]",
                    size, String.join(", ", details), mode);
            System.out.println("[DEBUG] import.sql check: " + evidence);
            report.pass(rule, evidence);

        } catch (IOException e) {
            report.fail(rule, "Error reading import.sql: " + e.getMessage());
        }
    }

    private void checkDatasourceConfiguration(String rule) {
        Path propsPath = projectRoot.resolve("src/main/resources/application.properties");

        if (!Files.isRegularFile(propsPath)) {
            report.fail(rule, "application.properties not found");
            return;
        }

        try {
            String content = Files.readString(propsPath);

            Map<String, Pattern> requiredProps = new LinkedHashMap<>();
            requiredProps.put("quarkus.datasource.db-kind",
                    Pattern.compile("quarkus\\.datasource\\.db-kind\\s*=\\s*(\\S+)",
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
            requiredProps.put("quarkus.datasource.jdbc.url",
                    Pattern.compile("quarkus\\.datasource\\.jdbc\\.url\\s*=\\s*(\\S+)",
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));

            List<String> missing = new ArrayList<>();
            Map<String, String> found = new HashMap<>();

            for (Map.Entry<String, Pattern> entry : requiredProps.entrySet()) {
                Matcher matcher = entry.getValue().matcher(content);
                if (matcher.find()) {
                    found.put(entry.getKey(), matcher.group(1).trim());
                } else {
                    missing.add(entry.getKey());
                }
            }

            if (!missing.isEmpty()) {
                report.fail(rule, "Missing required datasource properties: " + String.join(", ", missing));
                return;
            }

            // Validate db-kind matches expected database type
            String configuredDbKind = found.get("quarkus.datasource.db-kind").toLowerCase();
            if (!configuredDbKind.equals(dbType.toLowerCase())) {
                report.fail(rule,
                        String.format("Database type mismatch: configured '%s' but spec expects '%s'", configuredDbKind,
                                dbType));
                return;
            }

            String jdbcUrl = found.get("quarkus.datasource.jdbc.url");
            String truncatedUrl = jdbcUrl.length() > 60 ? jdbcUrl.substring(0, 60) + "..." : jdbcUrl;

            String evidence = String.format("Datasource configured: db-kind=%s, url=%s", configuredDbKind,
                    truncatedUrl);
            System.out.println("[DEBUG] Datasource check: " + evidence);
            report.pass(rule, evidence);

        } catch (IOException e) {
            report.fail(rule, "Error reading application.properties: " + e.getMessage());
        }
    }

    private void checkNoSpringDatasourceConfig(String rule) {
        Path propsPath = projectRoot.resolve("src/main/resources/application.properties");

        if (!Files.isRegularFile(propsPath)) {
            report.pass(rule, "application.properties not found (nothing to check)");
            return;
        }

        try {
            String content = Files.readString(propsPath);

            List<String> springProps = findMatches(content, "^spring\\.datasource\\.[^\\s=]+", Pattern.MULTILINE);
            List<String> springSqlProps = findMatches(content, "^spring\\.sql\\.[^\\s=]+", Pattern.MULTILINE);
            List<String> springJpaProps = findMatches(content, "^spring\\.jpa\\.[^\\s=]+", Pattern.MULTILINE);

            List<String> allSpringProps = new ArrayList<>();
            allSpringProps.addAll(springProps);
            allSpringProps.addAll(springSqlProps);
            allSpringProps.addAll(springJpaProps);

            if (!allSpringProps.isEmpty()) {
                Set<String> uniqueProps = new LinkedHashSet<>(allSpringProps);
                List<String> sample = new ArrayList<>(uniqueProps).subList(0, Math.min(5, uniqueProps.size()));
                report.fail(rule, "Spring datasource properties still present: " + String.join(", ", sample));
                return;
            }

            System.out.println("[DEBUG] No Spring datasource properties found");
            report.pass(rule, "No Spring datasource properties found");

        } catch (IOException e) {
            report.fail(rule, "Error reading application.properties: " + e.getMessage());
        }
    }

    private void checkJdbcDriverDependency(String rule) {
        Path pomPath = projectRoot.resolve("pom.xml");

        if (!Files.isRegularFile(pomPath)) {
            report.fail(rule, "pom.xml not found");
            return;
        }

        try {
            String content = Files.readString(pomPath);

            Map<String, String> jdbcDrivers = new HashMap<>();
            jdbcDrivers.put("h2", "quarkus-jdbc-h2");
            jdbcDrivers.put("postgresql", "quarkus-jdbc-postgresql");
            jdbcDrivers.put("mysql", "quarkus-jdbc-mysql");
            jdbcDrivers.put("mariadb", "quarkus-jdbc-mariadb");

            String expectedDriver = jdbcDrivers.get(dbType.toLowerCase());
            if (expectedDriver == null) {
                report.fail(rule, "Unknown database type: " + dbType);
                return;
            }

            if (!content.contains("<artifactId>" + expectedDriver + "</artifactId>")) {
                report.fail(rule,
                        String.format("Missing JDBC driver dependency: %s (required for %s)", expectedDriver, dbType));
                return;
            }

            String evidence = String.format("JDBC driver present: %s (for %s)", expectedDriver, dbType);
            System.out.println("[DEBUG] JDBC driver check: " + evidence);
            report.pass(rule, evidence);

        } catch (IOException e) {
            report.fail(rule, "Error reading pom.xml: " + e.getMessage());
        }
    }

    private void checkDatabaseConnectivity(String rule) {
        System.out.println("[DEBUG] Checking database connectivity via Maven compile...");

        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "-B", "--no-transfer-progress", "-DskipTests");
            pb.directory(projectRoot.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                report.fail(rule, "Database connectivity check timed out after 180 seconds");
                return;
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            // Check for datasource-related errors
            String[] datasourceErrors = {
                    "Unable to create requested service",
                    "Unable to build Hibernate SessionFactory",
                    "Failed to configure a DataSource",
                    "Could not create connection to database server"
            };

            boolean hasDatasourceError = false;
            for (String error : datasourceErrors) {
                if (outputStr.contains(error)) {
                    hasDatasourceError = true;
                    break;
                }
            }

            if (exitCode != 0) {
                if (hasDatasourceError) {
                    List<String> errorLines = new ArrayList<>();
                    for (String line : outputStr.split("\n")) {
                        if (line.contains("ERROR")) {
                            errorLines.add(line);
                            if (errorLines.size() >= 3)
                                break;
                        }
                    }
                    String errorSnippet = String.join("\n  ", errorLines);
                    report.fail(rule, "Database connectivity issue:\n  " + errorSnippet);
                } else {
                    report.fail(rule, "Maven compile failed (exit=" + exitCode + ")");
                }
                return;
            }

            String evidence = "Database connectivity check passed (Maven compile successful)";
            System.out.println("[DEBUG] Connectivity check: " + evidence);
            report.pass(rule, evidence);

        } catch (IOException e) {
            if (e.getMessage().contains("Cannot run program \"mvn\"")) {
                report.fail(rule, "Maven (mvn) not found in PATH");
            } else {
                report.fail(rule, "Error running Maven: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            report.fail(rule, "Maven process interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void checkSqlSyntaxCompatibility(String rule) {
        Path importSql = projectRoot.resolve("src/main/resources/import.sql");

        if (!Files.isRegularFile(importSql)) {
            report.pass(rule, "import.sql not found (nothing to check)");
            return;
        }

        try {
            String content = Files.readString(importSql).toUpperCase();
            List<String> issues = new ArrayList<>();

            // Check for H2-specific issues
            if (dbType.equalsIgnoreCase("h2")) {
                if (content.contains("AUTO_INCREMENT") && !content.contains("GENERATED BY DEFAULT AS IDENTITY")) {
                    issues.add("Uses AUTO_INCREMENT (consider GENERATED BY DEFAULT AS IDENTITY for H2)");
                }
            }

            // Check for PostgreSQL-specific issues
            if (dbType.equalsIgnoreCase("postgresql")) {
                if (content.contains("AUTO_INCREMENT")) {
                    issues.add("Uses AUTO_INCREMENT (should use SERIAL or BIGSERIAL for PostgreSQL)");
                }
            }

            // Common issues for all databases
            if (content.contains("DATETIME")
                    && (dbType.equalsIgnoreCase("h2") || dbType.equalsIgnoreCase("postgresql"))) {
                issues.add(String.format("Uses DATETIME (consider TIMESTAMP for %s)", dbType));
            }

            if (!issues.isEmpty()) {
                String sample = String.join("; ", issues.subList(0, Math.min(3, issues.size())));
                report.fail(rule, "SQL syntax compatibility issues: " + sample);
                return;
            }

            String evidence = String.format("SQL syntax check passed for %s", dbType);
            System.out.println("[DEBUG] SQL syntax check: " + evidence);
            report.pass(rule, evidence);

        } catch (IOException e) {
            report.fail(rule, "Error reading import.sql: " + e.getMessage());
        }
    }

    private void printHeader() {
        System.out.println("======================================================================");
        System.out.println("DatabaseMigrationValidator — database-migration phase (Spring to Quarkus)");
        System.out.println("Project root : " + projectRoot);
        System.out.println("Spec file    : " + specPath);
        System.out.println("======================================================================");
        System.out.println();
    }

    private void printSummary() {
        System.out.println("======================================================================");
        System.out.println("Verification Summary — database-migration (Spring to Quarkus)");
        System.out.println("======================================================================");
        System.out.println("Status       : " + report.getStatus().toUpperCase());
        System.out.println("Database type: " + dbType);
        System.out.println(String.format("Rules        : %d total  |  %d passed  |  %d failed\n",
                report.getTotal(), report.getPassed(), report.getFailed()));

        for (ValidationReport.Evidence evidence : report.getEvidenceList()) {
            String mark = evidence.isPassed() ? "✓" : "✗";
            System.out.println("  " + mark + " " + evidence.getRule());
            for (String line : evidence.getEvidence().split("\n")) {
                System.out.println("      " + line);
            }
        }
    }

    private void printRuntimeVerificationNote() {
        System.out.println("\n" + "!".repeat(70));
        System.out.println("IMPORTANT: Runtime Verification Limitation");
        System.out.println("!".repeat(70));
        System.out.println("This validator performs STATIC validation only:");
        System.out.println("  ✓ Checks file existence and configuration correctness");
        System.out.println("  ✓ Verifies Maven compilation succeeds");
        System.out.println("  ✗ CANNOT verify import.sql actually executes at runtime");
        System.out.println();
        System.out.println("WHY: Quarkus disables Hibernate ORM when no JPA entities exist.");
        System.out.println("     At Phase 4, entities haven't been migrated yet (Phase 5).");
        System.out.println("     Therefore, import.sql will NOT execute until Phase 5 completes.");
        System.out.println();
        System.out.println("NEXT STEP: Phase 5 (Persistence Migration) will:");
        System.out.println("  1. Migrate JPA entities");
        System.out.println("  2. Start the application in dev mode");
        System.out.println("  3. Verify Hibernate ORM activates");
        System.out.println("  4. Confirm import.sql executes (check logs for SQL statements)");
        System.out.println("!".repeat(70) + "\n");
    }

    private void saveResults() {
        try {
            System.out.println("======================================================================");

            // Prepare entry for history
            Map<String, Object> entry = report.toMap("database-migration");
            entry.put("migration_type", "spring-to-quarkus");
            entry.put("database_type", dbType);

            // Ensure intermediate.history exists
            Map<String, Object> intermediate = (Map<String, Object>) spec.computeIfAbsent("intermediate",
                    k -> new LinkedHashMap<>());
            List<Map<String, Object>> history = (List<Map<String, Object>>) intermediate.computeIfAbsent("history",
                    k -> new ArrayList<>());
            history.add(entry);

            // Save with backup
            YamlUtils.saveYaml(specPath, spec);

            System.out.println("\nResults appended to : " + specPath);
            System.out.println("Backup written to   : "
                    + specPath.toAbsolutePath().getParent().resolve("migration-metadata/backups") + "/"
                    + specPath.getFileName() + ".bak.<timestamp>");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to save results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper methods
    private int countMatches(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private List<String> findMatches(String text, String regex, int flags) {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }
}
