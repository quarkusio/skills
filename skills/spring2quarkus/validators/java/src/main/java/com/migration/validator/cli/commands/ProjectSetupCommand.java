package com.migration.validator.cli.commands;

import com.migration.validator.ProjectSetupValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command for validating Phase 3: Project Bootstrap.
 * 
 * Usage:
 * java -jar migration-validator-1.0.0.jar validate project-setup \
 * <project_root> <migration-spec.yaml> [--verbose]
 */
@Command(name = "project-setup", description = "Validate Phase 3: Project Bootstrap - Verifies Quarkus project structure, dependencies, and build configuration", mixinStandardHelpOptions = true)
public class ProjectSetupCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Target Quarkus project root directory")
    private Path projectRoot;

    @Parameters(index = "1", description = "Path to migration-spec.yaml file")
    private Path specPath;

    @Option(names = { "-v", "--verbose" }, description = "Enable verbose output with detailed logging")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        // Create validator instance with constructor injection
        ProjectSetupValidator validator = new ProjectSetupValidator(projectRoot, specPath);
        return validator.validate(verbose);
    }
}
