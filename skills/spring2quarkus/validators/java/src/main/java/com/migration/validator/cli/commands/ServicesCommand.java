package com.migration.validator.cli.commands;

import com.migration.validator.ServiceValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command for validating Phase 6: Service Layer Migration.
 * 
 * Usage:
 * java -jar migration-validator-1.0.0.jar validate services \
 * <target_dir> <migration-spec.yaml> [--verbose]
 */
@Command(name = "services", description = "Validate Phase 6: Service Layer Migration - Verifies Spring @Service/@Component migration to CDI beans", mixinStandardHelpOptions = true)
public class ServicesCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Target Quarkus project directory")
    private Path targetDir;

    @Parameters(index = "1", description = "Path to migration-spec.yaml file")
    private Path specPath;

    @Option(names = { "-v", "--verbose" }, description = "Enable verbose output with detailed logging")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        ServiceValidator validator = new ServiceValidator(targetDir, specPath);
        return validator.validate(verbose);
    }
}
