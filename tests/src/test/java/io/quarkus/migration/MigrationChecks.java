package io.quarkus.migration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Individual migration quality checks that can be run against a migrated project directory.
 */
public class MigrationChecks {

    private final Path projectDir;

    public MigrationChecks(Path projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * Check if the project compiles successfully.
     */
    public boolean builds() {
        return runMaven("compile", "-DskipTests") == 0;
    }

    /**
     * Check if all tests pass.
     */
    public boolean testsPass() {
        return runMaven("test") == 0;
    }

    /**
     * Check that no Spring Framework dependencies remain in pom.xml.
     */
    public boolean noSpringDeps() {
        return !fileContains(projectDir.resolve("pom.xml"), "org.springframework");
    }

    /**
     * Check that Quarkus dependencies are present in pom.xml.
     */
    public boolean hasQuarkus() {
        return fileContains(projectDir.resolve("pom.xml"), "io.quarkus");
    }

    /**
     * Check that the application starts up and responds to HTTP requests.
     */
    public boolean startsUp() {
        int port = 18080;
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getMvnCmd(), "-q", "quarkus:dev",
                    "-Dquarkus.http.port=" + port,
                    "-Dquarkus.devservices.enabled=false",
                    "-Dquarkus.analytics.disabled=true"
            ).directory(projectDir.toFile())
             .redirectErrorStream(true)
             .redirectOutput(projectDir.resolve(".startup.log").toFile());

            process = pb.start();

            // Poll for readiness
            for (int i = 0; i < 30; i++) {
                Thread.sleep(2000);

                if (!process.isAlive()) {
                    return false; // process died
                }

                if (httpOk("http://localhost:" + port + "/q/health/ready") ||
                    httpOk("http://localhost:" + port + "/")) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroyForcibly();
                try {
                    process.waitFor(10, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Check that no Thymeleaf references remain.
     */
    public boolean noThymeleaf() {
        if (fileContains(projectDir.resolve("pom.xml"), "thymeleaf")) {
            return false;
        }
        try (var stream = Files.walk(projectDir.resolve("src"))) {
            return stream
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".html") || p.toString().endsWith(".xml"))
                    .noneMatch(p -> fileContains(p, "thymeleaf") || fileContains(p, "th:"));
        } catch (IOException e) {
            return true; // no src dir = no thymeleaf
        }
    }

    /**
     * Run a specific named check.
     */
    public boolean runCheck(String checkName) {
        return switch (checkName) {
            case "builds" -> builds();
            case "tests-pass" -> testsPass();
            case "no-spring-deps" -> noSpringDeps();
            case "has-quarkus" -> hasQuarkus();
            case "starts-up" -> startsUp();
            case "no-thymeleaf" -> noThymeleaf();
            default -> throw new IllegalArgumentException("Unknown check: " + checkName);
        };
    }

    // -- helpers --

    private int runMaven(String... goals) {
        try {
            var cmd = new java.util.ArrayList<String>();
            cmd.add(getMvnCmd());
            cmd.add("-q");
            cmd.addAll(java.util.List.of(goals));

            Process p = new ProcessBuilder(cmd)
                    .directory(projectDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(projectDir.resolve(".maven-" + goals[0] + ".log").toFile())
                    .start();

            boolean done = p.waitFor(300, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return -1;
            }
            return p.exitValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private String getMvnCmd() {
        Path wrapper = projectDir.resolve("mvnw");
        if (Files.isExecutable(wrapper)) {
            return wrapper.toAbsolutePath().toString();
        }
        return "mvn";
    }

    private static boolean fileContains(Path file, String text) {
        try {
            return Files.readString(file).contains(text);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean httpOk(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }
}
