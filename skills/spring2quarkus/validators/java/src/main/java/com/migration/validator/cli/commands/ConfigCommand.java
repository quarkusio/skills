package com.migration.validator.cli.commands;

import com.migration.validator.ConfigValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command for validating Phase 9: Configuration Migration.
 * 
 * Usage:
 * java -jar migration-validator-1.0.0.jar validate config \
 * <spring_project> <quarkus_project> <migration-spec.yaml> [--verbose]
 */
@Command(name = "config", description = "Validate Phase 9: Configuration Migration - Verifies application.properties migration from Spring Boot to Quarkus", mixinStandardHelpOptions = true)
public class ConfigCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Spring project root directory")
    private Path springProject;

    @Parameters(index = "1", description = "Quarkus project root directory")
    private Path quarkusProject;

    @Parameters(index = "2", description = "Path to migration-spec.yaml file")
    private Path specPath;

    @Option(names = { "-v", "--verbose" }, description = "Enable verbose output with detailed logging")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        ConfigValidator validator = new ConfigValidator(springProject, quarkusProject, specPath);
        return validator.validate(verbose);
    }
}
