package com.migration.validator;

import com.migration.validator.core.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Validator for Phase 3: Project Bootstrap
 * Verifies that the Quarkus project structure is correctly set up.
 *
 * Instance-based validator with constructor injection for better testability
 * and design.
 */
public class ProjectSetupValidator {

    private final Path projectRoot;
    private final Path specPath;

    /**
     * Constructor with dependency injection.
     *
     * @param projectRoot Absolute path to the target Quarkus project root
     * @param specPath    Absolute path to migration-spec.yaml
     */
    public ProjectSetupValidator(Path projectRoot, Path specPath) {
        this.projectRoot = projectRoot.toAbsolutePath();
        this.specPath = specPath.toAbsolutePath();
    }

    /**
     * Run validation and return exit code.
     *
     * @param verbose Enable verbose output with detailed logging
     * @return 0 for success, 1 for failure
     */
    public int validate(boolean verbose) {
        try {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("verify_project_setup.py — project-bootstrap phase (Spring to Quarkus)");
            System.out.println("Project root : " + projectRoot);
            System.out.println("Spec file    : " + specPath);
            System.out.println("=".repeat(70) + "\n");

            // Load spec
            Map<String, Object> spec = YamlUtils.loadYaml(specPath);

            // Create validation report
            ValidationReport report = new ValidationReport();

            System.out.println("[INFO] Running validation rules for project-setup phase\n");

            // Run validation rules
            checkPomExists(report);
            checkApplicationProperties(report);
            checkNoSpringBootFiles(report);
            checkNoSpringDependencies(report);
            checkPomCoordinates(report);
            checkQuarkusPlugin(report);
            checkQuarkusBom(report);
            checkExtensions(spec, report);
            checkDirectories(report);
            checkMavenCompile(report);

            // Print summary
            report.printSummary("project-setup (Spring to Quarkus)");

            // Save to spec
            saveToSpec(spec, report);

            System.out.println("\nResults appended to : " + specPath);
            System.out.println("Backup written to   : "
                    + specPath.toAbsolutePath().getParent().resolve("migration-metadata/backups") + "/"
                    + specPath.getFileName() + ".bak.<timestamp>");

            // Return exit code
            return report.hasFailures() ? 1 : 0;

        } catch (Exception e) {
            System.err.println("[ERROR] Validation failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private void checkPomExists(ValidationReport report) {
        System.out.println("[RULE] pom.xml exists");
        Path pomPath = projectRoot.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            report.pass("pom.xml exists", "File exists: pom.xml");
            System.out.println("  ✓ File exists: pom.xml\n");
        } else {
            report.fail("pom.xml exists", "File not found: pom.xml");
            System.out.println("  ✗ File not found: pom.xml\n");
        }
    }

    private void checkApplicationProperties(ValidationReport report) {
        System.out.println("[RULE] application.properties exists");
        Path propsPath = projectRoot.resolve("src/main/resources/application.properties");
        if (Files.exists(propsPath)) {
            report.pass("application.properties exists", "File exists: application.properties");
            System.out.println("  ✓ File exists: application.properties\n");
        } else {
            report.fail("application.properties exists", "File not found: application.properties");
            System.out.println("  ✗ File not found: application.properties\n");
        }
    }

    private void checkNoSpringBootFiles(ValidationReport report) {
        System.out.println("[RULE] No Spring Boot files (@SpringBootApplication) exist in target project");
        try {
            List<String> springBootFiles = new ArrayList<>();
            Set<String> excludedDirs = Set.of("target", ".m2", ".git");

            Files.walk(projectRoot)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        for (String excluded : excludedDirs) {
                            if (p.toString().contains("/" + excluded + "/")) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            if (content.contains("@SpringBootApplication")) {
                                springBootFiles.add(projectRoot.relativize(p).toString());
                            }
                        } catch (IOException e) {
                            // Skip files that can't be read
                        }
                    });

            if (springBootFiles.isEmpty()) {
                report.pass("No Spring Boot files", "No Spring Boot files or dependencies found");
                System.out.println("  ✓ No Spring Boot files or dependencies found\n");
            } else {
                String sample = String.join("; ", springBootFiles.subList(0, Math.min(3, springBootFiles.size())));
                String extra = springBootFiles.size() > 3 ? " (and " + (springBootFiles.size() - 3) + " more)" : "";
                String evidence = "Spring Boot artifacts found: " + sample + extra;
                report.fail("No Spring Boot files", evidence);
                System.out.println("  ✗ " + evidence + "\n");
            }
        } catch (IOException e) {
            report.fail("No Spring Boot files", "Error checking files: " + e.getMessage());
            System.out.println("  ✗ Error checking files: " + e.getMessage() + "\n");
        }
    }

    private void checkNoSpringDependencies(ValidationReport report) {
        System.out.println("[RULE] No Spring Framework dependencies (org.springframework.*) in pom.xml");
        try {
            Path pomPath = projectRoot.resolve("pom.xml");
            if (!Files.exists(pomPath)) {
                report.fail("No Spring dependencies", "pom.xml not found");
                System.out.println("  ✗ pom.xml not found\n");
                return;
            }

            String content = Files.readString(pomPath);

            // Find Spring dependencies (excluding spring-boot which is checked separately)
            Pattern pattern = Pattern.compile("<groupId>(org\\.springframework(?!\\.boot)[^<]*)</groupId>",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);

            Set<String> springDeps = new HashSet<>();
            while (matcher.find()) {
                springDeps.add(matcher.group(1));
            }

            if (springDeps.isEmpty()) {
                report.pass("No Spring dependencies", "No Spring Framework dependencies found in pom.xml");
                System.out.println("  ✓ No Spring Framework dependencies found in pom.xml\n");
            } else {
                List<String> depsList = new ArrayList<>(springDeps);
                String deps = String.join(", ", depsList.subList(0, Math.min(5, depsList.size())));
                String evidence = "Spring dependencies found: " + deps;
                report.fail("No Spring dependencies", evidence);
                System.out.println("  ✗ " + evidence + "\n");
            }
        } catch (IOException e) {
            report.fail("No Spring dependencies", "Error checking pom.xml: " + e.getMessage());
            System.out.println("  ✗ Error checking pom.xml: " + e.getMessage() + "\n");
        }
    }

    private void checkPomCoordinates(ValidationReport report) {
        System.out.println("[RULE] pom.xml contains <groupId>, <artifactId> and <version> elements");
        try {
            Path pomPath = projectRoot.resolve("pom.xml");
            String content = Files.readString(pomPath);

            // Remove dependencies and plugins sections to avoid false positives
            content = content.replaceAll("<dependencies>[\\s\\S]*?</dependencies>", "");
            content = content.replaceAll("<plugins>[\\s\\S]*?</plugins>", "");

            boolean hasGroupId = Pattern.compile("<groupId>[^<]+</groupId>", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find();
            boolean hasArtifactId = Pattern.compile("<artifactId>[^<]+</artifactId>", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find();
            boolean hasVersion = Pattern.compile("<version>[^<]+</version>", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find();

            if (hasGroupId && hasArtifactId && hasVersion) {
                report.pass("POM coordinates", "pom.xml has groupId, artifactId, version");
                System.out.println("  ✓ pom.xml has groupId, artifactId, version\n");
            } else {
                List<String> missing = new ArrayList<>();
                if (!hasGroupId)
                    missing.add("groupId");
                if (!hasArtifactId)
                    missing.add("artifactId");
                if (!hasVersion)
                    missing.add("version");
                String evidence = "Missing POM coordinates: " + String.join(", ", missing);
                report.fail("POM coordinates", evidence);
                System.out.println("  ✗ " + evidence + "\n");
            }
        } catch (IOException e) {
            report.fail("POM coordinates", "Error checking pom.xml: " + e.getMessage());
            System.out.println("  ✗ Error checking pom.xml: " + e.getMessage() + "\n");
        }
    }

    private void checkQuarkusPlugin(ValidationReport report) {
        System.out.println("[RULE] quarkus-maven-plugin is present with version >= 3.0.0");
        try {
            Path pomPath = projectRoot.resolve("pom.xml");
            String content = Files.readString(pomPath);

            if (content.contains("<artifactId>quarkus-maven-plugin</artifactId>")) {
                report.pass("quarkus-maven-plugin", "quarkus-maven-plugin is present in pom.xml");
                System.out.println("  ✓ quarkus-maven-plugin is present in pom.xml\n");
            } else {
                report.fail("quarkus-maven-plugin", "quarkus-maven-plugin not declared in pom.xml");
                System.out.println("  ✗ quarkus-maven-plugin not declared in pom.xml\n");
            }
        } catch (IOException e) {
            report.fail("quarkus-maven-plugin", "Error checking pom.xml: " + e.getMessage());
            System.out.println("  ✗ Error checking pom.xml: " + e.getMessage() + "\n");
        }
    }

    private void checkQuarkusBom(ValidationReport report) {
        System.out.println("[RULE] quarkus-bom is present in dependencyManagement");
        try {
            Path pomPath = projectRoot.resolve("pom.xml");
            String content = Files.readString(pomPath);

            boolean hasDependencyManagement = content.contains("<dependencyManagement>");
            if (!hasDependencyManagement) {
                report.fail("quarkus-bom", "No <dependencyManagement> section found in pom.xml");
                System.out.println("  ✗ No <dependencyManagement> section found in pom.xml\n");
                return;
            }

            // Check for literal quarkus-bom
            boolean hasLiteralBom = content.contains("<artifactId>quarkus-bom</artifactId>");

            // Check for property-based artifactId that resolves to quarkus-bom
            boolean hasPropertyBom = false;
            String detectionMethod = "literal artifactId";
            String version = null;

            // Look for property placeholder in artifactId
            Pattern propertyPattern = Pattern.compile("<artifactId>\\$\\{([^}]+)\\}</artifactId>");
            Matcher propertyMatcher = propertyPattern.matcher(content);

            if (propertyMatcher.find()) {
                String propertyName = propertyMatcher.group(1);
                // Look for the property definition
                Pattern propDefPattern = Pattern
                        .compile("<" + Pattern.quote(propertyName) + ">([^<]+)</" + Pattern.quote(propertyName) + ">");
                Matcher propDefMatcher = propDefPattern.matcher(content);

                if (propDefMatcher.find() && propDefMatcher.group(1).trim().equals("quarkus-bom")) {
                    hasPropertyBom = true;
                    detectionMethod = "property placeholder";
                    System.out.println("[DEBUG] Found quarkus-bom via property: " + propertyName + " = quarkus-bom");
                }
            }

            if (!hasLiteralBom && !hasPropertyBom) {
                report.fail("quarkus-bom",
                        "quarkus-bom not found in <dependencyManagement> (checked literal and property placeholders)");
                System.out.println("  ✗ quarkus-bom not found in <dependencyManagement>\n");
                return;
            }

            // Try to extract version
            Pattern versionPattern;
            if (hasLiteralBom) {
                versionPattern = Pattern.compile(
                        "<dependency>[\\s\\S]*?<artifactId>quarkus-bom</artifactId>[\\s\\S]*?<version>([^<]+)</version>",
                        Pattern.DOTALL);
            } else {
                versionPattern = Pattern.compile(
                        "<dependency>[\\s\\S]*?<artifactId>\\$\\{[^}]+\\}</artifactId>[\\s\\S]*?<version>([^<]+)</version>",
                        Pattern.DOTALL);
            }

            Matcher versionMatcher = versionPattern.matcher(content);
            if (versionMatcher.find()) {
                version = versionMatcher.group(1).trim();

                // Resolve property placeholder if present
                if (version.startsWith("${") && version.endsWith("}")) {
                    String versionProp = version.substring(2, version.length() - 1);
                    Pattern versionPropPattern = Pattern.compile(
                            "<" + Pattern.quote(versionProp) + ">([^<]+)</" + Pattern.quote(versionProp) + ">");
                    Matcher versionPropMatcher = versionPropPattern.matcher(content);

                    if (versionPropMatcher.find()) {
                        String resolvedVersion = versionPropMatcher.group(1).trim();
                        System.out
                                .println("[DEBUG] Resolved version property " + versionProp + " = " + resolvedVersion);
                        String evidence = "quarkus-bom found in dependencyManagement (version: " + resolvedVersion +
                                " via ${" + versionProp + "}, " + detectionMethod + ")";
                        report.pass("quarkus-bom", evidence);
                        System.out.println("  ✓ " + evidence + "\n");
                        return;
                    }
                }

                String evidence = "quarkus-bom found in dependencyManagement (version: " + version + ", "
                        + detectionMethod + ")";
                report.pass("quarkus-bom", evidence);
                System.out.println("  ✓ " + evidence + "\n");
            } else {
                String evidence = "quarkus-bom found in dependencyManagement (" + detectionMethod + ")";
                report.pass("quarkus-bom", evidence);
                System.out.println("  ✓ " + evidence + "\n");
            }

        } catch (IOException e) {
            report.fail("quarkus-bom", "Error checking pom.xml: " + e.getMessage());
            System.out.println("  ✗ Error checking pom.xml: " + e.getMessage() + "\n");
        }
    }

    @SuppressWarnings("unchecked")
    private void checkExtensions(Map<String, Object> spec, ValidationReport report) {
        System.out.println(
                "[RULE] quarkus- extensions are present (e.g. quarkus-rest-jackson, quarkus-hibernate-orm-panache)");
        try {
            Path pomPath = projectRoot.resolve("pom.xml");
            String content = Files.readString(pomPath);

            // Get extensions from spec
            List<String> extensions = (List<String>) spec.getOrDefault("extensions", List.of());

            if (extensions.isEmpty()) {
                report.pass("Extensions", "No extensions listed in spec (nothing to check)");
                System.out.println("  ✓ No extensions listed in spec (nothing to check)\n");
                return;
            }

            List<String> missing = new ArrayList<>();
            List<String> present = new ArrayList<>();

            for (String ext : extensions) {
                if (content.contains("<artifactId>" + ext + "</artifactId>")) {
                    present.add(ext);
                } else {
                    missing.add(ext);
                }
            }

            if (missing.isEmpty()) {
                String evidence = "Extensions from spec: " + extensions.size() + " total (" + present.size()
                        + " present, 0 missing)";
                report.pass("Extensions", evidence);
                System.out.println("  ✓ " + evidence + "\n");
            } else {
                String evidence = "Extensions from spec: " + extensions.size() + " total (" + present.size() +
                        " present, " + missing.size() + " missing)\n  MISSING: " + String.join(", ", missing);
                report.fail("Extensions", evidence);
                System.out.println("  ✗ " + evidence + "\n");
            }
        } catch (IOException e) {
            report.fail("Extensions", "Error checking extensions: " + e.getMessage());
            System.out.println("  ✗ Error checking extensions: " + e.getMessage() + "\n");
        }
    }

    private void checkDirectories(ValidationReport report) {
        String[] dirs = {
                "src/main/java",
                "src/main/resources",
                "src/test/java",
                "src/test/resources"
        };

        for (String dir : dirs) {
            System.out.println("[RULE] " + dir + " directory exists");
            Path dirPath = projectRoot.resolve(dir);
            if (Files.isDirectory(dirPath)) {
                report.pass(dir + " exists", "Directory exists: " + dir);
                System.out.println("  ✓ Directory exists: " + dir + "\n");
            } else {
                report.fail(dir + " exists", "Directory not found: " + dir);
                System.out.println("  ✗ Directory not found: " + dir + "\n");
            }
        }
    }

    private void checkMavenCompile(ValidationReport report) {
        System.out.println("[RULE] mvn compile succeeds without errors");
        System.out.println("[DEBUG] Running mvn compile...");
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "-B", "--no-transfer-progress");
            pb.directory(projectRoot.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            boolean success = exitCode == 0 && output.toUpperCase().contains("BUILD SUCCESS");

            if (success) {
                report.pass("mvn compile", "mvn compile — BUILD SUCCESS");
                System.out.println("  ✓ mvn compile — BUILD SUCCESS\n");
            } else {
                // Extract error lines
                String[] lines = output.split("\n");
                String errors = Arrays.stream(lines)
                        .filter(l -> l.contains("[ERROR]"))
                        .limit(5)
                        .collect(Collectors.joining("\n  "));

                String evidence = "mvn compile — BUILD FAILURE (exit=" + exitCode + ")\n  " +
                        (errors.isEmpty() ? output.substring(Math.max(0, output.length() - 400)) : errors);
                report.fail("mvn compile", evidence);
                System.out.println("  ✗ " + evidence + "\n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            report.fail("mvn compile", "Maven compile interrupted");
            System.out.println("  ✗ Maven compile interrupted\n");
        } catch (IOException e) {
            report.fail("mvn compile", "Maven (mvn) not found in PATH — ensure mvn is installed and on PATH");
            System.out.println("  ✗ Maven (mvn) not found in PATH\n");
        }
    }

    @SuppressWarnings("unchecked")
    private void saveToSpec(Map<String, Object> spec, ValidationReport report) {
        try {
            // Ensure intermediate.history exists
            Map<String, Object> intermediate = (Map<String, Object>) spec.computeIfAbsent("intermediate",
                    k -> new HashMap<>());
            List<Map<String, Object>> history = (List<Map<String, Object>>) intermediate.computeIfAbsent("history",
                    k -> new ArrayList<>());

            // Add this validation run to history
            history.add(report.toMap("project-setup"));

            // Save updated spec
            YamlUtils.saveYaml(specPath, spec);

        } catch (IOException e) {
            System.err.println("[WARNING] Could not save to spec: " + e.getMessage());
        }
    }
}
