package org.eclipse.cargotracker.interfaces.handling.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("LineParseExceptionListener")
@Scope("prototype")
public class LineParseExceptionListener
    implements SkipListener<HandlingEventRegistrationAttempt, HandlingEventRegistrationAttempt> {

  private Logger logger;

  @Value("${batch.failed.directory}")
  private String failedDirectoryPath;
  private StepExecution stepExecution;

  public LineParseExceptionListener(Logger logger) {
    this.logger = logger;
  }

  @BeforeStep
  public void captureStepExecution(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  public void onSkipInRead(Throwable t) {
    if (!(t instanceof EventLineParseException)) {
      return;
    }

    EventLineParseException parseException = (EventLineParseException) t;
    File failedDirectory = new File(failedDirectoryPath);

    if (!failedDirectory.exists()) {
      failedDirectory.mkdirs();
    }

    logger.log(Level.WARNING, "Problem parsing event file line", parseException);

    String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
    Long jobId = stepExecution.getJobExecution().getId();

    try (PrintWriter failed =
        new PrintWriter(
            new BufferedWriter(
                new FileWriter(
                    new File(
                        failedDirectory,
                        "failed_"
                            + jobName
                            + "_"
                            + jobId
                            + ".csv"),
                    true)))) {
      failed.println(parseException.getLine());
    } catch (IOException e) {
      // re-throw to match behaviour?
      throw new RuntimeException(e);
    }
  }
}
